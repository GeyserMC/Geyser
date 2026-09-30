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
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedPortDetectorTest {
    private static final byte[] JAVA_HANDSHAKE = {0x10, 0x00, (byte) 0xF8, 0x05, 0x09, 'l', 'o', 'c', 'a', 'l', 'h', 'o', 's', 't', 0x63, (byte) 0xDD, 0x01};
    private static final byte[] HTTP = "GET /v1/join HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TLS = {0x16, 0x03, 0x01, 0x00, 0x05, 0x01, 0x00, 0x00, 0x01, 0x00};

    private final List<Channel> handedOver = new ArrayList<>();
    private final Capture signaling = new Capture();
    private final Capture minecraft = new Capture();

    /**
     * A connection as the Java server has it: accepted through the acceptor, then set up with the server's handlers.
     */
    private EmbeddedChannel accept() {
        EmbeddedChannel listener = new EmbeddedChannel(SharedPortDetector.acceptor(channel -> {
            handedOver.add(channel);
            channel.pipeline().addLast("signaling", signaling);
        }));
        EmbeddedChannel connection = new EmbeddedChannel();
        listener.writeInbound(connection);
        connection.pipeline().addLast("timeout", new ChannelInboundHandlerAdapter());
        connection.pipeline().addLast("minecraft", minecraft);
        return connection;
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static byte[] proxyV2() {
        byte[] header = new byte[16 + 12];
        System.arraycopy(new byte[]{0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A}, 0, header, 0, 12);
        header[12] = 0x21;
        header[13] = 0x11;
        header[15] = 12;
        return header;
    }

    private static final byte[] PROXY_V1 = "PROXY TCP4 203.0.113.7 127.0.0.1 5555 25565\r\n".getBytes(StandardCharsets.US_ASCII);

    private void assertStaysWithJava(EmbeddedChannel connection, byte[] sent) {
        assertTrue(handedOver.isEmpty());
        assertNull(connection.pipeline().get(SharedPortDetector.NAME));
        assertNotNull(connection.pipeline().get("minecraft"));
        assertArrayEquals(sent, minecraft.bytes());
    }

    private void assertHandedToSignaling(EmbeddedChannel connection, byte[] sent) {
        assertEquals(List.of(connection), handedOver);
        assertNull(connection.pipeline().get(SharedPortDetector.NAME));
        assertNull(connection.pipeline().get("timeout"));
        assertNull(connection.pipeline().get("minecraft"));
        assertArrayEquals(sent, signaling.bytes());
        assertEquals(0, minecraft.bytes().length);
    }

    @Test
    void leavesAJavaHandshakeToTheServer() {
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(JAVA_HANDSHAKE));
        assertStaysWithJava(connection, JAVA_HANDSHAKE);
    }

    @Test
    void leavesAHandshakeWithATwoByteLengthToTheServer() {
        byte[] sent = {(byte) 0x85, 0x02, 0x00, 0x01};
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(sent));
        assertStaysWithJava(connection, sent);
    }

    @Test
    void leavesALegacyPingToTheServer() {
        byte[] sent = {(byte) 0xFE, 0x01, (byte) 0xFA};
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(sent));
        assertStaysWithJava(connection, sent);
    }

    @Test
    void leavesAHandshakeThatStartsLikeALetterToTheServer() {
        // A handshake 'G' (71) bytes long is followed by packet id 0, never by another letter
        byte[] sent = {'G', 0x00, 0x01};
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(sent));
        assertStaysWithJava(connection, sent);
    }

    @Test
    void handsHttpToSignaling() {
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(HTTP));
        assertHandedToSignaling(connection, HTTP);
    }

    @Test
    void handsTlsToSignaling() {
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(TLS));
        assertHandedToSignaling(connection, TLS);
    }

    @Test
    void waitsForTwoBytesArrivingApart() {
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(HTTP, 0, 1));
        assertTrue(handedOver.isEmpty());
        assertNotNull(connection.pipeline().get(SharedPortDetector.NAME));

        connection.writeInbound(Unpooled.wrappedBuffer(HTTP, 1, HTTP.length - 1));
        assertHandedToSignaling(connection, HTTP);
    }

    @Test
    void looksPastAVersionOneProxyHeaderToJava() {
        byte[] java = concat(PROXY_V1, JAVA_HANDSHAKE);
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(java));
        assertStaysWithJava(connection, java);
    }

    @Test
    void looksPastAVersionOneProxyHeaderToSignaling() {
        byte[] http = concat(PROXY_V1, HTTP);
        EmbeddedChannel connection = accept();
        connection.writeInbound(Unpooled.wrappedBuffer(http));
        assertHandedToSignaling(connection, http);
    }

    @Test
    void looksPastAVersionTwoProxyHeader() {
        byte[] tls = concat(proxyV2(), TLS);
        EmbeddedChannel connection = accept();
        // The header arrives on its own first, so nothing can be told from it yet
        connection.writeInbound(Unpooled.wrappedBuffer(tls, 0, 20));
        assertTrue(handedOver.isEmpty());

        connection.writeInbound(Unpooled.wrappedBuffer(tls, 20, tls.length - 20));
        assertHandedToSignaling(connection, tls);
    }

    @Test
    void readsHowLongAProxyHeaderIs() {
        assertEquals(PROXY_V1.length, SharedPortDetector.proxyHeaderLength(Unpooled.wrappedBuffer(PROXY_V1)));
        assertEquals(28, SharedPortDetector.proxyHeaderLength(Unpooled.wrappedBuffer(proxyV2())));
        assertEquals(-1, SharedPortDetector.proxyHeaderLength(Unpooled.wrappedBuffer(PROXY_V1, 0, 3)));
        assertEquals(0, SharedPortDetector.proxyHeaderLength(Unpooled.wrappedBuffer("POST".getBytes(StandardCharsets.US_ASCII))));
        assertEquals(0, SharedPortDetector.proxyHeaderLength(Unpooled.wrappedBuffer(JAVA_HANDSHAKE)));
    }

    /** Collects the bytes that reach it. */
    private static final class Capture extends ChannelInboundHandlerAdapter {
        private final ByteArrayOutputStream received = new ByteArrayOutputStream();

        @Override
        public boolean isSharable() {
            return true;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            buf.release();
            received.writeBytes(bytes);
        }

        byte[] bytes() {
            return received.toByteArray();
        }
    }
}
