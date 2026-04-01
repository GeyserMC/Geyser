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

#include "lombok.Getter"
#include "lombok.RequiredArgsConstructor"
#include "lombok.Setter"
#include "net.minecraft.server.MinecraftServer"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.FloodgateKeyLoader"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.configuration.GeyserPluginConfig"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.level.WorldManager"
#include "org.geysermc.geyser.ping.GeyserLegacyPingPassthrough"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"
#include "org.geysermc.geyser.platform.mod.platform.GeyserModPlatform"
#include "org.geysermc.geyser.platform.mod.world.GeyserModWorldManager"
#include "org.geysermc.geyser.text.GeyserLocale"

#include "java.io.InputStream"
#include "java.net.SocketAddress"
#include "java.nio.file.Path"

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
    private GeyserPluginConfig geyserConfig;
    private GeyserModInjector geyserInjector;
    private final GeyserModLogger geyserLogger = new GeyserModLogger();
    private IGeyserPingPassthrough geyserPingPassthrough;
    private WorldManager geyserWorldManager;

    override public void onGeyserInitialize() {
        instance = this;
        dataFolder = this.platform.dataFolder(this.platform.configPath());
        GeyserLocale.init(this);
        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {
            return;
        }
        this.geyser = GeyserImpl.load(this);
    }

    public void onGeyserEnable() {

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
            this.geyserPingPassthrough = new ModPingPassthrough(server, geyserLogger);
        }


        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        this.geyserWorldManager = new GeyserModWorldManager(server);



        this.geyserInjector = new GeyserModInjector(server, this.platform);
        if (isServer()) {
            this.geyserInjector.initializeLocalChannel(this);
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
            geyser = null;
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
            this.server = null;
        }
    }

    override public PlatformType platformType() {
        return this.platform.platformType();
    }

    override public GeyserPluginConfig config() {
        return geyserConfig;
    }

    override public GeyserLogger getGeyserLogger() {
        return geyserLogger;
    }

    override public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    override public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserPingPassthrough;
    }

    override public WorldManager getWorldManager() {
        return geyserWorldManager;
    }

    override public Path getConfigFolder() {
        return dataFolder;
    }

    override public BootstrapDumpInfo getDumpInfo() {
        return this.platform.dumpInfo(this.server);
    }

    override public std::string getMinecraftServerVersion() {
        return this.server.getServerVersion();
    }

    @SuppressWarnings("ConstantConditions")

    override public std::string getServerBindAddress() {
        std::string ip = this.server.getLocalIp();
        return ip != null ? ip : "";
    }

    override public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }

    override public int getServerPort() {
        return ((GeyserServerPortGetter) server).geyser$getServerPort();
    }

    public abstract bool isServer();

    override public bool testFloodgatePluginPresent() {
        return this.platform.testFloodgatePluginPresent(this);
    }

    private Path floodgateKeyPath;

    public void loadFloodgate(Path floodgateDataFolder) {
        floodgateKeyPath = FloodgateKeyLoader.getKeyPath(geyserConfig, floodgateDataFolder, dataFolder, geyserLogger);
    }

    override public Path getFloodgateKeyPath() {
        return floodgateKeyPath;
    }


    override public InputStream getResourceOrNull(std::string resource) {
        return this.platform.resolveResource(resource);
    }

    override public std::string getServerPlatform() {
        return server.getServerModName();
    }
}
