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
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.isolation.IsolatedPlatform;
import org.geysermc.floodgate.isolation.library.LibraryManager;
import org.geysermc.floodgate.velocity.VelocityPlatform;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.velocity.command.GeyserVelocityCommandExecutor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.slf4j.Logger;

public class GeyserVelocityPlatform implements GeyserBootstrap, IsolatedPlatform {
    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxyServer;

    @Inject
    private CommandManager commandManager;

    private GeyserCommandManager geyserCommandManager;
    private GeyserVelocityConfiguration geyserConfig;
    private GeyserVelocityInjector geyserInjector;
    private GeyserVelocityLogger geyserLogger;
    private IGeyserPingPassthrough geyserPingPassthrough;

    private GeyserImpl geyser;

    @Getter
    private final Path configFolder = Paths.get("plugins/" + GeyserImpl.NAME + "-Velocity/"); //todo remove

    @Inject Injector guice;
    @Inject LibraryManager manager; // don't remove! We don't need it in Geyser, but in Floodgate. Weird Guice stuff

    @Inject PluginContainer container;

    @Override
    public void onGeyserInitialize() {
        GeyserLocale.init(this);

        if (!ProtocolVersion.isSupported(GameProtocol.getJavaProtocolVersion())) {
            logger.error("      / \\");
            logger.error("     /   \\");
            logger.error("    /  |  \\");
            logger.error("   /   |   \\    " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_proxy", proxyServer.getVersion().getName()));
            logger.error("  /         \\   " + GeyserLocale.getLocaleStringLog("geyser.may_not_work_as_intended_all_caps"));
            logger.error(" /     o     \\");
            logger.error("/_____________\\");
        }

        if (!loadConfig()) {
            return;
        }
        this.geyserLogger = new GeyserVelocityLogger(logger, geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        FloodgatePlatform platform = null;
        if (geyserConfig.getRemote().authType() == AuthType.FLOODGATE) {
            platform = guice.getInstance(VelocityPlatform.class);
        }

        this.geyser = GeyserImpl.load(PlatformType.VELOCITY, this, platform);
        this.geyserInjector = new GeyserVelocityInjector(proxyServer);
    }

    @Override
    public void onGeyserEnable() {
        if (GeyserImpl.getInstance().isReloading()) {
            if (!loadConfig()) {
                return;
            }
            this.geyserLogger.setDebug(geyserConfig.isDebugMode());
            GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        } else {
            this.geyserCommandManager = new GeyserCommandManager(geyser);
            this.geyserCommandManager.init();
        }

        GeyserImpl.start();

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserPingPassthrough = new GeyserVelocityPingPassthrough(proxyServer);
        }

        // No need to re-register commands when reloading
        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        this.commandManager.register("geyser", new GeyserVelocityCommandExecutor(geyser, geyserCommandManager.getCommands()));
        for (Map.Entry<Extension, Map<String, Command>> entry : this.geyserCommandManager.extensionCommands().entrySet()) {
            Map<String, Command> commands = entry.getValue();
            if (commands.isEmpty()) {
                continue;
            }

            this.commandManager.register(entry.getKey().description().id(), new GeyserVelocityCommandExecutor(this.geyser, commands));
        }

        proxyServer.getEventManager().register(container, new GeyserVelocityUpdateListener());
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
    public GeyserVelocityConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserVelocityLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public GeyserCommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserVelocityDumpInfo(proxyServer);
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
        if (floodgate.isPresent()) {
            geyserConfig.loadFloodgate(this, proxyServer, configFolder.toFile());
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean loadConfig() {
        try {
            if (!configFolder.toFile().exists())
                //noinspection ResultOfMethodCallIgnored
                configFolder.toFile().mkdirs();
            File configFile = FileUtils.fileOrCopiedFromResource(configFolder.resolve("config.yml").toFile(),
                    "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserVelocityConfiguration.class);
        } catch (IOException ex) {
            logger.error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            return false;
        }
        return true;
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
}
