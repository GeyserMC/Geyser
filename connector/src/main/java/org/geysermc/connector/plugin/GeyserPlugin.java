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
import lombok.Getter;
import org.geysermc.connector.event.EventHandler;
import org.geysermc.connector.event.events.GeyserEvent;
import org.geysermc.connector.plugin.annotations.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * All GeyserPlugins extend from this
 */
@Getter
@AllArgsConstructor
public abstract class GeyserPlugin {
    private final Map<Object, ArrayList<EventHandler<?>>> classEventHandlers = new HashMap<>();

    private final PluginManager pluginManager;
    private final PluginClassLoader pluginClassLoader;

    /**
     * Register all Events contained in a class
     */
    public void registerEvents(Object obj) {
        for (Method method : obj.getClass().getMethods()) {
            Event eventAnnotation = method.getAnnotation(Event.class);

            // Check that the method is annotated with @Event
            if (eventAnnotation == null) {
                continue;
            }

            // Make sure it only has a single Event parameter
            if (method.getParameterCount() != 2 || !GeyserEvent.class.isAssignableFrom(method.getParameters()[1].getType())) {
                continue;
            }

            //noinspection unchecked
            EventHandler<?> handler = pluginManager.getConnector().getEventManager()
                    .on((Class<? extends GeyserEvent>)method.getParameters()[1].getType(), (ctx, event) -> {
                try {
                    method.invoke(obj, ctx, event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });

            if (!classEventHandlers.containsKey(obj.getClass())) {
                classEventHandlers.put(obj, new ArrayList<>());
            }

            classEventHandlers.get(obj).add(handler);
        }
    }

    /**
     * Unregister events in class
     */
    public void unregisterEvents(Object obj) {
        if (!classEventHandlers.containsKey(obj)) {
            return;
        }

        for (EventHandler<?> handler : classEventHandlers.get(obj)) {
            pluginManager.getConnector().getEventManager().unregister(handler);
        }

        classEventHandlers.remove(obj);
    }

    /**
     * Unregister all events for a plugin
     */
    public void unregisterPluginEvents(Class<?> plugin) {
        for (Object obj : classEventHandlers.keySet()) {
            unregisterEvents(obj);
        }
    }
}
