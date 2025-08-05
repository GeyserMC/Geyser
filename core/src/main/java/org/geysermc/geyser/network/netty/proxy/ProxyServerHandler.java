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
        ByteBuf content = packet.content().copy();
        InetSocketAddress presentAddress = GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().get(packet.sender());

        if (presentAddress == null) {
            // --- First packet processing logic (trust the header once) ---
            int detectedVersion = ProxyProtocolDecoder.findVersion(content);
            if (detectedVersion == -1) {
                return;
            }

            final HAProxyMessage decoded;
            try {
                if ((decoded = ProxyProtocolDecoder.decode(content, detectedVersion)) == null) {
                    return;
                }
            } catch (HAProxyProtocolException e) {
                log.debug("{} sent malformed PROXY header", packet.sender(), e);
                return;
            }

            presentAddress = new InetSocketAddress(decoded.sourceAddress(), decoded.sourcePort());
            log.debug("Got PROXY header: (from {}) {}. Caching session.", packet.sender(), presentAddress);
            GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().put(packet.sender(), presentAddress);
        } else {
            // --- Subsequent packet processing logic ---
            int detectedVersion = ProxyProtocolDecoder.findVersion(content);
            if (detectedVersion != -1) {
                // If a subsequent packet also contains a header, only strip it without using its content
                try {
                    if (ProxyProtocolDecoder.decode(content, detectedVersion) == null) {
                        content = packet.content();
                    }
                } catch (HAProxyProtocolException e) {
                    log.debug("{} sent malformed subsequent PROXY header", packet.sender(), e);
                    content = packet.content();
                }
            }
        }

        // --- Unified packet reconstruction and forwarding logic ---
        // Regardless of whether the packet originally contained a header, `content` now represents the pure payload.
        // To ensure consistency in the behavior of downstream handlers, we always create a new packet instance to forward.
        // This avoids any issues that may arise from the difference in object instances between the original packet (retain) and a new packet.
        ByteBuf payloadOnly = content.copy();
        DatagramPacket newPacket = new DatagramPacket(payloadOnly, packet.recipient(), packet.sender());
        ctx.fireChannelRead(newPacket);
    }
}