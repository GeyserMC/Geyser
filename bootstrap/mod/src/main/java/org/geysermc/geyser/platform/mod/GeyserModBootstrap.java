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

package org.geysermc.geyser.platform.mod;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.mod.platform.GeyserModPlatform;
import org.geysermc.geyser.platform.mod.world.GeyserModWorldManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class GeyserModBootstrap implements GeyserBootstrap {

    @Getter
    private static GeyserModBootstrap instance;

    private final GeyserModPlatform platform;

    @Getter
    private GeyserImpl geyser;
    private Path dataFolder;

    @Setter @Getter
    private MinecraftServer server;

    @Setter
    private CommandRegistry commandRegistry;
    private GeyserModConfiguration geyserConfig;
    private GeyserModInjector geyserInjector;
    private final GeyserModLogger geyserLogger = new GeyserModLogger();
    private IGeyserPingPassthrough geyserPingPassthrough;
    private WorldManager geyserWorldManager;

    @Override
    public void onGeyserInitialize() {
        instance = this;
        dataFolder = this.platform.dataFolder(this.platform.configPath());
        GeyserLocale.init(this);
        if (!loadConfig()) {
            return;
        }
        this.geyserLogger.setDebug(geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        this.geyser = GeyserImpl.load(this.platform.platformType(), this);
    }

    public void onGeyserEnable() {
        if (GeyserImpl.getInstance().isReloading()) {
            if (!loadConfig()) {
                return;
            }
            this.geyserLogger.setDebug(geyserConfig.isDebugMode());
            GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        }

        GeyserImpl.start();

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            this.geyserPingPassthrough = new ModPingPassthrough(server, geyserLogger);
        }

        // No need to re-register commands, or try to re-inject
        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        this.geyserWorldManager = new GeyserModWorldManager(server);

        // We want to do this late in the server startup process to allow other mods
        // To do their job injecting, then connect into *that*
        this.geyserInjector = new GeyserModInjector(server, this.platform);
        if (isServer()) {
            this.geyserInjector.initializeLocalChannel(this);
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
            geyser = null;
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
            this.server = null;
        }
    }

    @Override
    public GeyserModConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Override
    public WorldManager getWorldManager() {
        return geyserWorldManager;
    }

    @Override
    public Path getConfigFolder() {
        return dataFolder;
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return this.platform.dumpInfo(this.server);
    }

    @Override
    public String getMinecraftServerVersion() {
        return this.server.getServerVersion();
    }

    @SuppressWarnings("ConstantConditions") // Certain IDEA installations think that ip cannot be null
    @NonNull
    @Override
    public String getServerBindAddress() {
        String ip = this.server.getLocalIp();
        return ip != null ? ip : ""; // See issue #3812
    }

    @Override
    public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }

    @Override
    public int getServerPort() {
        if (isServer()) {
            return ((GeyserServerPortGetter) server).geyser$getServerPort();
        } else {
            // Set in the IntegratedServerMixin
            return geyserConfig.getRemote().port();
        }
    }

    public abstract boolean isServer();

    @Override
    public boolean testFloodgatePluginPresent() {
        return this.platform.testFloodgatePluginPresent(this);
    }

    @Nullable
    @Override
    public InputStream getResourceOrNull(String resource) {
        return this.platform.resolveResource(resource);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean loadConfig() {
        try {
            if (!dataFolder.toFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                dataFolder.toFile().mkdir();
            }

            File configFile = FileUtils.fileOrCopiedFromResource(dataFolder.resolve("config.yml").toFile(), "config.yml",
                    (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserModConfiguration.class);
            return true;
        } catch (IOException ex) {
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            return false;
        }
    }
}
