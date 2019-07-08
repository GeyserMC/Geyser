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

package org.geysermc.connector.console;

import org.geysermc.api.ChatColor;
import org.geysermc.connector.GeyserConnector;
import io.sentry.Sentry;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GeyserLogger {

    private Logger logger;

    private boolean colored = true;
    private boolean debug = true;

    public GeyserLogger(GeyserConnector connector) {
        this.logger = Logger.getLogger(connector.getClass().getName());

        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        System.setOut(new PrintStream(new LoggingOutputStream(this.logger, Level.INFO), true));

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tT][%2$-5s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });

        logger.addHandler(consoleHandler);

        try {
            File logDir = new File("logs");
            logDir.mkdir();
            File logFile = new File(logDir, "latest.log");
            int maxLogFileSize = 20;//Mo
            if (logFile.exists() && (logFile.length()) > maxLogFileSize * 1024L * 1024L)
                logger.warning("Your log file is larger than " + maxLogFileSize + "Mo, you should backup and clean it !");
            FileHandler fileHandler = new FileHandler(logFile.getCanonicalPath(), true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new SimpleFormatter() {
                private static final String format = "[%1$tF %1$tT][%2$-5s] %3$s %n";

                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format(format,
                            new Date(lr.getMillis()),
                            lr.getLevel().getLocalizedName(),
                            lr.getMessage()
                    );
                }
            });
            logger.addHandler(fileHandler);
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(GeyserLogger.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (System.getenv().containsKey("DP_SENTRY_CLIENT_KEY")) {
            Handler sentryHandler = new io.sentry.jul.SentryHandler();
            sentryHandler.setLevel(Level.SEVERE);
            logger.addHandler(sentryHandler);
            Sentry.init(System.getenv().get("DP_SENTRY_CLIENT_KEY"));
        }
    }

    public void info(String message) {
        logger.info(printConsole(message, colored));
    }

    public void severe(String message) {
        logger.severe(printConsole(message, colored));
    }

    public void warning(String message) {
        logger.warning(printConsole(message, colored));
    }

    public void debug(String message) {
        if (debug)
            info(message);
    }

    public void stop() {
        for (Handler handler : logger.getHandlers())
            handler.close();
    }

    public static String printConsole(String message, boolean colors) {
        return colors ? ChatColor.toANSI(message + ChatColor.RESET) : ChatColor.stripColors(message + ChatColor.RESET);
    }
}
