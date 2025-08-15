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
import org.geysermc.geyser.network.CIDRMatcher;

import java.net.InetSocketAddress;
import java.util.List;

@ChannelHandler.Sharable
public class ProxyServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(ProxyServerHandler.class);
    public static final String NAME = "rak-proxy-server-handler";
    // The 13th byte of a v2 PROXY header for a PROXY command is 0x21 (33).
    private static final int V2_PROXY_COMMAND = 33;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        ByteBuf content = packet.content();
        // Mark the current position of the buffer before we attempt any speculative reads.
        content.markReaderIndex();

        // Try to decode a PROXY v2 header. If successful, the readerIndex will be advanced.
        // If it fails for any reason, we will reset the readerIndex and pass the original packet through.
        boolean decodedSuccessfully = tryDecodeProxyHeader(packet, content);

        if (!decodedSuccessfully) {
            // Decoding failed (header not present, malformed, or incomplete).
            // Reset the buffer to its original state to allow downstream handlers to process it.
            content.resetReaderIndex();
        }

        // Forward the packet. Its content is now either the stripped payload (on success)
        // or the original, untouched content (on failure).
        ctx.fireChannelRead(packet.retain());
    }

    private boolean tryDecodeProxyHeader(DatagramPacket packet, ByteBuf content) {
        // Since PROXY protocol v1 is not used for UDP, we only check for v2.
        if (ProxyProtocolDecoder.findVersion(content) != V2_PROXY_COMMAND) {
            return false;
        }

        // check if the address is whitelisted
        // This is a simple check to ensure that the proxier address is whitelisted.
        List<String> allowedProxyIPs = GeyserImpl.getInstance().getConfig().getBedrock().getProxyProtocolWhitelistedIPs();
        if (allowedProxyIPs.isEmpty()) {
            // No whitelisted IPs, so we trust the PROXY header
            return true;
        }
        boolean isWhitelistedProxier = false;
        for (org.geysermc.geyser.network.CIDRMatcher matcher : GeyserImpl.getInstance().getConfig().getBedrock().getWhitelistedIPsMatchers()) {
            // Check if the sender's address matches any of the whitelisted CIDRs
            if (matcher.matches(packet.sender().getAddress())) {
                isWhitelistedProxier = true;
                break;
            }
        }

        final HAProxyMessage decoded;
        try {
            decoded = ProxyProtocolDecoder.decode(content, V2_PROXY_COMMAND);
            if (decoded == null) {
                // The header was detected but was incomplete. Treat as failure.
                return false;
            }
        } catch (HAProxyProtocolException e) {
            // The header was malformed. Treat as failure.
            log.trace("{} sent what looked like a PROXY header, but it was malformed.", packet.sender(), e);
            return false;
        }

        // The readerIndex has been successfully advanced past the header.
        // If this is the first packet and proxier in whitelist from this proxy, cache the real address.
        InetSocketAddress presentAddress = GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().get(packet.sender());
        if (presentAddress == null && isWhitelistedProxier) {
            // The proxier is whitelisted, so we trust the PROXY header.
            presentAddress = new InetSocketAddress(decoded.sourceAddress(), decoded.sourcePort());
            log.debug("Got PROXY header: (from {}) {}. Caching session.", packet.sender(), presentAddress);
            GeyserImpl.getInstance().getGeyserServer().getProxiedAddresses().put(packet.sender(), presentAddress);
        }
        
        return true;
    }
}