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

package org.geysermc.platform.standalone;

import org.fusesource.jansi.AnsiConsole;
import org.geysermc.common.PlatformType;
import org.geysermc.common.bootstrap.IGeyserBootstrap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.platform.standalone.console.ConsoleCommandReader;
import org.geysermc.platform.standalone.console.GeyserLogger;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class GeyserBootstrap implements IGeyserBootstrap {

    private GeyserConfiguration geyserConfig;
    private GeyserLogger geyserLogger;

    public static void main(String[] args) {
        new GeyserBootstrap().onEnable();
    }

    @Override
    public void onEnable() {
        // Metric
        if (!(System.console() == null) && System.getProperty("os.name", "Windows 10").toLowerCase().contains("windows")) {
            AnsiConsole.systemInstall();
        }

        geyserLogger = new GeyserLogger();

        try {
            File configFile = FileUtils.fileOrCopiedFromResource("config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            geyserConfig = FileUtils.loadConfig(configFile, GeyserConfiguration.class);
        } catch (IOException ex) {
            geyserLogger.severe("Failed to read/create config.yml! Make sure it's up to date and/or readable+writable!", ex);
            System.exit(0);
        }

        GeyserConnector connector = GeyserConnector.start(PlatformType.STANDALONE, this);

        ConsoleCommandReader consoleReader = new ConsoleCommandReader(connector);
        consoleReader.startConsole();
    }

    @Override
    public void onDisable() {
        GeyserConnector.stop();
        System.exit(0);
    }

    @Override
    public GeyserConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserLogger getGeyserLogger() {
        return geyserLogger;
    }
}
