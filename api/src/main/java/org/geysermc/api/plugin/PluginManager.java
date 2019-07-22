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

package org.geysermc.api.plugin;

import org.geysermc.api.events.Listener;

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

    /**
     * @param name The name of the plugin you want to get.
     * @return The plugin with the String name in the parameters.
     */
    Plugin getPluginByName(String name);

    /**
     * Registers a listener to be run when an event is executed
     * @param plugin the plugin registering the listener
     * @param listener the listener which will contain the event methods
     */
    void registerEventListener(Plugin plugin, Listener listener);

    /**
     * Run an event
     * @param o the event object.
     */
    void runEvent(Object o);
}
