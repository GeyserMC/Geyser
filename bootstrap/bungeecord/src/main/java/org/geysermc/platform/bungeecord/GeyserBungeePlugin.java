/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.bungeecord;

import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.ping.GeyserLegacyPingPassthrough;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.bungeecord.command.GeyserBungeeCommandExecutor;
import org.geysermc.platform.bungeecord.command.GeyserBungeeCommandManager;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

public class GeyserBungeePlugin extends Plugin implements GeyserBootstrap {

    private GeyserBungeeCommandManager geyserCommandManager;
    private GeyserBungeeConfiguration geyserConfig;
    private GeyserBungeeLogger geyserLogger;
    private IGeyserPingPassthrough geyserBungeePingPassthrough;

    private GeyserConnector connector;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File configFile = FileUtils.fileOrCopiedFromResource(new File(getDataFolder(), "config.yml"), "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, GeyserBungeeConfiguration.class);
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, LanguageUtils.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
        }

        if (getProxy().getConfig().getListeners().size() == 1) {
            ListenerInfo listener = getProxy().getConfig().getListeners().toArray(new ListenerInfo[0])[0];

            InetSocketAddress javaAddr = listener.getHost();

            // By default this should be localhost but may need to be changed in some circumstances
            if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
                this.geyserConfig.setAutoconfiguredRemote(true);
                // Don't use localhost if not listening on all interfaces
                if (!javaAddr.getHostString().equals("0.0.0.0") && !javaAddr.getHostString().equals("")) {
                    this.geyserConfig.getRemote().setAddress(javaAddr.getHostString());
                }
                this.geyserConfig.getRemote().setPort(javaAddr.getPort());
            }

            if (geyserConfig.getBedrock().isCloneRemotePort()) {
                geyserConfig.getBedrock().setPort(javaAddr.getPort());
            }
        }

        this.geyserLogger = new GeyserBungeeLogger(getLogger(), geyserConfig.isDebugMode());
        GeyserConfiguration.checkGeyserConfiguration(geyserConfig, geyserLogger);

        // Remove this in like a year
        if (getProxy().getPluginManager().getPlugin("floodgate-bungee") != null) {
            geyserLogger.severe(LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.outdated", "https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/master/"));
            return;
        }

        if (geyserConfig.getRemote().getAuthType().equals("floodgate") && getProxy().getPluginManager().getPlugin("floodgate") == null) {
            geyserLogger.severe(LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
            return;
        } else if (geyserConfig.isAutoconfiguredRemote() && getProxy().getPluginManager().getPlugin("floodgate") != null) {
            // Floodgate installed means that the user wants Floodgate authentication
            geyserLogger.debug("Auto-setting to Floodgate authentication.");
            geyserConfig.getRemote().setAuthType("floodgate");
        }

        geyserConfig.loadFloodgate(this);

        this.connector = GeyserConnector.start(PlatformType.BUNGEECORD, this);

        this.geyserCommandManager = new GeyserBungeeCommandManager(connector);

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserBungeePingPassthrough = GeyserLegacyPingPassthrough.init(connector);
        } else {
            this.geyserBungeePingPassthrough = new GeyserBungeePingPassthrough(getProxy());
        }

        this.getProxy().getPluginManager().registerCommand(this, new GeyserBungeeCommandExecutor(connector));
    }

    @Override
    public void onDisable() {
        connector.shutdown();
    }

    @Override
    public GeyserBungeeConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserBungeeLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserBungeePingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserBungeeDumpInfo(getProxy());
    }
}
