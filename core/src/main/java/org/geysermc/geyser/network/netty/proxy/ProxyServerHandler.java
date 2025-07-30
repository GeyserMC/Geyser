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

        int detectedVersion = ProxyProtocolDecoder.findVersion(content);

        if (detectedVersion != -1) {
            // A PROXY protocol header is detected in this packet.
            final HAProxyMessage decoded;
            try {
                // The decode method consumes the header bytes from the `content` ByteBuf.
                decoded = ProxyProtocolDecoder.decode(content, detectedVersion);
            } catch (HAProxyProtocolException e) {
                log.debug("{} sent malformed PROXY header", packet.sender(), e);
                return; // Drop malformed packet.
            }

            if (decoded == null) {
                // This case should ideally not be reached if detectedVersion is valid, but acts as a safeguard.
                log.debug("PROXY header detected but failed to decode for {}", packet.sender());
                return;
            }

            // Header decoded successfully. Let's cache the real address.
            InetSocketAddress realAddress = new InetSocketAddress(decoded.sourceAddress(), decoded.sourcePort());
            GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().put(packet.sender(), realAddress);
            log.trace("Decoded PROXY header from proxy {} for real address {}", packet.sender(), realAddress);

            // CRITICAL CHECK: Determine if there is any payload left after stripping the header.
            if (!content.isReadable()) {
                // This was a header-only packet.
                // We have extracted and cached the necessary information.
                // There is no payload to forward, so we stop processing this packet here.
                return;
            }

            // This packet had a header AND a payload.
            // The header is now stripped, and we can forward the remaining payload.
            ctx.fireChannelRead(packet.retain());
            
        } else {
            // No PROXY header detected in this packet.
            // This must be a subsequent data packet for a session initiated with a header.
            InetSocketAddress cachedAddress = GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().get(packet.sender());

            if (cachedAddress == null) {
                // No header in this packet AND no cached session information.
                // This is an invalid packet from an unknown source.
                return;
            }

            // We have a valid session from cache. Forward this data packet as is.
            log.trace("Reusing PROXY session for proxy {}", packet.sender());
            ctx.fireChannelRead(packet.retain());
        }
    }
}
