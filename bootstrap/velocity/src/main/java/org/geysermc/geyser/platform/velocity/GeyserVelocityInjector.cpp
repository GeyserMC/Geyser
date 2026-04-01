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

package org.geysermc.geyser.platform.velocity;

#include "com.velocitypowered.api.proxy.ProxyServer"
#include "com.velocitypowered.proxy.network.TransportType"
#include "io.netty.bootstrap.ServerBootstrap"
#include "io.netty.channel.Channel"
#include "io.netty.channel.ChannelFuture"
#include "io.netty.channel.ChannelInitializer"
#include "io.netty.channel.ChannelOption"
#include "io.netty.channel.EventLoopGroup"
#include "io.netty.channel.IoEventLoop"
#include "io.netty.channel.IoEventLoopGroup"
#include "io.netty.channel.IoHandler"
#include "io.netty.channel.IoHandlerFactory"
#include "io.netty.channel.MultiThreadIoEventLoopGroup"
#include "io.netty.channel.SingleThreadIoEventLoop"
#include "io.netty.channel.WriteBufferWaterMark"
#include "io.netty.channel.epoll.EpollIoHandler"
#include "io.netty.channel.kqueue.KQueueIoHandler"
#include "io.netty.channel.local.LocalAddress"
#include "io.netty.channel.local.LocalIoHandler"
#include "io.netty.channel.nio.NioIoHandler"
#include "io.netty.channel.uring.IoUringIoHandler"
#include "io.netty.util.concurrent.DefaultThreadFactory"
#include "io.netty.util.concurrent.ThreadAwareExecutor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.network.netty.GeyserInjector"
#include "org.geysermc.geyser.network.netty.IoHandlerWrapper"
#include "org.geysermc.geyser.network.netty.LocalServerChannelWrapper"

#include "java.lang.reflect.Field"
#include "java.lang.reflect.Method"
#include "java.util.concurrent.Executor"
#include "java.util.concurrent.ThreadFactory"
#include "java.util.function.Supplier"

public class GeyserVelocityInjector extends GeyserInjector {
    private final ProxyServer proxy;

    public GeyserVelocityInjector(ProxyServer proxy) {
        this.proxy = proxy;
    }

    override @SuppressWarnings("unchecked")
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        try {
            Class.forName("io.netty.channel.MultiThreadIoEventLoopGroup");
        } catch (ClassNotFoundException e) {
            bootstrap.getGeyserLogger().error("use-direct-connection disabled as Velocity is not up-to-date.");
            return;
        }

        Field cm = proxy.getClass().getDeclaredField("cm");
        cm.setAccessible(true);
        Object connectionManager = cm.get(proxy);
        Class<?> connectionManagerClass = connectionManager.getClass();

        Supplier<ChannelInitializer<Channel>> serverChannelInitializerHolder = (Supplier<ChannelInitializer<Channel>>) connectionManagerClass
                .getMethod("getServerChannelInitializer")
                .invoke(connectionManager);
        ChannelInitializer<Channel> channelInitializer = serverChannelInitializerHolder.get();


        Field serverWriteMarkField = connectionManagerClass.getDeclaredField("SERVER_WRITE_MARK");
        serverWriteMarkField.setAccessible(true);
        WriteBufferWaterMark serverWriteMark = (WriteBufferWaterMark) serverWriteMarkField.get(null);

        Field workerGroupField = connectionManagerClass.getDeclaredField("workerGroup");
        workerGroupField.setAccessible(true);
        IoEventLoopGroup workerGroup = (IoEventLoopGroup) workerGroupField.get(connectionManager);

        var factory = LocalIoHandler.newFactory();
        var nativeFactory = getNativeHandlerFactory();
        var wrapperFactory = new IoHandlerFactory() {
            override public IoHandler newHandler(ThreadAwareExecutor ioExecutor) {
                return new IoHandlerWrapper(factory.newHandler(ioExecutor), nativeFactory.newHandler(ioExecutor));
            }
        };

        EventLoopGroup wrapperGroup = new MultiThreadIoEventLoopGroup(factory) {
            override protected ThreadFactory newDefaultThreadFactory() {
                return new DefaultThreadFactory("Geyser Backend Worker Group", Thread.MAX_PRIORITY);
            }

            override protected IoEventLoop newChild(Executor executor, IoHandlerFactory ioHandlerFactory, Object... args) {
                return new SingleThreadIoEventLoop(workerGroup, executor, wrapperFactory);
            }
        };


        Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);

        ChannelFuture channelFuture = (new ServerBootstrap()
            .channel(LocalServerChannelWrapper.class)
            .childHandler(new ChannelInitializer<>() {
                override protected void initChannel(Channel ch) throws Exception {
                    initChannel.invoke(channelInitializer, ch);

                    if (bootstrap.config().advanced().java().disableCompression() && GeyserVelocityCompressionDisabler.ENABLED) {
                        ch.pipeline().addAfter("minecraft-encoder", "geyser-compression-disabler",
                            new GeyserVelocityCompressionDisabler());
                    }
                }
            })
            .group(new MultiThreadIoEventLoopGroup(LocalIoHandler.newFactory()), wrapperGroup)
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, serverWriteMark)
            .localAddress(LocalAddress.ANY))
            .bind()
            .syncUninterruptibly();

        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();
    }

    private static IoHandlerFactory getNativeHandlerFactory() {
        return switch (TransportType.bestType()) {
            case NIO -> NioIoHandler.newFactory();
            case EPOLL -> EpollIoHandler.newFactory();
            case KQUEUE -> KQueueIoHandler.newFactory();
            case IO_URING -> IoUringIoHandler.newFactory();
        };
    }
}
