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

        // MODIFIED: Handle non-proxy packets from unknown sources
        if (presentAddress == null && detectedVersion == -1) {
            /*
             * Non-proxy packet from unknown source: 
             * - Retain the packet to prevent auto-release by SimpleChannelInboundHandler
             * - Forward through pipeline for normal processing
             * - Reference count: retain() balances the auto-release of the original packet
             */
            ctx.fireChannelRead(packet.retain());
            return;
        }

        // MODIFIED: Process potential proxy header
        if (presentAddress == null) {
            HAProxyMessage decoded = null;
            try {
                decoded = ProxyProtocolDecoder.decode(content, detectedVersion);
                
                // Handle non-proxy packets that passed initial detection
                if (decoded == null) {
                    /*
                     * Failed to decode proxy header but passed version check:
                     * - Treat as normal packet
                     * - Retain to prevent auto-release
                     */
                    ctx.fireChannelRead(packet.retain());
                    return;
                }
            } catch (HAProxyProtocolException e) {
                log.debug("{} sent malformed PROXY header", packet.sender(), e);
                return;
            }

            // Store decoded proxy address for future packets
            presentAddress = new InetSocketAddress(decoded.sourceAddress(), decoded.sourcePort());
            log.debug("Got PROXY header: (from {}) {}", packet.sender(), presentAddress);
            GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().put(packet.sender(), presentAddress);
        } else {
            log.trace("Reusing PROXY header: (from {}) {}", packet.sender(), presentAddress);
        }

        // MODIFIED: Create proxy-stripped packet
        /*
         * Processing proxy packets:
         * - content.readerIndex() was advanced by ProxyProtocolDecoder
         * - slice() creates view of remaining data (Bedrock payload)
         * - retain() is CRITICAL: 
         *   1. Counteracts auto-release of original packet
         *   2. Maintains separate reference count for sliced payload
         */
        ByteBuf bedrockPayload = content.slice().retain();
        DatagramPacket newPacket = new DatagramPacket(bedrockPayload, packet.recipient(), packet.sender());

        // Forward proxy-stripped packet
        ctx.fireChannelRead(newPacket);
        
        /*
         * NOTE: Original 'packet' will be auto-released by SimpleChannelInboundHandler
         * The sliced 'bedrockPayload' has its own reference count and will be released 
         * by downstream handlers after processing
         */
    }
}
