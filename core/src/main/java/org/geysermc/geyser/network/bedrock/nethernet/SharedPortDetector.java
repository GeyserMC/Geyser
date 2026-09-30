/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock.nethernet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * Separates NetherNet signaling from Java Edition connections on a shared TCP port, using the first two bytes.
 * <p>
 * Java clients start with a handshake (VarInt length, then packet id 0) or a legacy ping (0xFE).
 * Signaling starts with a TLS record (0x16 0x03) or an HTTP method (upper case letters), so the two never overlap.
 * A PROXY header is skipped, but left in place for whichever side takes the connection.
 */
public final class SharedPortDetector extends ByteToMessageDecoder {
    public static final String NAME = "geyser-nethernet-detector";

    private static final byte[] PROXY_V1 = "PROXY ".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PROXY_V2 = {0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A};
    private static final int PROXY_V1_MAX_LENGTH = 107;

    private final Consumer<Channel> signaling;

    private SharedPortDetector(Consumer<Channel> signaling) {
        this.signaling = signaling;
    }

    /**
     * Creates a handler for the Java server's listening channel, which adds a detector to every accepted connection.
     *
     * @param signaling called with each signaling connection after the Java server's handlers were removed
     */
    public static ChannelHandler acceptor(Consumer<Channel> signaling) {
        return new Acceptor(signaling);
    }

    /**
     * Adds a detector to a connection, before the Java server adds its handlers.
     *
     * @param signaling called with the connection if it is a signaling one, after the Java server's handlers were removed
     */
    public static void addTo(Channel connection, Consumer<Channel> signaling) {
        connection.pipeline().addFirst(NAME, new SharedPortDetector(signaling));
    }

    static boolean isSignaling(int first, int second) {
        if (first == 0x16) {
            return second == 0x03;
        }
        return isUpperCaseLetter(first) && isUpperCaseLetter(second);
    }

    private static boolean isUpperCaseLetter(int b) {
        return b >= 'A' && b <= 'Z';
    }

    /**
     * @return the length of the PROXY header at the start of the buffer, 0 if there is none, or -1 if more bytes are needed
     */
    static int proxyHeaderLength(ByteBuf in) {
        int start = in.readerIndex();
        int v2 = startsWith(in, PROXY_V2);
        if (v2 != 0) {
            if (v2 < 0 || in.readableBytes() < 16) {
                return -1;
            }
            return 16 + in.getUnsignedShort(start + 14);
        }
        int v1 = startsWith(in, PROXY_V1);
        if (v1 != 0) {
            if (v1 < 0) {
                return -1;
            }
            int end = Math.min(in.readableBytes(), PROXY_V1_MAX_LENGTH);
            for (int i = PROXY_V1.length; i < end - 1; i++) {
                if (in.getByte(start + i) == '\r' && in.getByte(start + i + 1) == '\n') {
                    return i + 2;
                }
            }
            // Too long to be a header
            return end == PROXY_V1_MAX_LENGTH ? 0 : -1;
        }
        return 0;
    }

    /**
     * @return 1 if the buffer starts with the prefix, 0 if it does not, or -1 if more bytes are needed
     */
    private static int startsWith(ByteBuf in, byte[] prefix) {
        int length = Math.min(in.readableBytes(), prefix.length);
        for (int i = 0; i < length; i++) {
            if (in.getByte(in.readerIndex() + i) != prefix[i]) {
                return 0;
            }
        }
        return length == prefix.length ? 1 : -1;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int header = proxyHeaderLength(in);
        if (header < 0 || in.readableBytes() < header + 2) {
            return;
        }
        int first = in.readerIndex() + header;
        ChannelPipeline pipeline = ctx.pipeline();
        if (isSignaling(in.getUnsignedByte(first), in.getUnsignedByte(first + 1))) {
            // Remove the handlers the Java server added
            List<String> names = pipeline.names();
            for (int i = names.indexOf(NAME) + 1; i < names.size(); i++) {
                ChannelHandler handler = pipeline.get(names.get(i));
                if (handler != null) {
                    pipeline.remove(handler);
                }
            }
            signaling.accept(ctx.channel());
        }
        // Passes the buffered bytes on to the next handler
        pipeline.remove(this);
    }

    @ChannelHandler.Sharable
    private static final class Acceptor extends ChannelInboundHandlerAdapter {
        private final Consumer<Channel> signaling;

        private Acceptor(Consumer<Channel> signaling) {
            this.signaling = signaling;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // Runs before the server initialises the connection, so the detector stays in front of its handlers
            if (msg instanceof Channel child) {
                addTo(child, signaling);
            }
            ctx.fireChannelRead(msg);
        }
    }
}
