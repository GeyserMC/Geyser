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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.v361.Bedrock_v361;
import lombok.Getter;
import org.fusesource.jansi.AnsiConsole;
import org.geysermc.api.Connector;
import org.geysermc.api.Geyser;
import org.geysermc.api.command.CommandMap;
import org.geysermc.api.logger.Logger;
import org.geysermc.api.plugin.Plugin;
import org.geysermc.connector.command.GeyserCommandMap;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.console.ConsoleCommandReader;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.ConnectorServerEventHandler;
import org.geysermc.connector.network.remote.RemoteJavaServer;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.plugin.GeyserPluginLoader;
import org.geysermc.connector.plugin.GeyserPluginManager;
import org.geysermc.connector.utils.Toolbox;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GeyserConnector implements Connector {

    public static final BedrockPacketCodec BEDROCK_PACKET_CODEC = Bedrock_v361.V361_CODEC;

    private static final String NAME = "Geyser";
    private static final String VERSION = "1.0-SNAPSHOT";

    private static GeyserConnector instance;

    @Getter
    private RemoteJavaServer remoteServer;

    @Getter
    private Logger logger;

    @Getter
    private CommandMap commandMap;

    @Getter
    private GeyserConfiguration config;

    @Getter
    private GeyserPluginManager pluginManager;

    @Getter
    private boolean shuttingDown = false;

    @Getter
    private final ScheduledExecutorService generalThreadPool;

    public static void main(String[] args) {
        instance = new GeyserConnector();
    }

    private GeyserConnector() {
        if(!(System.console() == null) && System.getProperty("os.name", "Windows 10").toLowerCase().contains("windows")) {
            AnsiConsole.systemInstall();
        }
        instance = this;

        this.generalThreadPool = Executors.newScheduledThreadPool(32); //TODO: Make configurable value

        this.logger = GeyserLogger.DEFAULT;

        ConsoleCommandReader consoleReader = new ConsoleCommandReader(this);
        consoleReader.startConsole();

        logger.info("******************************************");
        logger.info("");
        logger.info("Loading " + NAME + " vesion " + VERSION);
        logger.info("");
        logger.info("******************************************");

        try {
            File configFile = new File("config.yml");
            if (!configFile.exists()) {
                FileOutputStream fos = new FileOutputStream(configFile);
                InputStream is = GeyserConnector.class.getResourceAsStream("/config.yml");
                int data;
                while ((data = is.read()) != -1)
                    fos.write(data);
                is.close();
                fos.close();
            }

            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            config = objectMapper.readValue(configFile, GeyserConfiguration.class);
        } catch (IOException ex) {
            logger.severe("Failed to create config.yml! Make sure it's up to date and writable!");
            shutdown();
        }

        Toolbox.CACHED_PALLETE.array();

        TranslatorsInit.start();

        commandMap = new GeyserCommandMap(this);
        remoteServer = new RemoteJavaServer(config.getRemote().getAddress(), config.getRemote().getPort());

        Geyser.setConnector(this);

        pluginManager = new GeyserPluginManager(new GeyserPluginLoader(this));
        pluginManager.getLoader().loadPlugins();

        BedrockServer bedrockServer = new BedrockServer(new InetSocketAddress(config.getBedrock().getAddress(), config.getBedrock().getPort()));
        bedrockServer.setHandler(new ConnectorServerEventHandler(this));
        bedrockServer.bind().whenComplete((avoid, throwable) -> {
            if (throwable == null) {
                logger.info("Started RakNet on " + config.getBedrock().getAddress() + ":" + config.getBedrock().getPort());
            } else {
                logger.severe("Failed to start RakNet on " + config.getBedrock().getAddress() + ":" + config.getBedrock().getPort());
                throwable.printStackTrace();
            }
        }).join();
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
}
