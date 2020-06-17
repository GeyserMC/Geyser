/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.plugin;

import lombok.AllArgsConstructor;
import org.geysermc.connector.GeyserLogger;


/**
 * Provides a proxy to the main logger that prefixes the plugin name to messages
 */
@AllArgsConstructor
public class PluginLogger implements GeyserLogger {

    private final GeyserPlugin plugin;

    private String prefixMessage(String message) {
        return String.format("{%s} %s", plugin.getName(), message);
    }

    @Override
    public void severe(String message) {
        plugin.getConnector().getLogger().severe(prefixMessage(message));
    }

    @Override
    public void severe(String message, Throwable error) {
        plugin.getConnector().getLogger().severe(prefixMessage(message), error);
    }

    @Override
    public void error(String message) {
        plugin.getConnector().getLogger().error(prefixMessage(message));
    }

    @Override
    public void error(String message, Throwable error) {
        plugin.getConnector().getLogger().severe(prefixMessage(message), error);
    }

    @Override
    public void warning(String message) {
        plugin.getConnector().getLogger().warning(prefixMessage(message));
    }

    @Override
    public void info(String message) {
        plugin.getConnector().getLogger().info(prefixMessage(message));
    }

    @Override
    public void debug(String message) {
        plugin.getConnector().getLogger().debug(prefixMessage(message));
    }

    @Override
    public void setDebug(boolean debug) {
        plugin.getConnector().getLogger().setDebug(debug);
    }
}
