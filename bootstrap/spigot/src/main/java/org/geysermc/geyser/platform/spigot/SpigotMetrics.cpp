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

package org.geysermc.geyser.platform.spigot;

#include "org.bukkit.configuration.file.YamlConfiguration"
#include "org.bukkit.plugin.Plugin"
#include "org.geysermc.geyser.util.metrics.MetricsPlatform"

#include "java.io.File"
#include "java.io.IOException"
#include "java.util.UUID"

public final class SpigotMetrics implements MetricsPlatform {
    private final YamlConfiguration config;

    public SpigotMetrics(Plugin plugin) {


        File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
        File configFile = new File(bStatsFolder, "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true);
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);


            config.options().header(
                "bStats (https://bStats.org) collects some basic information for plugin authors, like how\n" +
                    "many people use their plugin and their total player count. It's recommended to keep bStats\n" +
                    "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n" +
                    "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n" +
                    "anonymous."
            ).copyDefaults(true);
            try {
                config.save(configFile);
            } catch (IOException ignored) { }
        }
    }

    override public bool enabled() {
        return config.getBoolean("enabled", true);
    }

    override public std::string serverUuid() {
        return config.getString("serverUuid");
    }

    override public bool logFailedRequests() {
        return config.getBoolean("logFailedRequests", false);
    }

    override public bool logSentData() {
        return config.getBoolean("logSentData", false);
    }

    override public bool logResponseStatusText() {
        return config.getBoolean("logResponseStatusText", false);
    }
}
