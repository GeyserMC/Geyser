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

package org.geysermc.api;

import org.geysermc.api.command.CommandMap;
import org.geysermc.api.logger.Logger;
import org.geysermc.api.plugin.PluginManager;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

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

    public static ScheduledExecutorService getGeneralThreadPool() {
        return connector.getGeneralThreadPool();
    }

    /**
     * @return the amount of online players
     */
    public static int getPlayerCount() {
        return connector.getConnectedPlayers().size();
    }

    /**
     * Returns a collection of the connected players
     *
     * @return a collection of the connected players
     */
    public static Collection<? extends Player> getConnectedPlayers() {
        return connector.getConnectedPlayers();
    }
}
