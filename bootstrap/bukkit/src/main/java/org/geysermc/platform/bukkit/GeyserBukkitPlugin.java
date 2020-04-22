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

package org.geysermc.platform.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.common.PlatformType;
import org.geysermc.common.command.ICommandManager;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.common.bootstrap.IGeyserBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.platform.bukkit.command.GeyserBukkitCommandExecutor;
import org.geysermc.platform.bukkit.command.GeyserBukkitCommandManager;

import java.util.UUID;

public class GeyserBukkitPlugin extends JavaPlugin implements IGeyserBootstrap {

    private GeyserBukkitCommandManager geyserCommandManager;
    private GeyserBukkitConfiguration geyserConfig;
    private GeyserBukkitLogger geyserLogger;

    private GeyserConnector connector;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.geyserConfig = new GeyserBukkitConfiguration(getDataFolder(), getConfig());
        if (geyserConfig.getMetrics().getUniqueId().equals("generateduuid")) {
            getConfig().set("metrics.uuid", UUID.randomUUID().toString());
            saveConfig();
        }

        this.geyserLogger = new GeyserBukkitLogger(getLogger(), geyserConfig.isDebugMode());
        this.connector = GeyserConnector.start(PlatformType.BUKKIT, this);

        this.geyserCommandManager = new GeyserBukkitCommandManager(this, connector);

        this.getCommand("geyser").setExecutor(new GeyserBukkitCommandExecutor(connector));
    }

    @Override
    public void onDisable() {
        connector.shutdown();
    }

    @Override
    public GeyserBukkitConfiguration getGeyserConfig() {
        return geyserConfig;
    }

    @Override
    public GeyserBukkitLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandManager getGeyserCommandManager() {
        return this.geyserCommandManager;
    }
}
