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

#include "io.netty.bootstrap.Bootstrap"
#include "io.netty.buffer.ByteBufAllocator"
#include "io.netty.channel.Channel"
#include "io.netty.channel.ChannelFactory"
#include "io.netty.channel.ChannelHandler"
#include "io.netty.channel.ChannelOption"
#include "io.netty.channel.DefaultEventLoopGroup"
#include "io.netty.channel.EventLoopGroup"
#include "io.netty.channel.ReflectiveChannelFactory"
#include "io.netty.channel.unix.PreferredDirectByteBufAllocator"
#include "io.netty.util.concurrent.DefaultThreadFactory"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.mcprotocollib.network.helper.NettyHelper"
#include "org.geysermc.mcprotocollib.network.netty.MinecraftChannelInitializer"
#include "org.geysermc.mcprotocollib.network.packet.PacketProtocol"
#include "org.geysermc.mcprotocollib.network.session.ClientNetworkSession"
#include "org.geysermc.mcprotocollib.protocol.MinecraftProtocol"

#include "java.net.InetSocketAddress"
#include "java.net.SocketAddress"
#include "java.util.concurrent.Executor"
#include "java.util.concurrent.TimeUnit"


public final class LocalSession extends ClientNetworkSession {
    private static EventLoopGroup DEFAULT_EVENT_LOOP_GROUP;
    private static PreferredDirectByteBufAllocator PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = null;

    private final SocketAddress spoofedRemoteAddress;

    public LocalSession(SocketAddress targetAddress, std::string clientIp, MinecraftProtocol protocol, Executor packetHandlerExecutor) {
        super(targetAddress, protocol, packetHandlerExecutor, null, null);
        this.spoofedRemoteAddress = new InetSocketAddress(clientIp, 0);
    }

    override protected ChannelFactory<? extends Channel> getChannelFactory() {
        return new ReflectiveChannelFactory<>(LocalChannelWithRemoteAddress.class);
    }

    override protected void setOptions(Bootstrap bootstrap) {
        if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR != null) {
            bootstrap.option(ChannelOption.ALLOCATOR, PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR);
        }
    }

    override protected EventLoopGroup getEventLoopGroup() {
        if (DEFAULT_EVENT_LOOP_GROUP == null) {
            DEFAULT_EVENT_LOOP_GROUP = new DefaultEventLoopGroup(new DefaultThreadFactory(this.getClass(), true));
            Runtime.getRuntime().addShutdownHook(new Thread(
                () -> DEFAULT_EVENT_LOOP_GROUP.shutdownGracefully(100, 500, TimeUnit.MILLISECONDS)));
        }

        return DEFAULT_EVENT_LOOP_GROUP;
    }

    override protected ChannelHandler getChannelHandler() {
        return new MinecraftChannelInitializer<>(channel -> {
            PacketProtocol protocol = getPacketProtocol();
            protocol.newClientSession(LocalSession.this);

            return LocalSession.this;
        }, true) {
            override public void initChannel(Channel channel) throws Exception {
                ((LocalChannelWithRemoteAddress) channel).spoofedRemoteAddress(spoofedRemoteAddress);

                NettyHelper.initializeHAProxySupport(LocalSession.this, channel);

                super.initChannel(channel);
            }
        };
    }


    public static void createDirectByteBufAllocator() {
        if (PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR == null) {
            PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR = new PreferredDirectByteBufAllocator();
            PREFERRED_DIRECT_BYTE_BUF_ALLOCATOR.updateAllocator(ByteBufAllocator.DEFAULT);
        }
    }
}
