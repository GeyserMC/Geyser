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
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.GeyserBedrockPeer;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class ProxyServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(ProxyServerHandler.class);
    public static final String NAME = "rak-proxy-server-handler";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        ByteBuf content = packet.content();
        GeyserBedrockPeer peer = (GeyserBedrockPeer) ctx.pipeline().get(BedrockPeer.NAME);
        int detectedVersion = peer != null ? -1 : ProxyProtocolDecoder.findVersion(content);
        InetSocketAddress presentAddress = GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().get(packet.sender());

        if (presentAddress == null && detectedVersion == -1) {
            // We haven't received a header from given address before and we couldn't detect a
            // PROXY header, ignore.
            return;
        }

        if (presentAddress == null) {
            final HAProxyMessage decoded;
            try {
                if ((decoded = ProxyProtocolDecoder.decode(content, detectedVersion)) == null) {
                    // PROXY header was not present in the packet, ignore.
                    return;
                }
            } catch (HAProxyProtocolException e) {
                log.debug("{} sent malformed PROXY header", packet.sender(), e);
                return;
            }

            presentAddress = new InetSocketAddress(decoded.sourceAddress(), decoded.sourcePort());
            log.debug("Got PROXY header: (from {}) {}", packet.sender(), presentAddress);
            GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().put(packet.sender(), presentAddress);
        } else {
            log.trace("Reusing PROXY header: (from {}) {}", packet.sender(), presentAddress);
        }

        ctx.fireChannelRead(packet.retain());
    }
}