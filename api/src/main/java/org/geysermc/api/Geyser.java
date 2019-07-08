package org.geysermc.api;

import org.geysermc.api.command.CommandMap;
import org.geysermc.api.logger.Logger;
import org.geysermc.api.plugin.PluginManager;

public class Geyser {

    private static Connector connector;

    /**
     * Returns the connector instance for Geyser
     *
     * @return the connector instance for Geyser
     */
    public static Connector getConnector() {
        return connector;
    }

    /**
     * Sets the connector instance for Geyser
     *
     * @param connector the connector instance
     */
    public static void setConnector(Connector connector) {
        Geyser.connector = connector;
    }

    /**
     * Returns the logger
     *
     * @return the logger
     */
    public static Logger getLogger() {
        return connector.getLogger();
    }

    /**
     * Returns the plugin manager
     *
     * @return the plugin manager
     */
    public static PluginManager getPluginManager() {
        return connector.getPluginManager();
    }

    /**
     * Returns the command map
     *
     * @return the command map
     */
    public static CommandMap getCommandMap() {
        return connector.getCommandMap();
    }
}
