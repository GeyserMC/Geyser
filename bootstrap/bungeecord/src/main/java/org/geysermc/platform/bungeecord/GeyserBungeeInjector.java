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

package org.geysermc.platform.bungeecord;

import com.github.steveice10.packetlib.io.local.LocalServerChannelWrapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.util.AttributeKey;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.netty.PipelineUtils;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.common.GeyserInjector;

import java.lang.reflect.Method;

public class GeyserBungeeInjector extends GeyserInjector {
    private final ProxyServer proxy;
    /**
     * Set as a variable so it is only set after the proxy has finished initializing
     */
    private ChannelInitializer<Channel> channelInitializer = null;

    public GeyserBungeeInjector(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        // TODO - allow Geyser to specify its own listener info properties
        if (proxy.getConfig().getListeners().size() != 1) {
            throw new UnsupportedOperationException("We currently do not support multiple listeners with injection! " +
                    "Please reach out to us on our Discord so we can hear feedback on your setup.");
        }
        ListenerInfo listenerInfo = proxy.getConfig().getListeners().stream().findFirst().orElseThrow(IllegalStateException::new);

        Class<? extends ProxyServer> proxyClass = proxy.getClass();
        // Using the specified EventLoop is required, or else an error will be thrown
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        try {
            EventLoopGroup eventLoops = (EventLoopGroup) proxyClass.getField("eventLoops").get(proxy);
            // Netty redirects ServerBootstrap#group(EventLoopGroup) to #group(EventLoopGroup, EventLoopGroup) and uses the same event loop for both.
            bossGroup = eventLoops;
            workerGroup = eventLoops;
            bootstrap.getGeyserLogger().debug("BungeeCord event loop style detected.");
        } catch (NoSuchFieldException e) {
            // Waterfall uses two separate event loops
            // https://github.com/PaperMC/Waterfall/blob/fea7ec356dba6c6ac28819ff11be604af6eb484e/BungeeCord-Patches/0022-Use-a-worker-and-a-boss-event-loop-group.patch
            bossGroup = (EventLoopGroup) proxyClass.getField("bossEventLoopGroup").get(proxy);
            workerGroup = (EventLoopGroup) proxyClass.getField("workerEventLoopGroup").get(proxy);
            bootstrap.getGeyserLogger().debug("Waterfall event loop style detected.");
        }

        // Is currently just AttributeKey.valueOf("ListerInfo") but we might as well copy the value itself.
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
                bootstrap.getGeyserConfig().getRemote().isUseProxyProtocol() // If Geyser is expecting HAProxy, so should the Bungee end
        );

        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);

        ChannelFuture channelFuture = (new ServerBootstrap().channel(LocalServerChannelWrapper.class).childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                if (!((BungeeCord) proxy).isRunning) {
                    // Proxy hasn't finished; yeet
                    ch.close();
                    return;
                }

                if (channelInitializer == null) {
                    // Proxy has finished initializing; we can safely grab this variable without fear of plugins modifying it
                    // (ViaVersion replaces this to inject)
                    channelInitializer = PipelineUtils.SERVER_CHILD;
                }
                initChannel.invoke(channelInitializer, ch);
            }
        }).childAttr(listener, listenerInfo).group(bossGroup, workerGroup).localAddress(LocalAddress.ANY)).bind().syncUninterruptibly();

        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();
    }
}
