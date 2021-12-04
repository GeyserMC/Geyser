/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.handler.codec.haproxy.*;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Manages a Minecraft Java session over our LocalChannel implementations.
 */
public final class LocalSession extends TcpSession {
    private static DefaultEventLoopGroup DEFAULT_EVENT_LOOP_GROUP;
    private static PreferredDirectByteBufAllocator PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = null;

    private final SocketAddress targetAddress;
    private final String clientIp;

    public LocalSession(String host, int port, SocketAddress targetAddress, String clientIp, PacketProtocol protocol) {
        super(host, port, protocol);
        this.targetAddress = targetAddress;
        this.clientIp = clientIp;
    }

    @Override
    public void connect() {
        if (this.disconnected) {
            throw new IllegalStateException("Connection has already been disconnected.");
        }

        if (DEFAULT_EVENT_LOOP_GROUP == null) {
            DEFAULT_EVENT_LOOP_GROUP = new DefaultEventLoopGroup();
        }

        try {
            final Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(LocalChannelWithRemoteAddress.class);
            bootstrap.handler(new ChannelInitializer<LocalChannelWithRemoteAddress>() {
                @Override
                public void initChannel(LocalChannelWithRemoteAddress channel) {
                    channel.spoofedRemoteAddress(new InetSocketAddress(clientIp, 0));
                    PacketProtocol protocol = getPacketProtocol();
                    protocol.newClientSession(LocalSession.this);

                    refreshReadTimeoutHandler(channel);
                    refreshWriteTimeoutHandler(channel);

                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast("sizer", new TcpPacketSizer(LocalSession.this, protocol.getPacketHeader().getLengthSize()));
                    pipeline.addLast("codec", new TcpPacketCodec(LocalSession.this, true));
                    pipeline.addLast("manager", LocalSession.this);

                    addHAProxySupport(pipeline);
                }
            }).group(DEFAULT_EVENT_LOOP_GROUP).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeout() * 1000);

            if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR != null) {
                bootstrap.option(ChannelOption.ALLOCATOR, PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR);
            }

            bootstrap.remoteAddress(targetAddress);

            bootstrap.connect().addListener((future) -> {
                if (!future.isSuccess()) {
                    exceptionCaught(null, future.cause());
                }
            });
        } catch (Throwable t) {
            exceptionCaught(null, t);
        }
    }

    // TODO duplicate code
    private void addHAProxySupport(ChannelPipeline pipeline) {
        InetSocketAddress clientAddress = getFlag(BuiltinFlags.CLIENT_PROXIED_ADDRESS);
        if (getFlag(BuiltinFlags.ENABLE_CLIENT_PROXY_PROTOCOL, false) && clientAddress != null) {
            pipeline.addFirst("proxy-protocol-packet-sender", new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
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
