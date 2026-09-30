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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.geysermc.geyser.GeyserLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedJavaPortTest {
    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    private final GeyserLogger logger = Mockito.mock(GeyserLogger.class);
    private final SharedJavaPort sharedJavaPort = new SharedJavaPort();
    private final BlockingQueue<String> javaServer = new ArrayBlockingQueue<>(4);
    private final BlockingQueue<Channel> signaling = new ArrayBlockingQueue<>(4);
    private Channel listener;

    @AfterEach
    void tearDown() {
        sharedJavaPort.close();
        if (listener != null) {
            listener.close().syncUninterruptibly();
        }
        group.shutdownGracefully();
    }

    /**
     * A Java server that reports the first byte of every connection its own handler sees.
     */
    private int startJavaServer() throws InterruptedException {
        listener = new ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast("minecraft", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            javaServer.offer("received");
                            ReferenceCountUtil.release(msg);
                        }
                    });
                }
            })
            .bind(new InetSocketAddress("127.0.0.1", 0)).sync().channel();
        return ((InetSocketAddress) listener.localAddress()).getPort();
    }

    private static void send(int port, byte[] bytes) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            OutputStream out = socket.getOutputStream();
            out.write(bytes);
            out.flush();
            Thread.sleep(200);
        }
    }

    private boolean share(int port) {
        return sharedJavaPort.share(signaling::offer, port, () -> List.of(listener), logger);
    }

    @Test
    void handsSignalingOnTheJavaPortOverAndLeavesJavaToTheServer() throws Exception {
        int port = startJavaServer();
        assertTrue(share(port));
        assertNotNull(listener.pipeline().get(SharedPortDetector.NAME));

        send(port, "GET /v1/join HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        assertNotNull(signaling.poll(5, TimeUnit.SECONDS));

        send(port, new byte[]{0x10, 0x00, 0x01});
        assertEquals("received", javaServer.poll(5, TimeUnit.SECONDS));
        assertTrue(signaling.isEmpty());
    }

    @Test
    void skipsListenersOnOtherPorts() throws Exception {
        int port = startJavaServer();
        assertFalse(share(port + 1));
        assertNull(listener.pipeline().get(SharedPortDetector.NAME));
    }

    @Test
    void closeLeavesTheServerAsItWas() throws Exception {
        int port = startJavaServer();
        assertTrue(share(port));
        sharedJavaPort.close();
        assertNull(listener.pipeline().get(SharedPortDetector.NAME));

        send(port, "GET / HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        assertEquals("received", javaServer.poll(5, TimeUnit.SECONDS));
    }

    @Test
    void detectsOnlyWhileSharing() {
        EmbeddedChannel before = new EmbeddedChannel();
        sharedJavaPort.detect(before);
        assertNull(before.pipeline().get(SharedPortDetector.NAME));

        sharedJavaPort.share(signaling::offer);
        EmbeddedChannel connection = new EmbeddedChannel();
        sharedJavaPort.detect(connection);
        assertNotNull(connection.pipeline().get(SharedPortDetector.NAME));
    }
}
