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
import java.util.logging.Level;

public class GeyserSpigotPlugin extends JavaPlugin implements GeyserBootstrap {
    /**
     * Determines if the plugin has been ran once before, including before /geyser reload.
     */
    private static boolean INITIALIZED = false;

    private GeyserSpigotCommandManager geyserCommandManager;
    private GeyserSpigotConfiguration geyserConfig;
    private GeyserSpigotInjector geyserInjector;
    private GeyserSpigotLogger geyserLogger;
    private IGeyserPingPassthrough geyserSpigotPingPassthrough;
    private GeyserSpigotWorldManager geyserWorldManager;

    private GeyserImpl geyser;

    /**
     * The Minecraft server version, formatted as <code>1.#.#</code>
     */
    private String minecraftVersion;

    @Override
    public void onLoad() {
        GeyserLocale.init(this);

        try {
            // AvailableCommandsSerializer_v291 complains otherwise - affects at least 1.8
            ByteBuf.class.getMethod("writeShortLE", int.class);
            // Only available in 1.13.x
            Class.forName("org.bukkit.event.server.ServerLoadEvent");
            // We depend on this as a fallback in certain scenarios
            BlockData.class.getMethod("getAsString");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            getLogger().severe("*********************************************");
            getLogger().severe("");
            getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.header"));
            getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.message", "1.13.2"));
            getLogger().severe("");
            getLogger().severe("*********************************************");
            return;
        }

        try {
            Class.forName("net.md_5.bungee.chat.ComponentSerializer");
        } catch (ClassNotFoundException e) {
            if (!PaperAdventure.canSendMessageUsingComponent()) { // Prepare for Paper eventually removing Bungee chat
                getLogger().severe("*********************************************");
                getLogger().severe("");
                getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.header", getServer().getName()));
                getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.message", "Paper"));
                getLogger().severe("");
                getLogger().severe("*********************************************");
                return;
            }
        }

        try {
            Class.forName("io.netty.util.internal.ObjectPool$ObjectCreator");
        } catch (ClassNotFoundException e) {
            getLogger().severe("*********************************************");
            getLogger().severe("");
            getLogger().severe("This version of Spigot is using an outdated version of netty. Please use Paper instead!");
            getLogger().severe("");
            getLogger().severe("*********************************************");
            return;
        }

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
            getLogger().log(Level.SEVERE, GeyserLocale.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.geyserLogger = GeyserPaperLogger.supported() ? new GeyserPaperLogger(this, getLogger(), geyserConfig.isDebugMode())
                : new GeyserSpigotLogger(getLogger(), geyserConfig.isDebugMode());

        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        this.geyser = GeyserImpl.load(PlatformType.SPIGOT, this);
    }

    @Override
    public void onEnable() {
        if (this.geyserConfig == null) {
            // We failed to initialize correctly
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.geyserCommandManager = new GeyserSpigotCommandManager(geyser);
        this.geyserCommandManager.init();

        if (!INITIALIZED) {
            // Needs to be an anonymous inner class otherwise Bukkit complains about missing classes
            Bukkit.getPluginManager().registerEvents(new Listener() {

                @EventHandler
                public void onServerLoaded(ServerLoadEvent event) {
                    // Wait until all plugins have loaded so Geyser can start
                    postStartup();
                }
            }, this);

            // Because Bukkit locks its command map upon startup, we need to
            // add our plugin commands in onEnable, but populating the executor
            // can happen at any time
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
        }

        if (INITIALIZED) {
            // Reload; continue with post startup
            postStartup();
        }
    }

    private void postStartup() {
        GeyserImpl.start();

        // Turn "(MC: 1.16.4)" into 1.16.4.
        this.minecraftVersion = Bukkit.getServer().getVersion().split("\\(MC: ")[1].split("\\)")[0];

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

        boolean isViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
        if (isViaVersion) {
            try {
                // Ensure that we have the latest 4.0.0 changes and not an older ViaVersion version
                Class.forName("com.viaversion.viaversion.api.ViaManager");
            } catch (ClassNotFoundException e) {
                GeyserSpigotVersionChecker.sendOutdatedViaVersionMessage(geyserLogger);
                isViaVersion = false;
                if (this.geyserConfig.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }

        // We want to do this late in the server startup process to allow plugins such as ViaVersion and ProtocolLib
        // To do their job injecting, then connect into *that*
        this.geyserInjector = new GeyserSpigotInjector(isViaVersion);
        this.geyserInjector.initializeLocalChannel(this);

        if (Boolean.parseBoolean(System.getProperty("Geyser.UseDirectAdapters", "true"))) {
            try {
                String name = Bukkit.getServer().getClass().getPackage().getName();
                String nmsVersion = name.substring(name.lastIndexOf('.') + 1);
                SpigotAdapters.registerWorldAdapter(nmsVersion);
                if (isViaVersion && isViaVersionNeeded()) {
                    this.geyserWorldManager = new GeyserSpigotLegacyNativeWorldManager(this);
                } else {
                    // No ViaVersion
                    this.geyserWorldManager = new GeyserSpigotNativeWorldManager(this);
                }
                geyserLogger.debug("Using NMS adapter: " + this.geyserWorldManager.getClass() + ", " + nmsVersion);
            } catch (Exception e) {
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

        if (!INITIALIZED) {
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
        }

        boolean brigadierSupported = CommodoreProvider.isSupported();
        geyserLogger.debug("Brigadier supported? " + brigadierSupported);
        if (brigadierSupported) {
            GeyserBrigadierSupport.loadBrigadier(this, geyserCommand);
        }

        // Check to ensure the current setup can support the protocol version Geyser uses
        GeyserSpigotVersionChecker.checkForSupportedProtocol(geyserLogger, isViaVersion);

        INITIALIZED = true;
    }

    @Override
    public void onDisable() {
        if (geyser != null) {
            geyser.shutdown();
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
        }
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
            MappingData mappingData = protocolList.get(i).getProtocol().getMappingData();
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
}
