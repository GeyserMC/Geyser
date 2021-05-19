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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.util.AttributeKey;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.ping.GeyserLegacyPingPassthrough;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.bungeecord.command.GeyserBungeeCommandExecutor;
import org.geysermc.platform.bungeecord.command.GeyserBungeeCommandManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

public class GeyserBungeePlugin extends Plugin implements GeyserBootstrap {

    private GeyserBungeeCommandManager geyserCommandManager;
    private GeyserBungeeConfiguration geyserConfig;
    private GeyserBungeeLogger geyserLogger;
    private IGeyserPingPassthrough geyserBungeePingPassthrough;

    private GeyserConnector connector;

    private SocketAddress serverSocketAddress;
    private ChannelFuture localChannel;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File configFile = FileUtils.fileOrCopiedFromResource(new File(getDataFolder(), "config.yml"), "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserBungeeConfiguration.class);
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, LanguageUtils.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
        }

        if (getProxy().getConfig().getListeners().size() == 1) {
            ListenerInfo listener = getProxy().getConfig().getListeners().toArray(new ListenerInfo[0])[0];

            InetSocketAddress javaAddr = listener.getHost();

            // By default this should be localhost but may need to be changed in some circumstances
            if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
                this.geyserConfig.setAutoconfiguredRemote(true);
                // Don't use localhost if not listening on all interfaces
                if (!javaAddr.getHostString().equals("0.0.0.0") && !javaAddr.getHostString().equals("")) {
                    this.geyserConfig.getRemote().setAddress(javaAddr.getHostString());
                }
                this.geyserConfig.getRemote().setPort(javaAddr.getPort());
            }

            if (geyserConfig.getBedrock().isCloneRemotePort()) {
                geyserConfig.getBedrock().setPort(javaAddr.getPort());
            }
        }

        this.geyserLogger = new GeyserBungeeLogger(getLogger(), geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        if (geyserConfig.getRemote().getAuthType().equals("floodgate") && getProxy().getPluginManager().getPlugin("floodgate-bungee") == null) {
            geyserLogger.severe(LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
            return;
        } else if (geyserConfig.isAutoconfiguredRemote() && getProxy().getPluginManager().getPlugin("floodgate-bungee") != null) {
            // Floodgate installed means that the user wants Floodgate authentication
            geyserLogger.debug("Auto-setting to Floodgate authentication.");
            geyserConfig.getRemote().setAuthType("floodgate");
        }

        geyserConfig.loadFloodgate(this);

        this.connector = GeyserConnector.start(PlatformType.BUNGEECORD, this);

        try {
            this.serverSocketAddress = initializeLocalChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.geyserCommandManager = new GeyserBungeeCommandManager(connector);

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserBungeePingPassthrough = GeyserLegacyPingPassthrough.init(connector);
        } else {
            this.geyserBungeePingPassthrough = new GeyserBungeePingPassthrough(getProxy());
        }

        this.getProxy().getPluginManager().registerCommand(this, new GeyserBungeeCommandExecutor(connector));
    }

    @Override
    public void onDisable() {
        connector.shutdown();
        if (this.localChannel != null) {
            try {
                localChannel.channel().close().sync();
                localChannel = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private SocketAddress initializeLocalChannel() throws Exception {
        Class<?> pipelineUtils = Class.forName("net.md_5.bungee.netty.PipelineUtils");
        ChannelInitializer<Channel> channelInitializer = (ChannelInitializer<Channel>) pipelineUtils.getField("SERVER_CHILD").get(null);

        Class<? extends ProxyServer> proxyClass = getProxy().getClass();
        // Using the specified EventLoop is required, or else an error will be thrown
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        try {
            EventLoopGroup eventLoops = (EventLoopGroup) proxyClass.getField("eventLoops").get(getProxy());
            // Netty redirects ServerBootstrap#group(EventLoopGroup) to #group(EventLoopGroup, EventLoopGroup) and uses the same event loop for both.
            bossGroup = eventLoops;
            workerGroup = eventLoops;
            geyserLogger.debug("BungeeCord event loop style detected.");
        } catch (NoSuchFieldException e) {
            // Waterfall uses two separate event loops
            // https://github.com/PaperMC/Waterfall/blob/fea7ec356dba6c6ac28819ff11be604af6eb484e/BungeeCord-Patches/0022-Use-a-worker-and-a-boss-event-loop-group.patch
            bossGroup = (EventLoopGroup) proxyClass.getField("bossEventLoopGroup").get(getProxy());
            workerGroup = (EventLoopGroup) proxyClass.getField("workerEventLoopGroup").get(getProxy());
            geyserLogger.debug("Waterfall event loop style detected.");
        }

        // Is currently just AttributeKey.valueOf("ListerInfo") but we might as well copy the value itself.
        AttributeKey<ListenerInfo> listener = (AttributeKey<ListenerInfo>) pipelineUtils.getField("LISTENER").get(null);
        //TODO define our own ListenerInfo?
        ListenerInfo listenerInfo = getProxy().getConfig().getListeners().stream().findFirst().orElseThrow(IllegalStateException::new);

        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = channelInitializer.getClass().getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);

        ChannelFuture channelFuture = (new ServerBootstrap().channel(LocalServerChannel.class).childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                initChannel.invoke(channelInitializer, ch);
            }
        }).childAttr(listener, listenerInfo).group(bossGroup, workerGroup).localAddress(LocalAddress.ANY)).bind().syncUninterruptibly();
        this.localChannel = channelFuture;

        return channelFuture.channel().localAddress();
    }

    @Override
    public GeyserBungeeConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserBungeeLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserBungeePingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserBungeeDumpInfo(getProxy());
    }

    @Nullable
    @Override
    public SocketAddress getSocketAddress() {
        return serverSocketAddress;
    }
}
