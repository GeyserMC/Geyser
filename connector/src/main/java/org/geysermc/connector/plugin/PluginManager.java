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
import org.geysermc.connector.plugin.annotations.Event;
import org.geysermc.connector.plugin.api.CancellableGeyserEvent;
import org.geysermc.connector.plugin.api.GeyserEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles 3rd party plugins for Geyser
 */
@Getter
@ToString
public class PluginManager {

    private final GeyserConnector connector;
    private final File pluginPath;

    private final Map<String, Class<?>> pluginClasses = new ConcurrentHashMap<>();
    private final List<PluginClassLoader> loaders = new ArrayList<>();
    private final Map<Class<?>, PriorityQueue<EventHandler>> eventHandlers = new HashMap<>();

    public PluginManager(GeyserConnector connector, File pluginPath) {
        this.connector = connector;
        this.pluginPath = pluginPath;
        loadPlugins();
    }

    /**
     * Load all plugins in the defined pluginPath
     */
    public void loadPlugins() {
        pluginPath.mkdirs();
        for (File entry : pluginPath.listFiles()) {
            if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }
            try {
                loadPlugin(entry);
            } catch (IOException | PluginClassLoader.InvalidPluginClassLoaderException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load a specific plugin and register its events
     */
    private void loadPlugin(File pluginFile) throws IOException, PluginClassLoader.InvalidPluginClassLoaderException {
        if (!pluginFile.exists()) {
            throw new FileNotFoundException(String.format("%s does not exist", pluginFile.getName()));
        }
        PluginClassLoader loader = new PluginClassLoader(this, getClass().getClassLoader(), pluginFile);
        registerEvents(loader.getPlugin(), loader.getPlugin());
        loaders.add(loader);
    }


    /**
     * Register all Events contained in class
     */
    public void registerEvents(Object plugin, Object cls) {
        for (Method method : cls.getClass().getMethods()) {
            Event eventAnnotation = method.getAnnotation(Event.class);

            // Check that the method is annotated with @Event
            if (eventAnnotation == null) {
                continue;
            }

            // Make sure it only has a single Event parameter
            if (method.getParameterCount() != 1 || !GeyserEvent.class.isAssignableFrom(method.getParameters()[0].getType())) {
                continue;
            }

            if (!eventHandlers.containsKey(method.getParameters()[0].getType())) {
                eventHandlers.put(method.getParameters()[0].getType(), new PriorityQueue<>());
            }

            eventHandlers.get(method.getParameters()[0].getType()).add(
                    new EventHandler(
                            plugin,
                            cls,
                            method,
                            eventAnnotation
                    )
            );
        }
    }

    /**
     * Unregister events in class
     */
    public void unregisterEvents(Class<?> cls) {
        for (Map.Entry<Class<?>, PriorityQueue<EventHandler>> entry : eventHandlers.entrySet()) {
            List<EventHandler> remove = new ArrayList<>();
            for (EventHandler handler : entry.getValue()) {
                if (handler.cls == cls) {
                    remove.add(handler);
                }
            }
            entry.getValue().removeAll(remove);
        }
    }

    /**
     * Unregister all events for a plugin
     */
    public void unregisterPluginEvents(Class<?> plugin) {
        for (Map.Entry<Class<?>, PriorityQueue<EventHandler>> entry : eventHandlers.entrySet()) {
            List<EventHandler> remove = new ArrayList<>();
            for (EventHandler handler : entry.getValue()) {
                if (handler.plugin == plugin) {
                    remove.add(handler);
                }
            }
            entry.getValue().removeAll(remove);
        }
    }

    /**
     * Trigger a new event
     *
     * This will be executed with all registered handlers in order of priority
     */
    public void triggerEvent(GeyserEvent event) {
        if (!eventHandlers.containsKey(event.getClass())) {
            return;
        }

        for (EventHandler handler : eventHandlers.get(event.getClass())) {
            try {
                handler.method.invoke(handler.cls, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Trigger a new cancellable event
     *
     * This will be executed with all registered handlers in order of priority ignoring those who
     * do not wish to process events that are cancelled
     */
    public void triggerEvent(CancellableGeyserEvent event) {
        if (!eventHandlers.containsKey(event.getClass())) {
            return;
        }

        boolean cancelled = event.isCancelled();
        for (EventHandler handler : eventHandlers.get(event.getClass())) {
            if (cancelled && handler.annotation.ignoreCancelled()) {
                continue;
            }

            try {
                handler.method.invoke(handler.cls, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            cancelled = event.isCancelled();
        }
    }

    static class EventHandler implements Comparator<EventHandler> {
        Object plugin;
        Object cls;
        Method method;
        Event annotation;

        EventHandler(Object plugin, Object cls, Method method, Event annotation) {
            this.plugin = plugin;
            this.cls = cls;
            this.method = method;
            this.annotation = annotation;
        }

        @Override
        public int compare(EventHandler left, EventHandler right) {
            return left.annotation.priority() - right.annotation.priority();
        }
    }
}
