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

package org.geysermc.platform.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.ping.GeyserLegacyPingPassthrough;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.platform.bukkit.command.GeyserBukkitCommandExecutor;
import org.geysermc.platform.bukkit.command.GeyserBukkitCommandManager;
import org.geysermc.platform.bukkit.world.GeyserBukkitBlockPlaceListener;
import org.geysermc.platform.bukkit.world.GeyserBukkitWorldManager;
import us.myles.ViaVersion.api.Via;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class GeyserBukkitPlugin extends JavaPlugin implements GeyserBootstrap {

    private GeyserBukkitCommandManager geyserCommandManager;
    private GeyserBukkitConfiguration geyserConfig;
    private GeyserBukkitLogger geyserLogger;
    private IGeyserPingPassthrough geyserBukkitPingPassthrough;
    private GeyserBukkitBlockPlaceListener blockPlaceListener;
    private GeyserBukkitWorldManager geyserWorldManager;

    private GeyserConnector connector;

    @Override
    public void onEnable() {
        // This is manually done instead of using Bukkit methods to save the config because otherwise comments get removed
        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File configFile = FileUtils.fileOrCopiedFromResource(new File(getDataFolder(), "config.yml"), "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserBukkitConfiguration.class);
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Failed to read/create config.yml! Make sure it's up to date and/or readable+writable!", ex);
            ex.printStackTrace();
        }

        // Don't change the ip if its listening on all interfaces
        // By default this should be 127.0.0.1 but may need to be changed in some circumstances
        if (!Bukkit.getIp().equals("0.0.0.0") && !Bukkit.getIp().equals("")) {
            geyserConfig.getRemote().setAddress(Bukkit.getIp());
        }

        geyserConfig.getRemote().setPort(Bukkit.getPort());

        this.geyserLogger = new GeyserBukkitLogger(getLogger(), geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        if (geyserConfig.getRemote().getAuthType().equals("floodgate") && Bukkit.getPluginManager().getPlugin("floodgate-bukkit") == null) {
            geyserLogger.severe("Auth type set to Floodgate but Floodgate not found! Disabling...");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        geyserConfig.loadFloodgate(this);

        this.connector = GeyserConnector.start(PlatformType.BUKKIT, this);

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserBukkitPingPassthrough = GeyserLegacyPingPassthrough.init(connector);
        } else {
            this.geyserBukkitPingPassthrough = new GeyserBukkitPingPassthrough(geyserLogger);
        }

        this.geyserCommandManager = new GeyserBukkitCommandManager(this, connector);

        boolean isViaVersion = false;
        // Used to determine if Block.getBlockData() is present.
        boolean isLegacy = !isCompatible(Bukkit.getServer().getVersion(), "1.13.0");
        if (isLegacy)
            geyserLogger.debug("Legacy version of Minecraft (1.12.2 or older) detected.");

        if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
            // TODO: Update when ViaVersion updates
            // API changes between 2.2.3 and 3.0.0-SNAPSHOT require this check
            if (!Via.getAPI().getVersion().equals("3.0.0-SNAPSHOT") && isLegacy) {
                geyserLogger.info("ViaVersion detected but not ViaVersion-ABSTRACTION. Please update your ViaVersion plugin for compatibility with Geyser.");
            } else {
                isViaVersion = true;
            }
        }

        this.geyserWorldManager = new GeyserBukkitWorldManager(isLegacy, isViaVersion);
        this.blockPlaceListener = new GeyserBukkitBlockPlaceListener(connector, isLegacy, isViaVersion);
        Bukkit.getServer().getPluginManager().registerEvents(blockPlaceListener, this);

        this.getCommand("geyser").setExecutor(new GeyserBukkitCommandExecutor(connector));
    }

    @Override
    public void onDisable() {
        if (connector != null)
            connector.shutdown();
    }

    @Override
    public GeyserBukkitConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserBukkitLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserBukkitPingPassthrough;
    }

    @Override
    public WorldManager getWorldManager() {
        return this.geyserWorldManager;
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
}
