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

package org.geysermc.connector.plugin;

import lombok.Getter;
import org.geysermc.api.plugin.Plugin;
import org.geysermc.api.plugin.PluginManager;

import java.util.HashSet;
import java.util.Set;

public class GeyserPluginManager implements PluginManager {

    @Getter
    private GeyserPluginLoader loader;

    @Getter
    private Set<Plugin> plugins = new HashSet<Plugin>();

    public GeyserPluginManager(GeyserPluginLoader loader) {
        this.loader = loader;
    }

    public void loadPlugin(Plugin plugin) {
        loader.loadPlugin(plugin);
        plugins.add(plugin);
    }

    public void unloadPlugin(Plugin plugin) {
        plugins.remove(plugin);
    }

    public void enablePlugin(Plugin plugin) {
        loader.enablePlugin(plugin);
    }

    public void disablePlugin(Plugin plugin) {
        loader.disablePlugin(plugin);
    }

    public Set<Plugin> getPlugins() {
        return plugins;
    }
}