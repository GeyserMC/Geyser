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
        InetSocketAddress presentAddress = GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().get(packet.sender());

        if (presentAddress == null) {
            // We haven't received a header from given address before and we couldn't detect a PROXY header, ignore
            content.markReaderIndex(); // Mark the reader index before attempting to decode.
            int detectedVersion = ProxyProtocolDecoder.findVersion(content);
            if (detectedVersion == -1) {
                // No header detected. Reset index and drop the packet as it's invalid.
                content.resetReaderIndex(); 
                return;
            }

            final HAProxyMessage decoded;
            try {
                // Attempt to decode. If it returns null, the header is incomplete/malformed.
                decoded = ProxyProtocolDecoder.decode(content, detectedVersion);
                if (decoded == null) {
                    log.debug("{} sent a packet that looked like a PROXY header but was malformed/incomplete.", packet.sender());
                    content.resetReaderIndex(); // Reset index on failure and drop.
                    return;
                }
            } catch (HAProxyProtocolException e) {
                log.debug("{} sent malformed PROXY header", packet.sender(), e);
                content.resetReaderIndex(); // Reset index on exception and drop.
                return;
            }

            // Successfully decoded, cache the real address to establish the session.
            presentAddress = new InetSocketAddress(decoded.sourceAddress(), decoded.sourcePort());
            log.debug("Got PROXY header: (from {}) {}. Caching session.", packet.sender(), presentAddress);
            GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().put(packet.sender(), presentAddress);
        } else {
            // --- SUBSEQUENT PACKET LOGIC (Strip header if present) ---
            // A session for this proxy is already cached. We trust the cached address.
            // Our only job here is to strip any redundant headers (for per-packet-header mode).
            
            content.markReaderIndex(); // Mark the reader index before speculative decoding.
            int detectedVersion = ProxyProtocolDecoder.findVersion(content);
            if (detectedVersion != -1) {
                try {
                    // We only call decode for its side-effect of advancing the reader index.
                    // If it returns null, the decode failed, so we must reset and drop the packet.
                    if (ProxyProtocolDecoder.decode(content, detectedVersion) == null) {
                        log.debug("Detected a subsequent PROXY header from {} but it was malformed/incomplete.", packet.sender());
                        content.resetReaderIndex(); // Reset index on failure and drop.
                        return;
                    }
                } catch (HAProxyProtocolException e) {
                    log.debug("{} sent malformed subsequent PROXY header", packet.sender(), e);
                    content.resetReaderIndex(); // Reset index on exception and drop.
                    return;
                }
            } else {
                // No subsequent header was detected. Reset the index to its original state before this check.
                content.resetReaderIndex();
            }
        }
        
        // At this point, the buffer's reader index is correctly positioned at the start of the actual payload,
        // because any valid PROXY header has been consumed.
        // We pass the original packet down the pipeline, with its buffer view now correctly adjusted.
        // This avoids the overhead of creating a new packet object and copying the buffer.
        ctx.fireChannelRead(packet.retain());
    }
}