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

package org.geysermc.api;

import org.geysermc.api.command.CommandMap;
import org.geysermc.api.logger.Logger;
import org.geysermc.api.plugin.PluginManager;

public interface Connector {

    /**
     * Returns the logger
     *
     * @return the logger
     */
    Logger getLogger();

    /**
     * Returns the command map
     *
     * @return the command map
     */
    CommandMap getCommandMap();

    /**
     * Returns the plugin manager
     *
     * @return the plugin manager
     */
    PluginManager getPluginManager();

    /**
     * Shuts down the connector
     */
    void shutdown();
}
