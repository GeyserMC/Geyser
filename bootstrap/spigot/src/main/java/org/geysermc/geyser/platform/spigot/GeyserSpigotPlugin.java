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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import me.lucko.commodore.CommodoreProvider;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.adapters.paper.PaperAdapters;
import org.geysermc.geyser.adapters.spigot.SpigotAdapters;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.spigot.command.GeyserBrigadierSupport;
import org.geysermc.geyser.platform.spigot.command.GeyserSpigotCommandExecutor;
import org.geysermc.geyser.platform.spigot.command.GeyserSpigotCommandManager;
import org.geysermc.geyser.platform.spigot.world.GeyserPistonListener;
import org.geysermc.geyser.platform.spigot.world.GeyserSpigotBlockPlaceListener;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotLegacyNativeWorldManager;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotNativeWorldManager;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotWorldManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GeyserSpigotPlugin extends JavaPlugin implements GeyserBootstrap {

    private GeyserSpigotCommandManager geyserCommandManager;
    private GeyserSpigotConfiguration geyserConfig;
    private GeyserSpigotInjector geyserInjector;
    private final GeyserSpigotLogger geyserLogger = GeyserPaperLogger.supported() ?
            new GeyserPaperLogger(this, getLogger()) : new GeyserSpigotLogger(getLogger());
    private IGeyserPingPassthrough geyserSpigotPingPassthrough;
    private GeyserSpigotWorldManager geyserWorldManager;

    private GeyserImpl geyser;

    /**
     * The Minecraft server version, formatted as <code>1.#.#</code>
     */
    private String minecraftVersion;

    @Override
    public void onLoad() {
        onGeyserInitialize();
    }

    @Override
    public void onGeyserInitialize() {
        GeyserLocale.init(this);

        try {
            // AvailableCommandsSerializer_v291 complains otherwise - affects at least 1.8
            ByteBuf.class.getMethod("writeShortLE", int.class);
            // Only available in 1.13.x
            Class.forName("org.bukkit.event.server.ServerLoadEvent");
            // We depend on this as a fallback in certain scenarios
            BlockData.class.getMethod("getAsString");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            geyserLogger.error("*********************************************");
            geyserLogger.error("");
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.header"));
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.message", "1.13.2"));
            geyserLogger.error("");
            geyserLogger.error("*********************************************");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            Class.forName("net.md_5.bungee.chat.ComponentSerializer");
        } catch (ClassNotFoundException e) {
            if (!PaperAdventure.canSendMessageUsingComponent()) { // Prepare for Paper eventually removing Bungee chat
                geyserLogger.error("*********************************************");
                geyserLogger.error("");
                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.header", getServer().getName()));
                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.message", "Paper"));
                geyserLogger.error("");
                geyserLogger.error("*********************************************");
                Bukkit.getPluginManager().disablePlugin(this);
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
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!loadConfig()) {
            return;
        }
        this.geyserLogger.setDebug(geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        // Turn "(MC: 1.16.4)" into 1.16.4.
        this.minecraftVersion = Bukkit.getServer().getVersion().split("\\(MC: ")[1].split("\\)")[0];

        this.geyser = GeyserImpl.load(PlatformType.SPIGOT, this);
    }

    @Override
    public void onEnable() {
        this.geyserCommandManager = new GeyserSpigotCommandManager(geyser);
        this.geyserCommandManager.init();

        // Because Bukkit locks its command map upon startup, we need to
        // add our plugin commands in onEnable, but populating the executor
        // can happen at any time (later in #onGeyserEnable())
        CommandMap commandMap = GeyserSpigotCommandManager.getCommandMap();
        for (Extension extension : this.geyserCommandManager.extensionCommands().keySet()) {
            // Thanks again, Bukkit
            try {
                Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);

                PluginCommand pluginCommand = constructor.newInstance(extension.description().id(), this);
                pluginCommand.setDescription("The main command for the " + extension.name() + " Geyser extension!");

                commandMap.register(extension.description().id(), "geyserext", pluginCommand);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                this.geyserLogger.error("Failed to construct PluginCommand for extension " + extension.name(), ex);
            }
        }

        // Needs to be an anonymous inner class otherwise Bukkit complains about missing classes
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
        // Configs are loaded once early - so we can create the logger, then load extensions and finally register
        // extension commands in #onEnable. To ensure reloading geyser also reloads the geyser config, this exists
        if (GeyserImpl.getInstance().isReloading()) {
            if (!loadConfig()) {
                return;
            }
            this.geyserLogger.setDebug(this.geyserConfig.isDebugMode());
            GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);
        }

        GeyserImpl.start();

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserSpigotPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            if (ReflectedNames.checkPaperPingEvent()) {
                this.geyserSpigotPingPassthrough = new GeyserPaperPingPassthrough(geyserLogger);
            } else if (ReflectedNames.newSpigotPingConstructorExists()) {
                this.geyserSpigotPingPassthrough = new GeyserSpigotPingPassthrough(geyserLogger);
            } else {
                // Can't enable one of the other options
                this.geyserSpigotPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
            }
        }
        geyserLogger.debug("Spigot ping passthrough type: " + (this.geyserSpigotPingPassthrough == null ? null : this.geyserSpigotPingPassthrough.getClass()));

        // Don't need to re-create the world manager/re-register commands/reinject when reloading
        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        boolean isViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;

        // Check to ensure the current setup can support the protocol version Geyser uses
        GeyserSpigotVersionChecker.checkForSupportedProtocol(geyserLogger, isViaVersion);

        // We want to do this late in the server startup process to allow plugins such as ViaVersion and ProtocolLib
        // To do their job injecting, then connect into *that*
        this.geyserInjector = new GeyserSpigotInjector(isViaVersion);
        this.geyserInjector.initializeLocalChannel(this);

        if (Boolean.parseBoolean(System.getProperty("Geyser.UseDirectAdapters", "true"))) {
            try {
                boolean isPaper = false;
                try {
                    String name = Bukkit.getServer().getClass().getPackage().getName();
                    String nmsVersion = name.substring(name.lastIndexOf('.') + 1);
                    SpigotAdapters.registerWorldAdapter(nmsVersion);
                    geyserLogger.debug("Using spigot NMS adapter for nms version: " + nmsVersion);
                } catch (Exception e) { // Likely running on Paper 1.20.5+
                    geyserLogger.debug("Unable to find spigot world manager: " + e.getMessage());
                    //noinspection deprecation
                    int protocolVersion = Bukkit.getUnsafe().getProtocolVersion();
                    PaperAdapters.registerClosestWorldAdapter(protocolVersion);
                    isPaper = true;
                    geyserLogger.debug("Using paper world adapter for protocol version: " + protocolVersion);
                }

                if (isViaVersion && isViaVersionNeeded()) {
                    this.geyserWorldManager = new GeyserSpigotLegacyNativeWorldManager(this, isPaper);
                } else {
                    // No ViaVersion
                    this.geyserWorldManager = new GeyserSpigotNativeWorldManager(this, isPaper);
                }
                geyserLogger.debug("Using world manager of type: " + this.geyserWorldManager.getClass().getSimpleName());
            } catch (Throwable e) {
                if (geyserConfig.isDebugMode()) {
                    geyserLogger.debug("Error while attempting to find NMS adapter. Most likely, this can be safely ignored. :)");
                    e.printStackTrace();
                }
            }
        } else {
            geyserLogger.debug("Not using NMS adapter as it is disabled via system property.");
        }

        if (this.geyserWorldManager == null) {
            // No NMS adapter
            this.geyserWorldManager = new GeyserSpigotWorldManager(this);
            geyserLogger.debug("Using default world manager.");
        }

        PluginCommand geyserCommand = this.getCommand("geyser");
        Objects.requireNonNull(geyserCommand, "base command cannot be null");
        geyserCommand.setExecutor(new GeyserSpigotCommandExecutor(geyser, geyserCommandManager.getCommands()));

        for (Map.Entry<Extension, Map<String, Command>> entry : this.geyserCommandManager.extensionCommands().entrySet()) {
            Map<String, Command> commands = entry.getValue();
            if (commands.isEmpty()) {
                continue;
            }

            PluginCommand command = this.getCommand(entry.getKey().description().id());
            if (command == null) {
                continue;
            }

            command.setExecutor(new GeyserSpigotCommandExecutor(this.geyser, commands));
        }

        // Register permissions so they appear in, for example, LuckPerms' UI
        // Re-registering permissions throws an error
        for (Map.Entry<String, Command> entry : geyserCommandManager.commands().entrySet()) {
            Command command = entry.getValue();
            if (command.aliases().contains(entry.getKey())) {
                // Don't register aliases
                continue;
            }

            Bukkit.getPluginManager().addPermission(new Permission(command.permission(),
                    GeyserLocale.getLocaleStringLog(command.description()),
                    command.isSuggestedOpOnly() ? PermissionDefault.OP : PermissionDefault.TRUE));
        }

        // Register permissions for extension commands
        for (Map.Entry<Extension, Map<String, Command>> commandEntry : this.geyserCommandManager.extensionCommands().entrySet()) {
            for (Map.Entry<String, Command> entry : commandEntry.getValue().entrySet()) {
                Command command = entry.getValue();
                if (command.aliases().contains(entry.getKey())) {
                    // Don't register aliases
                    continue;
                }

                if (command.permission().isBlank()) {
                    continue;
                }

                // Avoid registering the same permission twice, e.g. for the extension help commands
                if (Bukkit.getPluginManager().getPermission(command.permission()) != null) {
                    GeyserImpl.getInstance().getLogger().debug("Skipping permission " + command.permission() + " as it is already registered");
                    continue;
                }

                Bukkit.getPluginManager().addPermission(new Permission(command.permission(),
                        GeyserLocale.getLocaleStringLog(command.description()),
                        command.isSuggestedOpOnly() ? PermissionDefault.OP : PermissionDefault.TRUE));
            }
        }

        Bukkit.getPluginManager().addPermission(new Permission(Constants.UPDATE_PERMISSION,
                "Whether update notifications can be seen", PermissionDefault.OP));

        // Events cannot be unregistered - re-registering results in duplicate firings
        GeyserSpigotBlockPlaceListener blockPlaceListener = new GeyserSpigotBlockPlaceListener(geyser, this.geyserWorldManager);
        Bukkit.getServer().getPluginManager().registerEvents(blockPlaceListener, this);

        Bukkit.getServer().getPluginManager().registerEvents(new GeyserPistonListener(geyser, this.geyserWorldManager), this);

        Bukkit.getServer().getPluginManager().registerEvents(new GeyserSpigotUpdateListener(), this);

        boolean brigadierSupported = CommodoreProvider.isSupported();
        geyserLogger.debug("Brigadier supported? " + brigadierSupported);
        if (brigadierSupported) {
            GeyserBrigadierSupport.loadBrigadier(this, geyserCommand);
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
    public void onDisable() {
        this.onGeyserShutdown();
    }

    @Override
    public GeyserSpigotConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserSpigotLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public GeyserCommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserSpigotPingPassthrough;
    }

    @Override
    public WorldManager getWorldManager() {
        return this.geyserWorldManager;
    }

    @Override
    public Path getConfigFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserSpigotDumpInfo();
    }

    @Override
    public String getMinecraftServerVersion() {
        return this.minecraftVersion;
    }

    @Override
    public SocketAddress getSocketAddress() {
        return this.geyserInjector.getServerSocketAddress();
    }

    /**
     * @return the server version before ViaVersion finishes initializing
     */
    public ProtocolVersion getServerProtocolVersion() {
        return ProtocolVersion.getClosest(this.minecraftVersion);
    }

    /**
     * This function should not run unless ViaVersion is installed on the server.
     *
     * @return true if there is any block mappings difference between the server and client.
     */
    private boolean isViaVersionNeeded() {
        ProtocolVersion serverVersion = getServerProtocolVersion();
        List<ProtocolPathEntry> protocolList = Via.getManager().getProtocolManager().getProtocolPath(GameProtocol.getJavaProtocolVersion(),
                serverVersion.getVersion());
        if (protocolList == null) {
            // No translation needed!
            return false;
        }
        for (int i = protocolList.size() - 1; i >= 0; i--) {
            MappingData mappingData = protocolList.get(i).protocol().getMappingData();
            if (mappingData != null) {
                return true;
            }
        }
        // All mapping data is null, which means client and server block states are the same
        return false;
    }

    @NonNull
    @Override
    public String getServerBindAddress() {
        return Bukkit.getIp();
    }

    @Override
    public int getServerPort() {
        return Bukkit.getPort();
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            geyserConfig.loadFloodgate(this);
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean loadConfig() {
        // This is manually done instead of using Bukkit methods to save the config because otherwise comments get removed
        try {
            if (!getDataFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                getDataFolder().mkdir();
            }
            File configFile = FileUtils.fileOrCopiedFromResource(new File(getDataFolder(), "config.yml"), "config.yml",
                    (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()), this);
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserSpigotConfiguration.class);
        } catch (IOException ex) {
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        return true;
    }
}
