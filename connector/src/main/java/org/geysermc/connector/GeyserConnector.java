/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view the LICENCE file for details.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.v354.Bedrock_v354;
import lombok.Getter;
import org.geysermc.api.ChatColor;
import org.geysermc.connector.command.GeyserCommandMap;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.console.ConsoleCommandReader;
import org.geysermc.connector.console.GeyserLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GeyserConnector {

    public static final BedrockPacketCodec BEDROCK_PACKET_CODEC = Bedrock_v354.V354_CODEC;

    private static final String NAME = "Geyser";
    private static final String VERSION = "1.0-SNAPSHOT";

    private static GeyserConnector instance;

    @Getter
    private GeyserLogger logger;

    @Getter
    private GeyserCommandMap commandMap;

    @Getter
    private GeyserConfiguration config;

    @Getter
    private boolean shuttingDown = false;

    @Getter
    private final ScheduledExecutorService generalThreadPool;

    public static void main(String[] args) {
        instance = new GeyserConnector();
    }

    private GeyserConnector() {
        instance = this;

        this.generalThreadPool = Executors.newScheduledThreadPool(32); //TODO: Make configurable value
        this.logger = new GeyserLogger(this);

        ConsoleCommandReader consoleReader = new ConsoleCommandReader(this);
        consoleReader.startConsole();

        logger.info(ChatColor.AQUA + "******************************************");
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
        commandMap = new GeyserCommandMap(this);
    }

    public void shutdown() {
        logger.info("Shutting down connector.");
        shuttingDown = true;

        generalThreadPool.shutdown();
        System.exit(0);
    }
}
