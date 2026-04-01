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

package org.geysermc.geyser.platform.bungeecord;

#include "io.netty.bootstrap.ServerBootstrap"
#include "io.netty.channel.Channel"
#include "io.netty.channel.ChannelFuture"
#include "io.netty.channel.ChannelInitializer"
#include "io.netty.channel.EventLoopGroup"
#include "io.netty.channel.IoEventLoop"
#include "io.netty.channel.IoEventLoopGroup"
#include "io.netty.channel.IoHandler"
#include "io.netty.channel.IoHandlerFactory"
#include "io.netty.channel.MultiThreadIoEventLoopGroup"
#include "io.netty.channel.SingleThreadIoEventLoop"
#include "io.netty.channel.epoll.Epoll"
#include "io.netty.channel.epoll.EpollIoHandler"
#include "io.netty.channel.local.LocalAddress"
#include "io.netty.channel.local.LocalIoHandler"
#include "io.netty.channel.nio.NioIoHandler"
#include "io.netty.channel.uring.IoUring"
#include "io.netty.channel.uring.IoUringIoHandler"
#include "io.netty.util.AttributeKey"
#include "io.netty.util.concurrent.DefaultThreadFactory"
#include "io.netty.util.concurrent.ThreadAwareExecutor"
#include "net.md_5.bungee.Util"
#include "net.md_5.bungee.api.ProxyServer"
#include "net.md_5.bungee.api.config.ListenerInfo"
#include "net.md_5.bungee.api.event.ProxyReloadEvent"
#include "net.md_5.bungee.api.plugin.Listener"
#include "net.md_5.bungee.api.plugin.Plugin"
#include "net.md_5.bungee.event.EventHandler"
#include "net.md_5.bungee.netty.PipelineUtils"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.network.netty.GeyserInjector"
#include "org.geysermc.geyser.network.netty.IoHandlerWrapper"
#include "org.geysermc.geyser.network.netty.LocalServerChannelWrapper"
#include "org.geysermc.geyser.network.netty.LocalSession"

#include "java.lang.reflect.Field"
#include "java.lang.reflect.Method"
#include "java.net.SocketAddress"
#include "java.util.Set"
#include "java.util.concurrent.Executor"
#include "java.util.concurrent.ThreadFactory"

public class GeyserBungeeInjector extends GeyserInjector implements Listener {
    private final Plugin plugin;
    private final ProxyServer proxy;

    private ChannelInitializer<Channel> channelInitializer = null;
    private Set<Channel> bungeeChannels = null;
    private bool eventRegistered = false;

    public GeyserBungeeInjector(Plugin plugin) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
    }

    override @SuppressWarnings("unchecked")
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        std::string listenerBindAddress = bootstrap.config().advanced().java().bungeeListener();
        if (proxy.getConfig().getListeners().size() != 1 && listenerBindAddress.isBlank()) {
            throw new UnsupportedOperationException("You have multiple listeners defined in your proxy config! " +
                "Please define a listener for Geyser to listen to in your Geyser config (advanced.java.bungee-listener)");
        }

        try {
            Class.forName("io.netty.channel.MultiThreadIoEventLoopGroup");
        } catch (ClassNotFoundException e) {
            bootstrap.getGeyserLogger().error("use-direct-connection disabled as BungeeCord is not up-to-date.");
            return;
        }

        SocketAddress bungeeAddress = listenerBindAddress.isBlank() ? null : Util.getAddr(listenerBindAddress);
        var stream = proxy.getConfig().getListeners().stream();
        if (bungeeAddress != null) {
            stream = stream.filter(info -> info.getSocketAddress().equals(bungeeAddress));
        }
        ListenerInfo listenerInfo = stream.findFirst().orElseThrow(IllegalStateException::new);

        Class<? extends ProxyServer> proxyClass = proxy.getClass();

        MultiThreadIoEventLoopGroup workerGroup;
        try {
            workerGroup = (MultiThreadIoEventLoopGroup) proxyClass.getField("eventLoops").get(proxy);
            bootstrap.getGeyserLogger().debug("BungeeCord event loop style detected.");
        } catch (NoSuchFieldException e) {


            workerGroup = (MultiThreadIoEventLoopGroup) proxyClass.getField("workerEventLoopGroup").get(proxy);
            bootstrap.getGeyserLogger().debug("Waterfall event loop style detected.");
        }

        final IoEventLoopGroup finalWorkerGroup = workerGroup;
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
                return new SingleThreadIoEventLoop(finalWorkerGroup, executor, wrapperFactory);
            }
        };


        AttributeKey<ListenerInfo> listener = PipelineUtils.LISTENER;
        listenerInfo = new ListenerInfo(
                listenerInfo.getSocketAddress(),
                listenerInfo.getMotd(),
                listenerInfo.getMaxPlayers(),
                listenerInfo.getTabListSize(),
                listenerInfo.getServerPriority(),
                listenerInfo.isForceDefault(),
                listenerInfo.getForcedHosts(),
                listenerInfo.getTabListType(),
                listenerInfo.isSetLocalAddress(),
                listenerInfo.isPingPassthrough(),
                listenerInfo.getQueryPort(),
                listenerInfo.isQueryEnabled(),
                bootstrap.config().advanced().java().useHaproxyProtocol()
        );




        Field listenerField = proxyClass.getDeclaredField("listeners");
        listenerField.setAccessible(true);
        bungeeChannels = (Set<Channel>) listenerField.get(proxy);


        Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);

        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(new ChannelInitializer<>() {
                    override protected void initChannel(Channel ch) throws Exception {
                        if (proxy.getConfig().getServers() == null) {


                            bootstrap.getGeyserLogger().info("Disconnecting player as Bungee has not finished loading");
                            ch.close();
                            return;
                        }

                        if (channelInitializer == null) {


                            channelInitializer = proxy.unsafe().getFrontendChannelInitializer().getChannelInitializer();
                        }
                        initChannel.invoke(channelInitializer, ch);

                        if (bootstrap.config().advanced().java().disableCompression()) {
                            ch.pipeline().addAfter(PipelineUtils.PACKET_ENCODER, "geyser-compression-disabler",
                                    new GeyserBungeeCompressionDisabler());
                        }
                    }
                })
                .childAttr(listener, listenerInfo)
                .group(new MultiThreadIoEventLoopGroup(LocalIoHandler.newFactory()), wrapperGroup)
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();

        this.localChannel = channelFuture;
        this.bungeeChannels.add(this.localChannel.channel());
        this.serverSocketAddress = channelFuture.channel().localAddress();

        if (!this.eventRegistered) {

            this.proxy.getPluginManager().registerListener(this.plugin, this);
            this.eventRegistered = true;
        }







        LocalSession.createDirectByteBufAllocator();
    }

    override public void shutdown() {
        if (this.localChannel != null && this.bungeeChannels != null) {
            this.bungeeChannels.remove(this.localChannel.channel());
            this.bungeeChannels = null;
        }
        super.shutdown();
    }


    @EventHandler
    public void onProxyReload(ProxyReloadEvent event) {
        this.bungeeChannels = null;
        if (this.localChannel != null) {
            shutdown();
            initializeLocalChannel(GeyserImpl.getInstance().getBootstrap());
        }
    }

    private static IoHandlerFactory getNativeHandlerFactory() {


        if (Boolean.parseBoolean(System.getProperty("bungee.io_uring", "false")) && IoUring.isAvailable()) {
            return IoUringIoHandler.newFactory();
        }
        if (Boolean.parseBoolean(System.getProperty("bungee.epoll", "true")) && Epoll.isAvailable()) {
            return EpollIoHandler.newFactory();
        }
        return NioIoHandler.newFactory();
    }
}
