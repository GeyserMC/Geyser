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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.isolation.IsolatedPlatform;
import org.geysermc.floodgate.isolation.library.LibraryManager;
import org.geysermc.floodgate.velocity.VelocityPlatform;
import org.geysermc.geyser.FloodgateKeyLoader;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.CommandSourceConverter;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.configuration.GeyserPluginConfig;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.velocity.command.VelocityCommandSource;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.metrics.MetricsPlatform;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.VelocityCommandManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class GeyserVelocityPlatform implements GeyserBootstrap, IsolatedPlatform {
    private final ProxyServer proxyServer;
    private final PluginContainer container;
    private final GeyserVelocityLogger geyserLogger;
    private GeyserPluginConfig geyserConfig;
    private GeyserVelocityInjector geyserInjector;
    private IGeyserPingPassthrough geyserPingPassthrough;
    private CommandRegistry commandRegistry;
    private GeyserImpl geyser;

    @Getter
    private final Path configFolder = Paths.get("plugins/" + GeyserImpl.NAME + "-Velocity/"); //todo remove

    @Inject Injector guice;
    @Inject LibraryManager manager; // don't remove! We don't need it in Geyser, but in Floodgate. Weird Guice stuff

    @Inject
    public GeyserVelocityPlatform(ProxyServer server, PluginContainer container, Logger logger) {
        this.proxyServer = server;
        this.container = container;
        this.geyserLogger = new GeyserVelocityLogger(logger);
    }

    @Override
    public void onGeyserInitialize() {
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

        // Only use io_uring when velocity does as well
        if (Boolean.getBoolean("velocity.enable-iouring-transport")) {
            System.setProperty("Mcpl.io_uring", "true");
        }

        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {
            return;
        }

        this.geyser = GeyserImpl.load(this);
        this.geyserInjector = new GeyserVelocityInjector(proxyServer);

        // We need to register commands here, rather than in onGeyserEnable which is invoked during the appropriate ListenerBoundEvent.
        // Reason: players can connect after a listener is bound, and a player join locks registration to the cloud CommandManager.
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
        this.commandRegistry = new CommandRegistry(geyser, cloud);
    }

    @Override
    public void onGeyserEnable() {
        // If e.g. the config failed to load, GeyserImpl was not loaded and we cannot start
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

        // No need to re-register events
        if (!GeyserImpl.getInstance().isReloading()) {
            proxyServer.getEventManager().register(container, new GeyserVelocityUpdateListener());
        }
    }

    @Override
    public void onGeyserDisable() {
        if (geyser != null) {
            geyser.disable();
        }
    }

    @Override
    public void onGeyserShutdown() {
        if (geyser != null) {
            geyser.shutdown();
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
        }
    }

    @Override
    public @NonNull PlatformType platformType() {
        return PlatformType.VELOCITY;
    }

    @Override
    public GeyserPluginConfig config() {
        return geyserConfig;
    }

    @Override
    public GeyserVelocityLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserVelocityDumpInfo(proxyServer);
    }

    @Override
    public @NonNull String getServerPlatform() {
        return proxyServer.getVersion().getName();
    }

    @Nullable
    @Override
    public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }

    @NonNull
    @Override
    public String getServerBindAddress() {
        return proxyServer.getBoundAddress().getHostString();
    }

    @Override
    public int getServerPort() {
        return proxyServer.getBoundAddress().getPort();
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        var floodgate = proxyServer.getPluginManager().getPlugin("floodgate");
        return floodgate.isPresent();
    }

    @Override
    public Path getFloodgateKeyPath() {
        Optional<PluginContainer> floodgate = proxyServer.getPluginManager().getPlugin("floodgate");
        Path floodgateDataPath = floodgate.isPresent() ? Paths.get("plugins/floodgate/") : null;
        return FloodgateKeyLoader.getKeyPath(geyserConfig, floodgateDataPath, configFolder, geyserLogger);
    }

    @Override
    public @Nullable FloodgatePlatform floodgatePlatform() {
        return guice.getInstance(VelocityPlatform.class);
    }

    @Override
    public MetricsPlatform createMetricsPlatform() {
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

    @Override
    public void load() {
        this.onGeyserInitialize();
    }

    @Override
    public void enable() {
        this.onGeyserEnable();

        if (geyserInjector != null) {
            // After this bound, we know that the channel initializer cannot change without it being ineffective for Velocity, too
            geyserInjector.initializeLocalChannel(this);
        }
    }

    @Override
    public void disable() {
        this.onGeyserShutdown();
    }

    @Override
    public void shutdown() {
        this.onGeyserShutdown();
    }
}
