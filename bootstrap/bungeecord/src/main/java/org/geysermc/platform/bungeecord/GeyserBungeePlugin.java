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

package org.geysermc.platform.bungeecord;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import org.geysermc.common.PlatformType;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.common.bootstrap.IGeyserBootstrap;
import org.geysermc.platform.bungeecord.command.GeyserBungeeCommandExecutor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;

public class GeyserBungeePlugin extends Plugin implements IGeyserBootstrap {

    private GeyserBungeeConfiguration geyserConfig;
    private GeyserBungeeLogger geyserLogger;

    private GeyserConnector connector;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");
        Configuration configuration = null;

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Failed to read/create config.yml! Make sure it's up to date and/or readable+writable!", ex);
                return;
            }
        }
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        if (configuration == null) {
            getLogger().severe("Failed to read/create config.yml! Make sure it's up to date and/or readable+writable!");
            return;
        }

        this.geyserConfig = new GeyserBungeeConfiguration(getDataFolder(), configuration);

        if (geyserConfig.getMetrics().getUniqueId().equals("generateduuid")) {
            configuration.set("metrics.uuid", UUID.randomUUID().toString());
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, new File(getDataFolder(), "config.yml"));
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Failed to read/create config.yml! Make sure it's up to date and/or readable+writable!", ex);
                return;
            }
        }

        this.geyserLogger = new GeyserBungeeLogger(getLogger(), geyserConfig.isDebugMode());
        this.connector = GeyserConnector.start(PlatformType.BUNGEECORD, this);

        this.getProxy().getPluginManager().registerCommand(this, new GeyserBungeeCommandExecutor(connector));
    }

    @Override
    public void onDisable() {
        connector.shutdown();
    }

    @Override
    public GeyserBungeeConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserBungeeLogger getGeyserLogger() {
        return geyserLogger;
    }
}
