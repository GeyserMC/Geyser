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

import org.geysermc.api.ChatColor;
import org.geysermc.connector.command.GeyserCommandMap;
import org.geysermc.connector.console.ConsoleCommandReader;
import org.geysermc.connector.console.GeyserLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GeyserConnector {

    private static final String NAME = "Geyser";
    private static final String VERSION = "1.0-SNAPSHOT";

    private static GeyserConnector instance;

    private boolean shuttingDown = false;
    private GeyserLogger logger;
    private GeyserCommandMap commandMap;
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

        commandMap = new GeyserCommandMap(this);
    }

    public ScheduledExecutorService getGeneralThreadPool() {
        return generalThreadPool;
    }

    public GeyserCommandMap getCommandMap() {
        return commandMap;
    }

    public GeyserLogger getLogger() {
        return logger;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public void shutdown() {
        logger.info("Shutting down connector.");
        shuttingDown = true;

        generalThreadPool.shutdown();
        System.exit(0);
    }
}
