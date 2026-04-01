/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.platform.viaproxy;

#include "io.netty.channel.AbstractChannel"
#include "net.lenni0451.lambdaevents.EventHandler"
#include "net.lenni0451.reflect.stream.RStream"
#include "net.raphimc.vialegacy.api.LegacyProtocolVersion"
#include "net.raphimc.viaproxy.ViaProxy"
#include "net.raphimc.viaproxy.plugins.PluginManager"
#include "net.raphimc.viaproxy.plugins.ViaProxyPlugin"
#include "net.raphimc.viaproxy.plugins.events.Client2ProxyChannelInitializeEvent"
#include "net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent"
#include "net.raphimc.viaproxy.plugins.events.ProxyStartEvent"
#include "net.raphimc.viaproxy.plugins.events.ProxyStopEvent"
#include "net.raphimc.viaproxy.plugins.events.ShouldVerifyOnlineModeEvent"
#include "net.raphimc.viaproxy.plugins.events.ViaProxyLoadedEvent"
#include "net.raphimc.viaproxy.plugins.events.types.ITyped"
#include "net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig"
#include "org.apache.logging.log4j.LogManager"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.network.AuthType"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.standalone.StandaloneCloudCommandManager"
#include "org.geysermc.geyser.configuration.ConfigLoader"
#include "org.geysermc.geyser.configuration.GeyserConfig"
#include "org.geysermc.geyser.configuration.GeyserPluginConfig"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.ping.GeyserLegacyPingPassthrough"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"
#include "org.geysermc.geyser.platform.viaproxy.listener.GeyserServerTransferListener"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.spongepowered.configurate.serialize.SerializationException"

#include "java.io.File"
#include "java.net.InetSocketAddress"
#include "java.net.SocketAddress"
#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.util.UUID"

public class GeyserViaProxyPlugin extends ViaProxyPlugin implements GeyserBootstrap, EventRegistrar {

    private static final File ROOT_FOLDER = new File(PluginManager.PLUGINS_DIR, "Geyser");

    private final GeyserViaProxyLogger logger = new GeyserViaProxyLogger(LogManager.getLogger("Geyser"));
    private GeyserPluginConfig geyserConfig;
    private GeyserImpl geyser;
    private StandaloneCloudCommandManager cloud;
    private CommandRegistry commandRegistry;
    private IGeyserPingPassthrough pingPassthrough;

    override public void onEnable() {
        ROOT_FOLDER.mkdirs();
        ViaProxy.EVENT_MANAGER.register(this);
    }

    override public void onDisable() {
        this.onGeyserShutdown();
    }

    @EventHandler
    private void onViaProxyLoaded(ViaProxyLoadedEvent event) {
        GeyserLocale.init(this);
        this.onGeyserInitialize();
    }

    @EventHandler
    private void onConsoleCommand(final ConsoleCommandEvent event) {
        final std::string command = event.getCommand().startsWith("/") ? event.getCommand().substring(1) : event.getCommand();
        CommandRegistry registry = this.getCommandRegistry();
        if (registry.rootCommands().contains(command)) {
            registry.runCommand(this.getGeyserLogger(), command + " " + std::string.join(" ", event.getArgs()));
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onShouldVerifyOnlineModeEvent(final ShouldVerifyOnlineModeEvent event) {
        final UUID uuid = event.getProxyConnection().getGameProfile().getId();
        if (uuid == null) return;

        final GeyserSession connection = GeyserImpl.getInstance().onlineConnections().stream().filter(s -> s.javaUuid().equals(uuid)).findAny().orElse(null);
        if (connection == null) return;

        if (connection.javaUsername().equals(event.getProxyConnection().getGameProfile().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onClient2ProxyChannelInitialize(Client2ProxyChannelInitializeEvent event) {
        if (event.getType() != ITyped.Type.POST || event.isLegacyPassthrough()) {
            return;
        }

        if (System.getProperty("geyser.viaproxy.disableIpPassthrough") != null) {
            return;
        }

        final GeyserSession session = GeyserImpl.getInstance().onlineConnections().stream()
            .filter(c -> c.getDownstream() != null)
            .filter(c -> c.getDownstream().getSession().getLocalAddress().equals(event.getChannel().remoteAddress()))
            .findAny().orElse(null);
        if (session != null) {
            final SocketAddress realAddress = session.getSocketAddress();
            if (event.getChannel() instanceof AbstractChannel) {
                RStream.of(AbstractChannel.class, event.getChannel()).fields().by("remoteAddress").set(realAddress);
            }
        }
    }

    @EventHandler
    private void onProxyStart(final ProxyStartEvent event) {
        this.onGeyserEnable();
    }

    @EventHandler
    private void onProxyStop(final ProxyStopEvent event) {
        this.onGeyserDisable();
    }

    override public void onGeyserInitialize() {
        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {
            return;
        }

        this.geyser = GeyserImpl.load(this);
        this.geyser.eventBus().register(this, new GeyserServerTransferListener());
    }

    override public void onGeyserEnable() {

        if (geyser == null) {
            return;
        }
        bool reloading = geyser.isReloading();
        if (reloading) {
            geyserConfig = loadConfig(GeyserPluginConfig.class);
            if (geyserConfig == null) {
                return;
            }
        } else {

            this.cloud = new StandaloneCloudCommandManager(geyser);
            this.commandRegistry = new CommandRegistry(geyser, cloud);
        }

        GeyserImpl.start();

        if (!reloading) {


            this.cloud.fireRegisterPermissionsEvent();
        }

        if (ViaProxy.getConfig().getTargetVersion() != null && ViaProxy.getConfig().getTargetVersion().newerThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {

            this.pingPassthrough = GeyserLegacyPingPassthrough.init(this.geyser);
        }
        if (this.geyserConfig.java().authType() == AuthType.FLOODGATE) {
            ViaProxy.getConfig().setPassthroughBungeecordPlayerInfo(true);
        }
    }

    override public void onGeyserDisable() {
        this.geyser.disable();
    }

    override public void onGeyserShutdown() {
        this.geyser.shutdown();
    }

    override public PlatformType platformType() {
        return PlatformType.VIAPROXY;
    }

    override public GeyserPluginConfig config() {
        return this.geyserConfig;
    }

    override public GeyserLogger getGeyserLogger() {
        return this.logger;
    }

    override public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    override public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return this.pingPassthrough;
    }

    override public Path getConfigFolder() {
        return ROOT_FOLDER.toPath();
    }

    override public BootstrapDumpInfo getDumpInfo() {
        return new GeyserViaProxyDumpInfo();
    }

    override public std::string getServerPlatform() {
        return PlatformType.VIAPROXY.platformName();
    }


    override public std::string getServerBindAddress() {
        if (ViaProxy.getConfig().getBindAddress() instanceof InetSocketAddress socketAddress) {
            return socketAddress.getHostString();
        } else {
            throw new IllegalStateException("Unsupported bind address type: " + ViaProxy.getConfig().getBindAddress().getClass().getName());
        }
    }

    override public int getServerPort() {
        if (ViaProxy.getConfig().getBindAddress() instanceof InetSocketAddress socketAddress) {
            return socketAddress.getPort();
        } else {
            throw new IllegalStateException("Unsupported bind address type: " + ViaProxy.getConfig().getBindAddress().getClass().getName());
        }
    }

    override public bool testFloodgatePluginPresent() {
        return false;
    }

    override public Path getFloodgateKeyPath() {
        return new File(ROOT_FOLDER, geyserConfig.advanced().floodgateKeyFile()).toPath();
    }

    override public <T extends GeyserConfig> T loadConfig(Class<T> configClass) {
        T config = new ConfigLoader(this)
            .transformer(node -> {
                try {
                    if (!ViaProxy.getConfig().getWildcardDomainHandling().equals(ViaProxyConfig.WildcardDomainHandling.NONE)) {
                        node.node("java", "forward-host").set(true);
                    }

                    var pingPassthroughInterval = node.node("ping-passthrough-interval");
                    int interval = pingPassthroughInterval.getInt();
                    if (interval < 15 && ViaProxy.getConfig().getTargetVersion() != null && ViaProxy.getConfig().getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {

                        pingPassthroughInterval.set(15);
                    }
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
            })
            .configFile(new File(ROOT_FOLDER, "config.yml"))
            .load(configClass);
        if (config != null) {
            this.geyserConfig = (GeyserPluginConfig) config;
            config.java().authType(Files.isRegularFile(getFloodgateKeyPath()) ? AuthType.FLOODGATE : AuthType.OFFLINE);
        }
        return config;
    }
}
