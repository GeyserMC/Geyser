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
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.adapters.spigot.SpigotAdapters;
import org.geysermc.geyser.command.CommandManager;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.network.MinecraftProtocol;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.spigot.command.GeyserBrigadierSupport;
import org.geysermc.geyser.platform.spigot.command.GeyserSpigotCommandExecutor;
import org.geysermc.geyser.platform.spigot.command.GeyserSpigotCommandManager;
import org.geysermc.geyser.platform.spigot.command.SpigotCommandSender;
import org.geysermc.geyser.platform.spigot.world.GeyserPistonListener;
import org.geysermc.geyser.platform.spigot.world.GeyserSpigotBlockPlaceListener;
import org.geysermc.geyser.platform.spigot.world.manager.*;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
    public void onEnable() {
        GeyserLocale.init(this);

        // This is manually done instead of using Bukkit methods to save the config because otherwise comments get removed
        try {
            if (!getDataFolder().exists()) {
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

        try {
            // AvailableCommandsSerializer_v291 complains otherwise
            ByteBuf.class.getMethod("writeShortLE", int.class);
        } catch (NoSuchMethodException e) {
            getLogger().severe("*********************************************");
            getLogger().severe("");
            getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.header"));
            getLogger().severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.message", "1.12.2"));
            getLogger().severe("");
            getLogger().severe("*********************************************");

            Bukkit.getPluginManager().disablePlugin(this);
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

                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        // By default this should be localhost but may need to be changed in some circumstances
        if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
            geyserConfig.setAutoconfiguredRemote(true);
            // Don't use localhost if not listening on all interfaces
            if (!Bukkit.getIp().equals("0.0.0.0") && !Bukkit.getIp().equals("")) {
                geyserConfig.getRemote().setAddress(Bukkit.getIp());
            }
            geyserConfig.getRemote().setPort(Bukkit.getPort());
        }

        if (geyserConfig.getBedrock().isCloneRemotePort()) {
            geyserConfig.getBedrock().setPort(Bukkit.getPort());
        }

        this.geyserLogger = GeyserPaperLogger.supported() ? new GeyserPaperLogger(this, getLogger(), geyserConfig.isDebugMode())
                : new GeyserSpigotLogger(getLogger(), geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        // Remove this in like a year
        if (Bukkit.getPluginManager().getPlugin("floodgate-bukkit") != null) {
            geyserLogger.severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.outdated", Constants.FLOODGATE_DOWNLOAD_LOCATION));
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        if (geyserConfig.getRemote().getAuthType() == AuthType.FLOODGATE && Bukkit.getPluginManager().getPlugin("floodgate") == null) {
            geyserLogger.severe(GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " " + GeyserLocale.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
            this.getPluginLoader().disablePlugin(this);
            return;
        } else if (geyserConfig.isAutoconfiguredRemote() && Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            // Floodgate installed means that the user wants Floodgate authentication
            geyserLogger.debug("Auto-setting to Floodgate authentication.");
            geyserConfig.getRemote().setAuthType(AuthType.FLOODGATE);
        }

        geyserConfig.loadFloodgate(this);

        // Turn "(MC: 1.16.4)" into 1.16.4.
        this.minecraftVersion = Bukkit.getServer().getVersion().split("\\(MC: ")[1].split("\\)")[0];

        this.geyser = GeyserImpl.start(PlatformType.SPIGOT, this);

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

        this.geyserCommandManager = new GeyserSpigotCommandManager(geyser);

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
        // Used to determine if Block.getBlockData() is present.
        boolean isLegacy = !isCompatible(Bukkit.getServer().getVersion(), "1.13.0");
        if (isLegacy)
            geyserLogger.debug("Legacy version of Minecraft (1.12.2 or older) detected; falling back to ViaVersion for block state retrieval.");

        boolean isPre1_12 = !isCompatible(Bukkit.getServer().getVersion(), "1.12.0");
        // Set if we need to use a different method for getting a player's locale
        SpigotCommandSender.setUseLegacyLocaleMethod(isPre1_12);

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
                    if (isLegacy) {
                        // Pre-1.13
                        this.geyserWorldManager = new GeyserSpigot1_12NativeWorldManager(this);
                    } else {
                        // Post-1.13
                        this.geyserWorldManager = new GeyserSpigotLegacyNativeWorldManager(this);
                    }
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
            if (isLegacy && isViaVersion) {
                // Use ViaVersion for converting pre-1.13 block states
                this.geyserWorldManager = new GeyserSpigot1_12WorldManager(this);
            } else if (isLegacy) {
                // Not sure how this happens - without ViaVersion, we don't know any block states, so just assume everything is air
                this.geyserWorldManager = new GeyserSpigotFallbackWorldManager(this);
            } else {
                // Post-1.13
                this.geyserWorldManager = new GeyserSpigotWorldManager(this);
            }
            geyserLogger.debug("Using default world manager: " + this.geyserWorldManager.getClass());
        }

        PluginCommand pluginCommand = this.getCommand("geyser");
        pluginCommand.setExecutor(new GeyserSpigotCommandExecutor(geyser));

        if (!INITIALIZED) {
            // Register permissions so they appear in, for example, LuckPerms' UI
            // Re-registering permissions throws an error
            for (Map.Entry<String, GeyserCommand> entry : geyserCommandManager.getCommands().entrySet()) {
                GeyserCommand command = entry.getValue();
                if (command.getAliases().contains(entry.getKey())) {
                    // Don't register aliases
                    continue;
                }

                Bukkit.getPluginManager().addPermission(new Permission(command.getPermission(),
                        GeyserLocale.getLocaleStringLog(command.getDescription()),
                        command.isSuggestedOpOnly() ? PermissionDefault.OP : PermissionDefault.TRUE));
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
            GeyserBrigadierSupport.loadBrigadier(this, pluginCommand);
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
    public CommandManager getGeyserCommandManager() {
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

    public boolean isCompatible(String version, String whichVersion) {
        int[] currentVersion = parseVersion(version);
        int[] otherVersion = parseVersion(whichVersion);
        int length = Math.max(currentVersion.length, otherVersion.length);
        for (int index = 0; index < length; index = index + 1) {
            int self = (index < currentVersion.length) ? currentVersion[index] : 0;
            int other = (index < otherVersion.length) ? otherVersion[index] : 0;

            if (self != other) {
                return (self - other) > 0;
            }
        }
        return true;
    }

    private int[] parseVersion(String versionParam) {
        versionParam = (versionParam == null) ? "" : versionParam;
        if (versionParam.contains("(MC: ")) {
            versionParam = versionParam.split("\\(MC: ")[1];
            versionParam = versionParam.split("\\)")[0];
        }
        String[] stringArray = versionParam.split("[_.-]");
        int[] temp = new int[stringArray.length];
        for (int index = 0; index <= (stringArray.length - 1); index = index + 1) {
            String t = stringArray[index].replaceAll("\\D", "");
            try {
                temp[index] = Integer.parseInt(t);
            } catch (NumberFormatException ex) {
                temp[index] = 0;
            }
        }
        return temp;
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
        List<ProtocolPathEntry> protocolList = Via.getManager().getProtocolManager().getProtocolPath(MinecraftProtocol.getJavaProtocolVersion(),
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
}
