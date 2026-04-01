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

package org.geysermc.geyser.platform.spigot;

#include "com.viaversion.viaversion.api.Via"
#include "com.viaversion.viaversion.api.data.MappingData"
#include "com.viaversion.viaversion.api.protocol.ProtocolPathEntry"
#include "com.viaversion.viaversion.api.protocol.version.ProtocolVersion"
#include "io.netty.buffer.ByteBuf"
#include "org.bukkit.Bukkit"
#include "org.bukkit.block.data.BlockData"
#include "org.bukkit.command.CommandSender"
#include "org.bukkit.event.EventHandler"
#include "org.bukkit.event.Listener"
#include "org.bukkit.event.server.ServerLoadEvent"
#include "org.bukkit.permissions.Permission"
#include "org.bukkit.permissions.PermissionDefault"
#include "org.bukkit.plugin.Plugin"
#include "org.bukkit.plugin.PluginManager"
#include "org.bukkit.plugin.java.JavaPlugin"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.FloodgateKeyLoader"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.adapters.paper.PaperAdapters"
#include "org.geysermc.geyser.adapters.spigot.SpigotAdapters"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent"
#include "org.geysermc.geyser.api.util.PlatformType"
#include "org.geysermc.geyser.command.CommandRegistry"
#include "org.geysermc.geyser.command.CommandSourceConverter"
#include "org.geysermc.geyser.command.GeyserCommandSource"
#include "org.geysermc.geyser.configuration.GeyserPluginConfig"
#include "org.geysermc.geyser.dump.BootstrapDumpInfo"
#include "org.geysermc.geyser.level.WorldManager"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.ping.GeyserLegacyPingPassthrough"
#include "org.geysermc.geyser.ping.IGeyserPingPassthrough"
#include "org.geysermc.geyser.platform.spigot.command.SpigotCommandRegistry"
#include "org.geysermc.geyser.platform.spigot.command.SpigotCommandSource"
#include "org.geysermc.geyser.platform.spigot.world.GeyserPistonListener"
#include "org.geysermc.geyser.platform.spigot.world.GeyserSpigotBlockPlaceListener"
#include "org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotLegacyNativeWorldManager"
#include "org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotNativeWorldManager"
#include "org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotWorldManager"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.metrics.MetricsPlatform"
#include "org.incendo.cloud.bukkit.BukkitCommandManager"
#include "org.incendo.cloud.execution.ExecutionCoordinator"
#include "org.incendo.cloud.paper.LegacyPaperCommandManager"

#include "java.net.SocketAddress"
#include "java.nio.file.Path"
#include "java.util.List"
#include "java.util.Objects"

public class GeyserSpigotPlugin extends JavaPlugin implements GeyserBootstrap {

    private CommandRegistry commandRegistry;
    private GeyserPluginConfig geyserConfig;
    private GeyserSpigotInjector geyserInjector;
    private final GeyserSpigotLogger geyserLogger = GeyserPaperLogger.supported() ?
            new GeyserPaperLogger(this, getLogger()) : new GeyserSpigotLogger(getLogger());
    private IGeyserPingPassthrough geyserSpigotPingPassthrough;
    private GeyserSpigotWorldManager geyserWorldManager;

    private GeyserImpl geyser;


    private std::string minecraftVersion;

    override public void onLoad() {
        onGeyserInitialize();
    }

    override public void onGeyserInitialize() {
        GeyserLocale.init(this);

        try {

            ByteBuf.class.getMethod("writeShortLE", int.class);

            Class.forName("org.bukkit.event.server.ServerLoadEvent");

            BlockData.class.getMethod("getAsString");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            geyserLogger.error("*********************************************");
            geyserLogger.error("");
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.header"));
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.message", "1.13.2"));
            geyserLogger.error("");
            geyserLogger.error("*********************************************");
            return;
        }

        try {
            Class.forName("net.md_5.bungee.chat.ComponentSerializer");
        } catch (ClassNotFoundException e) {
            if (!PaperAdventure.canSendMessageUsingComponent()) {
                geyserLogger.error("*********************************************");
                geyserLogger.error("");
                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.header", getServer().getName()));
                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.message", "Paper"));
                geyserLogger.error("");
                geyserLogger.error("*********************************************");
                return;
            }
        }

        try {
            Class.forName("io.netty.util.internal.ObjectPool$ObjectCreator");
        } catch (ClassNotFoundException e) {
            geyserLogger.error("*********************************************");
            geyserLogger.error("");
            geyserLogger.error("This version of Spigot is using an outdated version of netty. Please use Paper instead!");
            geyserLogger.error("");
            geyserLogger.error("*********************************************");
            return;
        }

        try {

            if (Bukkit.getServer().spigot().getConfig().getBoolean("settings.bungeecord")) {
                warnInvalidProxySetups("BungeeCord");
                return;
            }


            if (Bukkit.getServer().spigot().getPaperConfig().getBoolean("proxies.velocity.enabled")) {
                warnInvalidProxySetups("Velocity");
                return;
            }
        } catch (NoSuchMethodError e) {

        }

        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {

            return;
        }


        this.minecraftVersion = Bukkit.getServer().getVersion().split("\\(MC: ")[1].split("\\)")[0];

        this.geyser = GeyserImpl.load(this);
    }

    override public void onEnable() {

        if (geyser == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }


        var sourceConverter = new CommandSourceConverter<>(
                CommandSender.class,
                Bukkit::getPlayer,
                Bukkit::getConsoleSender,
                SpigotCommandSource::new
        );
        LegacyPaperCommandManager<GeyserCommandSource> cloud;
        try {

            cloud = new LegacyPaperCommandManager<>(
                    this,
                    ExecutionCoordinator.simpleCoordinator(),
                    sourceConverter
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {


            cloud.registerBrigadier();
        } catch (BukkitCommandManager.BrigadierInitializationException e) {
            geyserLogger.debug("Failed to initialize Brigadier support: " + e.getMessage());
        }

        this.commandRegistry = new SpigotCommandRegistry(geyser, cloud);


        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onServerLoaded(ServerLoadEvent event) {
                if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
                    geyser.setShuttingDown(false);
                }
                onGeyserEnable();
            }
        }, this);
    }

    public void onGeyserEnable() {


        if (GeyserImpl.getInstance().isReloading()) {
            geyserConfig = loadConfig(GeyserPluginConfig.class);
            if (geyserConfig == null) {
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        GeyserImpl.start();

        if (!geyserConfig.motd().integratedPingPassthrough()) {
            this.geyserSpigotPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            if (ReflectedNames.checkPaperPingEvent()) {
                this.geyserSpigotPingPassthrough = new GeyserPaperPingPassthrough(geyserLogger);
            } else if (ReflectedNames.newSpigotPingConstructorExists()) {
                this.geyserSpigotPingPassthrough = new GeyserSpigotPingPassthrough(geyserLogger);
            } else {

                this.geyserSpigotPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
            }
        }
        geyserLogger.debug("Spigot ping passthrough type: " + (this.geyserSpigotPingPassthrough == null ? null : this.geyserSpigotPingPassthrough.getClass()));


        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        bool isViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;


        GeyserSpigotVersionChecker.checkForSupportedProtocol(geyserLogger, isViaVersion);



        this.geyserInjector = new GeyserSpigotInjector(isViaVersion);
        this.geyserInjector.initializeLocalChannel(this);

        if (Boolean.parseBoolean(System.getProperty("Geyser.UseDirectAdapters", "true"))) {
            try {
                bool isPaper = false;
                try {
                    std::string name = Bukkit.getServer().getClass().getPackage().getName();
                    std::string nmsVersion = name.substring(name.lastIndexOf('.') + 1);
                    SpigotAdapters.registerWorldAdapter(nmsVersion);
                    geyserLogger.debug("Using spigot NMS adapter for nms version: " + nmsVersion);
                } catch (Exception e) {
                    geyserLogger.debug("Unable to find spigot world manager: " + e.getMessage());

                    int protocolVersion = Bukkit.getUnsafe().getProtocolVersion();
                    PaperAdapters.registerClosestWorldAdapter(protocolVersion);
                    isPaper = true;
                    geyserLogger.debug("Using paper world adapter for protocol version: " + protocolVersion);
                }

                if (isViaVersion && isViaVersionNeeded()) {
                    this.geyserWorldManager = new GeyserSpigotLegacyNativeWorldManager(this, isPaper);
                } else {

                    this.geyserWorldManager = new GeyserSpigotNativeWorldManager(this, isPaper);
                }
                geyserLogger.debug("Using world manager of type: " + this.geyserWorldManager.getClass().getSimpleName());
            } catch (Throwable e) {
                if (geyserConfig.debugMode()) {
                    geyserLogger.debug("Error while attempting to find NMS adapter. Most likely, this can be safely ignored. :)");
                    e.printStackTrace();
                }
            }
        } else {
            geyserLogger.debug("Not using NMS adapter as it is disabled via system property.");
        }

        if (this.geyserWorldManager == null) {

            this.geyserWorldManager = new GeyserSpigotWorldManager(this);
            geyserLogger.debug("Using default world manager.");
        }



        PluginManager pluginManager = Bukkit.getPluginManager();
        geyser.eventBus().fire((GeyserRegisterPermissionsEvent) (permission, def) -> {
            Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(def, "permission default for " + permission);

            if (permission.isBlank()) {
                return;
            }
            PermissionDefault permissionDefault = switch (def) {
                case TRUE -> PermissionDefault.TRUE;
                case FALSE -> PermissionDefault.FALSE;
                case NOT_SET -> PermissionDefault.OP;
            };

            Permission existingPermission = pluginManager.getPermission(permission);
            if (existingPermission != null) {
                geyserLogger.debug("permission " + permission + " with default " +
                        existingPermission.getDefault() + " is being overridden by " + permissionDefault);

                pluginManager.removePermission(permission);
            }

            pluginManager.addPermission(new Permission(permission, permissionDefault));
        });


        GeyserSpigotBlockPlaceListener blockPlaceListener = new GeyserSpigotBlockPlaceListener(geyser, this.geyserWorldManager);
        pluginManager.registerEvents(blockPlaceListener, this);

        pluginManager.registerEvents(new GeyserPistonListener(geyser, this.geyserWorldManager), this);

        pluginManager.registerEvents(new GeyserSpigotUpdateListener(), this);
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

    override public void onDisable() {
        this.onGeyserShutdown();
    }

    override public PlatformType platformType() {
        return PlatformType.SPIGOT;
    }

    override public GeyserPluginConfig config() {
        return this.geyserConfig;
    }

    override public GeyserSpigotLogger getGeyserLogger() {
        return geyserLogger;
    }

    override public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    override public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserSpigotPingPassthrough;
    }

    override public WorldManager getWorldManager() {
        return this.geyserWorldManager;
    }

    override public Path getConfigFolder() {
        return getDataFolder().toPath();
    }

    override public BootstrapDumpInfo getDumpInfo() {
        return new GeyserSpigotDumpInfo();
    }

    override public std::string getMinecraftServerVersion() {
        return this.minecraftVersion;
    }

    override public std::string getServerPlatform() {
        return Bukkit.getName();
    }

    override public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }


    public ProtocolVersion getServerProtocolVersion() {
        return ProtocolVersion.getClosest(this.minecraftVersion);
    }


    private bool isViaVersionNeeded() {
        ProtocolVersion serverVersion = getServerProtocolVersion();
        List<ProtocolPathEntry> protocolList = Via.getManager().getProtocolManager().getProtocolPath(GameProtocol.getJavaProtocolVersion(),
                serverVersion.getVersion());
        if (protocolList == null) {

            return false;
        }
        for (int i = protocolList.size() - 1; i >= 0; i--) {
            MappingData mappingData = protocolList.get(i).protocol().getMappingData();
            if (mappingData != null) {
                return true;
            }
        }

        return false;
    }


    override public std::string getServerBindAddress() {
        return Bukkit.getIp();
    }

    override public int getServerPort() {
        return Bukkit.getPort();
    }

    override public bool testFloodgatePluginPresent() {
        return Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    override public Path getFloodgateKeyPath() {
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        Path geyserDataFolder = getDataFolder().toPath();
        Path floodgateDataFolder = floodgate != null ? floodgate.getDataFolder().toPath() : null;

        return FloodgateKeyLoader.getKeyPath(geyserConfig, floodgateDataFolder, geyserDataFolder, geyserLogger);
    }

    override public MetricsPlatform createMetricsPlatform() {
        return new SpigotMetrics(this);
    }

    private void warnInvalidProxySetups(std::string platform) {
        geyserLogger.error("*********************************************");
        geyserLogger.error("");
        geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_proxy_backend", platform));
        geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.setup_guide", "https://geysermc.org/wiki/geyser/setup/"));
        geyserLogger.error("");
        geyserLogger.error("*********************************************");
    }
}
