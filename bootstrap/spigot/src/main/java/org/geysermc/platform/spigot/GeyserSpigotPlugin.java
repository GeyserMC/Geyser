/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.spigot;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.connector.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.ping.GeyserLegacyPingPassthrough;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.spigot.command.GeyserSpigotCommandExecutor;
import org.geysermc.platform.spigot.command.GeyserSpigotCommandManager;
import org.geysermc.platform.spigot.world.GeyserSpigotBlockPlaceListener;
import org.geysermc.platform.spigot.world.GeyserSpigotWorldManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

public class GeyserSpigotPlugin extends JavaPlugin implements GeyserBootstrap {

    private GeyserSpigotCommandManager geyserCommandManager;
    private GeyserSpigotConfiguration geyserConfig;
    private GeyserSpigotLogger geyserLogger;
    private IGeyserPingPassthrough geyserSpigotPingPassthrough;
    private GeyserSpigotBlockPlaceListener blockPlaceListener;
    private GeyserSpigotWorldManager geyserWorldManager;

    private GeyserConnector connector;

    @Override
    public void onEnable() {
        // This is manually done instead of using Bukkit methods to save the config because otherwise comments get removed
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
                File bukkitConfig = new File("plugins/Geyser-Bukkit/config.yml");
                if (bukkitConfig.exists()) { // Copy over old configs
                    getLogger().log(Level.INFO, LanguageUtils.getLocaleStringLog("geyser.bootstrap.config.copy_bukkit_config"));
                    Files.copy(bukkitConfig.toPath(), new File(getDataFolder().toString() + "/config.yml").toPath());
                    getLogger().log(Level.INFO, LanguageUtils.getLocaleStringLog("geyser.bootstrap.config.copied_bukkit_config"));
                }
            }
            File configFile = FileUtils.fileOrCopiedFromResource(new File(getDataFolder(), "config.yml"), "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserSpigotConfiguration.class);
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, LanguageUtils.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
        }

        // Don't change the ip if its listening on all interfaces
        // By default this should be 127.0.0.1 but may need to be changed in some circumstances
        if (!Bukkit.getIp().equals("0.0.0.0") && !Bukkit.getIp().equals("")) {
            geyserConfig.getRemote().setAddress(Bukkit.getIp());
        }

        geyserConfig.getRemote().setPort(Bukkit.getPort());

        this.geyserLogger = new GeyserSpigotLogger(getLogger(), geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        if (geyserConfig.getRemote().getAuthType().equals("floodgate") && Bukkit.getPluginManager().getPlugin("floodgate-bukkit") == null) {
            geyserLogger.severe(LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        geyserConfig.loadFloodgate(this);

        this.connector = GeyserConnector.start(PlatformType.SPIGOT, this);

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserSpigotPingPassthrough = GeyserLegacyPingPassthrough.init(connector);
        } else {
            this.geyserSpigotPingPassthrough = new GeyserSpigotPingPassthrough(geyserLogger);
        }

        this.geyserCommandManager = new GeyserSpigotCommandManager(this, connector);

        boolean isViaVersion = (Bukkit.getPluginManager().getPlugin("ViaVersion") != null);
        // Used to determine if Block.getBlockData() is present.
        boolean isLegacy = !isCompatible(Bukkit.getServer().getVersion(), "1.13.0");
        if (isLegacy)
            geyserLogger.debug("Legacy version of Minecraft (1.12.2 or older) detected.");

        this.geyserWorldManager = new GeyserSpigotWorldManager(isLegacy, isViaVersion);
        this.blockPlaceListener = new GeyserSpigotBlockPlaceListener(connector, isLegacy, isViaVersion);

        Bukkit.getServer().getPluginManager().registerEvents(blockPlaceListener, this);

        this.getCommand("geyser").setExecutor(new GeyserSpigotCommandExecutor(connector));
    }

    @Override
    public void onDisable() {
        if (connector != null)
            connector.shutdown();
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
            } catch(NumberFormatException ex) {
                temp[index] = 0;
            }
        }
        return temp;
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserSpigotDumpInfo();
    }
}
