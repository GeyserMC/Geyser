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

package org.geysermc.platform.standalone.console;

import io.sentry.Sentry;

import org.geysermc.common.ChatColor;
import org.geysermc.common.logger.IGeyserLogger;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.*;

public class GeyserLogger implements IGeyserLogger {

    private boolean colored = true;
    private boolean debug = false;

    public GeyserLogger() {
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

        try {
            File logDir = new File("logs");
            logDir.mkdir();
            File logFile = new File(logDir, "latest.log");
            int maxLogFileSize = 20;//Mo
            if (logFile.exists() && (logFile.length()) > maxLogFileSize * 1024L * 1024L)
                this.warning("Your log file is larger than " + maxLogFileSize + "Mo, you should backup and clean it !");
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
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(GeyserLogger.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (System.getenv().containsKey("DP_SENTRY_CLIENT_KEY")) {
            Handler sentryHandler = new io.sentry.jul.SentryHandler();
            sentryHandler.setLevel(Level.SEVERE);
            Sentry.init(System.getenv().get("DP_SENTRY_CLIENT_KEY"));
        }
    }

    @Override
    public void severe(String message) {
        System.out.println(printConsole(ChatColor.DARK_RED + message, colored));
    }

    @Override
    public void severe(String message, Throwable error) {
        System.out.println(printConsole(ChatColor.DARK_RED + message + "\n" + error.getMessage(), colored));
    }

    @Override
    public void error(String message) {
        System.out.println(printConsole(ChatColor.RED + message, colored));
    }

    @Override
    public void error(String message, Throwable error) {
        System.out.println(printConsole(ChatColor.RED + message + "\n" + error, colored));
    }

    @Override
    public void warning(String message) {
        System.out.println(printConsole(ChatColor.YELLOW + message, colored));
    }

    @Override
    public void info(String message) {
        System.out.println(printConsole(ChatColor.WHITE + message, colored));
    }

    @Override
    public void debug(String message) {
        if (debug)
            System.out.println(printConsole(ChatColor.GRAY + message, colored));
    }

    public static String printConsole(String message, boolean colors) {
        return colors ? ChatColor.toANSI(message + ChatColor.RESET) : ChatColor.stripColors(message + ChatColor.RESET);
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
