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

package org.geysermc.connector.plugin;

import lombok.Getter;
import org.geysermc.api.events.EventHandler;
import org.geysermc.api.events.Listener;
import org.geysermc.api.plugin.Plugin;
import org.geysermc.api.plugin.PluginManager;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class GeyserPluginManager implements PluginManager {
    private final List<PluginListener> EVENTS = new ArrayList<>();

    @Getter
    private GeyserPluginLoader loader;

    private Map<String, Plugin> plugins = new HashMap<>();

    public GeyserPluginManager(GeyserPluginLoader loader) {
        this.loader = loader;
    }

    public void loadPlugin(Plugin plugin) {
        loader.loadPlugin(plugin);
        plugins.put(plugin.getName(), plugin);
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
        return new HashSet<>(plugins.values());
    }

    @Override
    public void registerEventListener(Plugin p, Listener l) {
        try {
            Class<? extends Listener> clazz = l.getClass();

            for(Method m : clazz.getMethods()) {
                if(m.getAnnotation(EventHandler.class) != null) {
                    PluginListener listener = new PluginListener();

                    listener.plugin = p;
                    listener.listener = l;
                    listener.clazz = m.getParameterTypes()[0];
                    listener.priority = m.getAnnotation(EventHandler.class).value();
                    listener.run = m;
                    EVENTS.add(listener);
                }
            }
        } catch (Exception e) {
            //
        }
    }

    @Override
    public void runEvent(Object o) {
        for(EventHandler.EventPriority p : EventHandler.EventPriority.values()) {
            for (PluginListener listener : EVENTS) {
                listener.runIfNeeded(p, o);
            }
        }
    }

    @Override
    public Plugin getPluginByName(String name) {
        return plugins.get(name);
    }
}