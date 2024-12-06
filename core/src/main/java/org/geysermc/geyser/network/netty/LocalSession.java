/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;
import org.geysermc.mcprotocollib.network.tcp.FlushHandler;
import org.geysermc.mcprotocollib.network.tcp.TcpFlowControlHandler;
import org.geysermc.mcprotocollib.network.tcp.TcpPacketCodec;
import org.geysermc.mcprotocollib.network.tcp.TcpPacketCompression;
import org.geysermc.mcprotocollib.network.tcp.TcpPacketEncryptor;
import org.geysermc.mcprotocollib.network.tcp.TcpPacketSizer;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Manages a Minecraft Java session over our LocalChannel implementations.
 */
public final class LocalSession extends TcpSession {
    private static DefaultEventLoopGroup DEFAULT_EVENT_LOOP_GROUP;
    private static PreferredDirectByteBufAllocator PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = null;

    private final SocketAddress targetAddress;
    private final String clientIp;
    private final PacketCodecHelper codecHelper;

    public LocalSession(String host, int port, SocketAddress targetAddress, String clientIp, PacketProtocol protocol, Executor packetHandlerExecutor) {
        super(host, port, protocol, packetHandlerExecutor);
        this.targetAddress = targetAddress;
        this.clientIp = clientIp;
        this.codecHelper = protocol.createHelper();
    }

    @Override
    public void connect(boolean wait, boolean transferring) {
        if (this.disconnected) {
            throw new IllegalStateException("Connection has already been disconnected.");
        }

        if (DEFAULT_EVENT_LOOP_GROUP == null) {
            DEFAULT_EVENT_LOOP_GROUP = new DefaultEventLoopGroup(new DefaultThreadFactory(this.getClass(), true));
            Runtime.getRuntime().addShutdownHook(new Thread(
                () -> DEFAULT_EVENT_LOOP_GROUP.shutdownGracefully(100, 500, TimeUnit.MILLISECONDS)));
        }

        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(LocalChannelWithRemoteAddress.class);
        bootstrap.handler(new ChannelInitializer<LocalChannelWithRemoteAddress>() {
            @Override
            public void initChannel(@NonNull LocalChannelWithRemoteAddress channel) {
                channel.spoofedRemoteAddress(new InetSocketAddress(clientIp, 0));
                PacketProtocol protocol = getPacketProtocol();
                protocol.newClientSession(LocalSession.this, transferring);

                ChannelPipeline pipeline = channel.pipeline();

                addHAProxySupport(pipeline);

                pipeline.addLast("read-timeout", new ReadTimeoutHandler(getFlag(BuiltinFlags.READ_TIMEOUT, 30)));
                pipeline.addLast("write-timeout", new WriteTimeoutHandler(getFlag(BuiltinFlags.WRITE_TIMEOUT, 0)));

                pipeline.addLast("encryption", new TcpPacketEncryptor());
                pipeline.addLast("sizer", new TcpPacketSizer(protocol.getPacketHeader(), getCodecHelper()));
                pipeline.addLast("compression", new TcpPacketCompression(getCodecHelper()));

                pipeline.addLast("flow-control", new TcpFlowControlHandler());
                pipeline.addLast("codec", new TcpPacketCodec(LocalSession.this, true));
                pipeline.addLast("flush-handler", new FlushHandler());
                pipeline.addLast("manager", LocalSession.this);
            }
        }).group(DEFAULT_EVENT_LOOP_GROUP).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getFlag(BuiltinFlags.CLIENT_CONNECT_TIMEOUT, 30) * 1000);

        if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR != null) {
            bootstrap.option(ChannelOption.ALLOCATOR, PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR);
        }

        bootstrap.remoteAddress(targetAddress);

        CompletableFuture<Void> handleFuture = new CompletableFuture<>();
        bootstrap.connect().addListener((futureListener) -> {
            if (!futureListener.isSuccess()) {
                exceptionCaught(null, futureListener.cause());
            }

            handleFuture.complete(null);
        });

        if (wait) {
            handleFuture.join();
        }
    }

    @Override
    public MinecraftCodecHelper getCodecHelper() {
        return (MinecraftCodecHelper) this.codecHelper;
    }

    // TODO duplicate code
    private void addHAProxySupport(ChannelPipeline pipeline) {
        InetSocketAddress clientAddress = getFlag(BuiltinFlags.CLIENT_PROXIED_ADDRESS);
        if (clientAddress != null) {
            pipeline.addFirst("proxy-protocol-packet-sender", new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(@NonNull ChannelHandlerContext ctx) throws Exception {
                    HAProxyProxiedProtocol proxiedProtocol = clientAddress.getAddress() instanceof Inet4Address ? HAProxyProxiedProtocol.TCP4 : HAProxyProxiedProtocol.TCP6;
                    InetSocketAddress remoteAddress;
                    if (ctx.channel().remoteAddress() instanceof InetSocketAddress) {
                        remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    } else {
                        remoteAddress = new InetSocketAddress(host, port);
                    }
                    ctx.channel().writeAndFlush(new HAProxyMessage(
                        HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, proxiedProtocol,
                        clientAddress.getAddress().getHostAddress(), remoteAddress.getAddress().getHostAddress(),
                        clientAddress.getPort(), remoteAddress.getPort()
                    ));
                    ctx.pipeline().remove(this);
                    ctx.pipeline().remove("proxy-protocol-encoder");
                    super.channelActive(ctx);
                }
            });
            pipeline.addFirst("proxy-protocol-encoder", HAProxyMessageEncoder.INSTANCE);
        }
        }

    /**
     * Should only be called when direct ByteBufs should be preferred. At this moment, this should only be called on BungeeCord.
     */
    public static void createDirectByteBufAllocator() {
        if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR == null) {
            PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = new PreferredDirectByteBufAllocator();
            PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR.updateAllocator(ByteBufAllocator.DEFAULT);
        }
    }
}
