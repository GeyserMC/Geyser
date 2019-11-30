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
     * Returns the general thread pool
     *
     * @return the general thread pool
     */
    ScheduledExecutorService getGeneralThreadPool();

    /**
     * Returns a collection of the connected players
     *
     * @return a collection of the connected players
     */
    Collection<? extends Player> getConnectedPlayers();

    /**
     * Shuts down the connector
     */
    void shutdown();

    /**
     * The auth type for the remote server
     */
    AuthType getAuthType();
}
