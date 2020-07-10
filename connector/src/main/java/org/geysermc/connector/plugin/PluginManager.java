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

import lombok.Getter;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles 3rd party plugins for Geyser and will hook into our Event system using annotations
 */
@Getter
@ToString
public class PluginManager {

    private final GeyserConnector connector;
    private final File pluginPath;

    private final Map<String, Class<?>> globalClasses = new ConcurrentHashMap<>();
    private final List<GeyserPlugin> plugins = new ArrayList<>();

    public PluginManager(GeyserConnector connector, File pluginPath) {
        this.connector = connector;
        this.pluginPath = pluginPath;
        loadPlugins();
    }

    /**
     * Load all plugins in the defined pluginPath
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void loadPlugins() {
        pluginPath.mkdirs();
        for (File entry : Objects.requireNonNull(pluginPath.listFiles())) {
            if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }
            try {
                loadPlugin(entry);
            } catch (IOException | PluginManagerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load a specific plugin and register its events
     */
    private void loadPlugin(File pluginFile) throws IOException, PluginManagerException {
        if (!pluginFile.exists()) {
            throw new FileNotFoundException(String.format("%s does not exist", pluginFile.getName()));
        }

        PluginClassLoader loader = null;
        try {
            loader = new PluginClassLoader(this, getClass().getClassLoader(), pluginFile);
        } catch (PluginClassLoader.InvalidPluginClassLoaderException e) {
            throw new PluginManagerException(e.getMessage(), e);
        }

        GeyserPlugin plugin;

        try {
            plugin = loader.getPluginClass().getConstructor(PluginManager.class, PluginClassLoader.class).newInstance(this, loader);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new PluginManagerException(e.getMessage(), e);
        }

        plugins.add(plugin);
        plugin.registerEvents(plugin);
    }

    /**
     * Enable all Plugins
     *
     * This may eventually use dependency priority to determine order of enabling but for now relies
     * on the priority
     */
    public void enablePlugins() {
        for (GeyserPlugin plugin : plugins) {
            plugin.enable();
        }
    }

    /**
     * Disable all Plugins
     */
    public void disablePlugins() {
        for (GeyserPlugin plugin : plugins) {
            plugin.disable();
        }
    }

    public static class PluginManagerException extends Exception {

        public PluginManagerException(String message, Throwable ex) {
            super(message, ex);
        }

        public PluginManagerException(Throwable ex) {

        }

    }
}
