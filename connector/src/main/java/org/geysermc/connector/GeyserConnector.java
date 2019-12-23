/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector;

import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.v389.Bedrock_v389;

import lombok.Getter;
import org.fusesource.jansi.AnsiConsole;
import org.geysermc.api.Connector;
import org.geysermc.api.Geyser;
import org.geysermc.api.Player;
import org.geysermc.api.command.CommandMap;
import org.geysermc.api.logger.Logger;
import org.geysermc.api.plugin.Plugin;
import org.geysermc.connector.command.GeyserCommandMap;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.console.ConsoleCommandReader;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.metrics.Metrics;
import org.geysermc.connector.network.ConnectorServerEventHandler;
import org.geysermc.connector.network.remote.RemoteJavaServer;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.plugin.GeyserPluginLoader;
import org.geysermc.connector.plugin.GeyserPluginManager;
import org.geysermc.connector.thread.PingPassthroughThread;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.Toolbox;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class GeyserConnector implements Connector {

    public static final BedrockPacketCodec BEDROCK_PACKET_CODEC = Bedrock_v389.V389_CODEC;

    public static final String NAME = "Geyser";
    public static final String VERSION = "1.0-SNAPSHOT";

    private final Map<Object, GeyserSession> players = new HashMap<>();

    private static GeyserConnector instance;

    private RemoteJavaServer remoteServer;

    private Logger logger;

    private CommandMap commandMap;

    private GeyserConfiguration config;
    private GeyserPluginManager pluginManager;

    private boolean shuttingDown = false;

    private final ScheduledExecutorService generalThreadPool;
    private PingPassthroughThread passthroughThread;

    private Metrics metrics;

    public static void main(String[] args) {
        instance = new GeyserConnector();
    }

    private GeyserConnector() {
        long startupTime = System.currentTimeMillis();

        // Metric
        if (!(System.console() == null) && System.getProperty("os.name", "Windows 10").toLowerCase().contains("windows")) {
            AnsiConsole.systemInstall();
        }

        instance = this;

        this.logger = GeyserLogger.DEFAULT;

        logger.info("******************************************");
        logger.info("");
        logger.info("Loading " + NAME + " version " + VERSION);
        logger.info("");
        logger.info("******************************************");

        try {
            File configFile = FileUtils.fileOrCopiedFromResource("config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            config = FileUtils.loadConfig(configFile, GeyserConfiguration.class);
        } catch (IOException ex) {
            logger.severe("Failed to read/create config.yml! Make sure it's up to date and/or readable+writable!", ex);
            shutdown();
        }

        this.generalThreadPool = Executors.newScheduledThreadPool(config.getGeneralThreadPool());
        ConsoleCommandReader consoleReader = new ConsoleCommandReader(this);
        consoleReader.startConsole();

        logger.setDebug(config.isDebugMode());

        Toolbox.init();
        TranslatorsInit.start();

        commandMap = new GeyserCommandMap(this);
        remoteServer = new RemoteJavaServer(config.getRemote().getAddress(), config.getRemote().getPort());

        Geyser.setConnector(this);

        pluginManager = new GeyserPluginManager(new GeyserPluginLoader(this));
        pluginManager.getLoader().loadPlugins();

        passthroughThread = new PingPassthroughThread(this);
        if (config.isPingPassthrough())
            generalThreadPool.scheduleAtFixedRate(passthroughThread, 1, 1, TimeUnit.SECONDS);

        BedrockServer bedrockServer = new BedrockServer(new InetSocketAddress(config.getBedrock().getAddress(), config.getBedrock().getPort()));
        bedrockServer.setHandler(new ConnectorServerEventHandler(this));
        bedrockServer.bind().whenComplete((avoid, throwable) -> {
            if (throwable == null) {
                logger.info("Started Geyser on " + config.getBedrock().getAddress() + ":" + config.getBedrock().getPort());
            } else {
                logger.severe("Failed to start Geyser on " + config.getBedrock().getAddress() + ":" + config.getBedrock().getPort());
                throwable.printStackTrace();
            }
        }).join();

        if (config.getMetrics().isEnabled()) {
            metrics = new Metrics("GeyserMC", config.getMetrics().getUUID(), false, java.util.logging.Logger.getLogger(""));
            metrics.addCustomChart(new Metrics.SingleLineChart("servers", () -> 1));
            metrics.addCustomChart(new Metrics.SingleLineChart("players", Geyser::getPlayerCount));
            metrics.addCustomChart(new Metrics.SimplePie("authMode", config.getRemote()::getAuthType));
        }

        double completeTime = (System.currentTimeMillis() - startupTime) / 1000D;
        logger.info(String.format("Done (%ss)! Run /help for help!", new DecimalFormat("#.###").format(completeTime)));
    }

    @Override
    public Collection<? extends Player> getConnectedPlayers() {
        return players.values();
    }

    public void shutdown() {
        logger.info("Shutting down connector.");
        for (Plugin plugin : pluginManager.getPlugins()) {
            pluginManager.disablePlugin(plugin);
            pluginManager.unloadPlugin(plugin);
        }

        shuttingDown = true;

        generalThreadPool.shutdown();
        System.exit(0);
    }

    public void addPlayer(GeyserSession player) {
        players.put(player.getAuthenticationData().getName(), player);
        players.put(player.getAuthenticationData().getUUID(), player);
        players.put(player.getSocketAddress(), player);
    }

    public void removePlayer(GeyserSession player) {
        players.remove(player.getAuthenticationData().getName());
        players.remove(player.getAuthenticationData().getUUID());
        players.remove(player.getSocketAddress());
    }
}
