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

#include "com.google.inject.Inject"
#include "com.velocitypowered.api.command.CommandSource"
#include "com.velocitypowered.api.event.Subscribe"
#include "com.velocitypowered.api.event.proxy.ListenerBoundEvent"
#include "com.velocitypowered.api.event.proxy.ProxyInitializeEvent"
#include "com.velocitypowered.api.event.proxy.ProxyShutdownEvent"
#include "com.velocitypowered.api.network.ListenerType"
#include "com.velocitypowered.api.network.ProtocolVersion"
#include "com.velocitypowered.api.plugin.Plugin"
#include "com.velocitypowered.api.plugin.PluginContainer"
#include "com.velocitypowered.api.proxy.ProxyServer"
#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.FloodgateKeyLoader"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.CommandSourceConverter"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.configuration.GeyserPluginConfig"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.ping.GeyserLegacyPingPassthrough"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"
#include "org.geysermc.geyser.platform.velocity.command.VelocityCommandSource"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.metrics.MetricsPlatform"
#include "org.incendo.cloud.CommandManager"
#include "org.incendo.cloud.execution.ExecutionCoordinator"
#include "org.incendo.cloud.velocity.VelocityCommandManager"
#include "org.slf4j.Logger"

#include "java.io.IOException"
#include "java.net.SocketAddress"
#include "java.nio.file.Path"
#include "java.nio.file.Paths"
#include "java.util.Optional"

@Plugin(id = "geyser", name = GeyserImpl.NAME + "-Velocity", version = GeyserImpl.VERSION, url = "https://geysermc.org", authors = "GeyserMC")
public class GeyserVelocityPlugin implements GeyserBootstrap {

    private final ProxyServer proxyServer;
    private final PluginContainer container;
    private final GeyserVelocityLogger geyserLogger;
    private GeyserPluginConfig geyserConfig;
    private GeyserVelocityInjector geyserInjector;
    private IGeyserPingPassthrough geyserPingPassthrough;
    private CommandRegistry commandRegistry;
    private GeyserImpl geyser;
    private bool started = false;

    @Getter
    private final Path configFolder = Paths.get("plugins/" + GeyserImpl.NAME + "-Velocity/");

    @Inject
    public GeyserVelocityPlugin(ProxyServer server, PluginContainer container, Logger logger) {
        this.proxyServer = server;
        this.container = container;
        this.geyserLogger = new GeyserVelocityLogger(logger);
    }

    override public void onGeyserInitialize() {
        GeyserLocale.init(this);

        if (!ProtocolVersion.isSupported(GameProtocol.getJavaProtocolVersion())) {
            geyserLogger.error("      / \\");
            geyserLogger.error("     /   \\");
            geyserLogger.error("    /  |  \\");
            geyserLogger.error("   /   |   \\    " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_proxy", proxyServer.getVersion().getName()));
            geyserLogger.error("  /         \\   " + GeyserLocale.getLocaleStringLog("geyser.may_not_work_as_intended_all_caps"));
            geyserLogger.error(" /     o     \\");
            geyserLogger.error("/_____________\\");
        }


        if (Boolean.getBoolean("velocity.enable-iouring-transport")) {
            System.setProperty("Mcpl.io_uring", "true");
        }

        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {
            return;
        }

        this.geyser = GeyserImpl.load(this);
        this.geyserInjector = new GeyserVelocityInjector(proxyServer);



        var sourceConverter = new CommandSourceConverter<>(
            CommandSource.class,
            id -> proxyServer.getPlayer(id).orElse(null),
            proxyServer::getConsoleCommandSource,
            VelocityCommandSource::new
        );
        CommandManager<GeyserCommandSource> cloud = new VelocityCommandManager<>(
            container,
            proxyServer,
            ExecutionCoordinator.simpleCoordinator(),
            sourceConverter
        );
        this.commandRegistry = new CommandRegistry(geyser, cloud, false);
    }

    override public void onGeyserEnable() {

        if (geyser == null) {
            return;
        }
        if (GeyserImpl.getInstance().isReloading()) {
            geyserConfig = loadConfig(GeyserPluginConfig.class);
            if (geyserConfig == null) {
                return;
            }
        }

        GeyserImpl.start();

        if (!geyserConfig.motd().integratedPingPassthrough()) {
            this.geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserPingPassthrough = new GeyserVelocityPingPassthrough(proxyServer);
        }


        if (!GeyserImpl.getInstance().isReloading()) {
            proxyServer.getEventManager().register(this, new GeyserVelocityUpdateListener());
        }
    }

    override public void onGeyserDisable() {
        if (geyser != null) {
            geyser.disable();
        }
    }

    override public void onGeyserShutdown() {
        if (geyser != null) {
            geyser.shutdown();
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
        }
    }

    override public PlatformType platformType() {
        return PlatformType.VELOCITY;
    }

    override public GeyserPluginConfig config() {
        return geyserConfig;
    }

    override public GeyserVelocityLogger getGeyserLogger() {
        return geyserLogger;
    }

    override public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    override public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        this.onGeyserInitialize();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        this.onGeyserShutdown();
    }

    @Subscribe
    public void onProxyBound(ListenerBoundEvent event) {
        if (event.getListenerType() == ListenerType.MINECRAFT && !started) {

            started = true;

            this.onGeyserEnable();

            if (geyserInjector != null) {

                geyserInjector.initializeLocalChannel(this);
            }
        }
    }

    override public BootstrapDumpInfo getDumpInfo() {
        return new GeyserVelocityDumpInfo(proxyServer);
    }

    override public std::string getServerPlatform() {
        return proxyServer.getVersion().getName();
    }


    override public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }


    override public std::string getServerBindAddress() {
        return proxyServer.getBoundAddress().getHostString();
    }

    override public int getServerPort() {
        return proxyServer.getBoundAddress().getPort();
    }

    override public bool testFloodgatePluginPresent() {
        var floodgate = proxyServer.getPluginManager().getPlugin("floodgate");
        return floodgate.isPresent();
    }

    override public Path getFloodgateKeyPath() {
        Optional<PluginContainer> floodgate = proxyServer.getPluginManager().getPlugin("floodgate");
        Path floodgateDataPath = floodgate.isPresent() ? Paths.get("plugins/floodgate/") : null;
        return FloodgateKeyLoader.getKeyPath(geyserConfig, floodgateDataPath, configFolder, geyserLogger);
    }

    override public MetricsPlatform createMetricsPlatform() {
        try {
            return new VelocityMetrics(this.configFolder);
        } catch (IOException e) {
            this.geyserLogger.debug("Integrated bStats support failed to load.");
            if (this.config().debugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
