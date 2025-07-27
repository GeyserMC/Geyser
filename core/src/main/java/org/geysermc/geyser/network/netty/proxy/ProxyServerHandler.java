/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.network.netty.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyProtocolException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.geysermc.geyser.GeyserImpl;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class ProxyServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(ProxyServerHandler.class);
    public static final String NAME = "rak-proxy-server-handler";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        ByteBuf content = packet.content();

        // Always check for a PROXY header in the current packet first.
        // This supports the "header-per-packet" scenario (e.g., frp for UDP).
        int detectedVersion = ProxyProtocolDecoder.findVersion(content);
        InetSocketAddress realAddress = null;

        if (detectedVersion != -1) {
            // A header is present in THIS packet. Attempt to decode it.
            // The decode method will consume the header bytes from the `content` ByteBuf.
            final HAProxyMessage decoded;
            try {
                if ((decoded = ProxyProtocolDecoder.decode(content, detectedVersion)) != null) {
                    // Successfully decoded and stripped the header.
                    realAddress = new InetSocketAddress(decoded.sourceAddress(), decoded.sourcePort());

                    // Update the cache with the latest address information.
                    GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().put(packet.sender(), realAddress);
                    log.trace("Decoded PROXY header: (from {}) {}", packet.sender(), realAddress);
                }
                // If decode returns null, it's a malformed header, and we will drop the packet later.
            } catch (HAProxyProtocolException e) {
                log.debug("{} sent malformed PROXY header", packet.sender(), e);
                return; // Drop malformed packet immediately.
            }
        } else {
            // No header in this packet. Check if we have a cached address.
            // This supports the "header-once" scenario for subsequent packets.
            realAddress = GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().get(packet.sender());
            if (realAddress != null) {
                log.trace("Reusing PROXY header: (from {}) {}", packet.sender(), realAddress);
            }
        }

        // After all checks, if we still don't have a real address, the packet is invalid.
        if (realAddress == null) {
            // This occurs if:
            // 1. It's the first packet from a source and it has no PROXY header.
            // 2. The packet contained a malformed header that failed to decode.
            return;
        }

        // If we've reached here, the packet is valid and 'content' is stripped of any header it might have had.
        // Fire the read to pass the clean packet (game data only) to the next handler in the pipeline.
        ctx.fireChannelRead(packet.retain());
    }
}
