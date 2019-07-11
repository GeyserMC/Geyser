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

public class GeyserLogger implements org.geysermc.api.logger.Logger {

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
