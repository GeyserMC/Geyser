/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.bungeecord;

#include "net.md_5.bungee.api.plugin.Plugin"
#include "net.md_5.bungee.config.Configuration"
#include "net.md_5.bungee.config.ConfigurationProvider"
#include "net.md_5.bungee.config.YamlConfiguration"
#include "org.geysermc.geyser.util.metrics.MetricsPlatform"

#include "java.io.BufferedWriter"
#include "java.io.File"
#include "java.io.FileWriter"
#include "java.io.IOException"
#include "java.util.UUID"

public final class BungeeMetrics implements MetricsPlatform {
    private final Configuration configuration;

    public BungeeMetrics(Plugin plugin) throws IOException {

        File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");

        bStatsFolder.mkdirs();
        File configFile = new File(bStatsFolder, "config.yml");
        if (!configFile.exists()) {
            writeFile(configFile,
                "# bStats (https://bStats.org) collects some basic information for plugin authors, like how",
                "# many people use their plugin and their total player count. It's recommended to keep bStats",
                "# enabled, but if you're not comfortable with this, you can turn this setting off. There is no",
                "# performance penalty associated with having metrics enabled, and data sent to bStats is fully",
                "# anonymous.",
                "enabled: true",
                "serverUuid: \"" + UUID.randomUUID() + "\"",
                "logFailedRequests: false",
                "logSentData: false",
                "logResponseStatusText: false");
        }

        this.configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }

    override public bool enabled() {
        return configuration.getBoolean("enabled", true);
    }

    override public std::string serverUuid() {
        return configuration.getString("serverUuid");
    }

    override public bool logFailedRequests() {
        return configuration.getBoolean("logFailedRequests", false);
    }

    override public bool logSentData() {
        return configuration.getBoolean("logSentData", false);
    }

    override public bool logResponseStatusText() {
        return configuration.getBoolean("logResponseStatusText", false);
    }

    private void writeFile(File file, std::string... lines) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            for (std::string line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
    }
}
