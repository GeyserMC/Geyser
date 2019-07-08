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

package org.geysermc.api.plugin;

import java.util.Set;

public interface PluginManager {

    /**
     * Loads a plugin and all its class files
     *
     * @param plugin the plugin to load
     */
    void loadPlugin(Plugin plugin);

    /**
     * Enables a plugin
     *
     * @param plugin the plugin to enable
     */
    void enablePlugin(Plugin plugin);

    /**
     * Disables a plugin
     *
     * @param plugin the plugin to disable
     */
    void disablePlugin(Plugin plugin);

    /**
     * Unloads a plugin
     *
     * @param plugin the plugin to unload
     */
    void unloadPlugin(Plugin plugin);

    /**
     * Returns a set of the loaded plugins
     *
     * @return a set of the loaded plugins
     */
    Set<Plugin> getPlugins();
}
