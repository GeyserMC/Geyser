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
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.event.EventHandler;
import org.geysermc.connector.event.EventRegisterResult;
import org.geysermc.connector.event.events.GeyserEvent;
import org.geysermc.connector.event.events.PluginDisableEvent;
import org.geysermc.connector.event.events.PluginEnableEvent;
import org.geysermc.connector.plugin.annotations.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * All GeyserPlugins extend from this
 */
@Getter
public abstract class GeyserPlugin {
    // List of EventHandlers associated with this Plugin
    private final List<EventHandler<?>> pluginEventHandlers = new ArrayList<>();

    private final PluginManager pluginManager;
    private final PluginClassLoader pluginClassLoader;
    private final PluginLogger logger;

    public GeyserPlugin(PluginManager pluginManager, PluginClassLoader pluginClassLoader) {
        this.pluginManager = pluginManager;
        this.pluginClassLoader = pluginClassLoader;
        this.logger = new PluginLogger(this);
    }

    // We provide some methods already provided in EventManager as we want to keep track of which EventHandlers
    // belong to a particular plugin. That way we can unregister them all easily.

    /**
     * Create a new EventHandler using an Executor
     */
    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor, int priority, boolean ignoreCancelled) {
        EventRegisterResult result = pluginManager.getConnector().getEventManager().on(cls, executor, priority, ignoreCancelled);
        pluginEventHandlers.add(result.getHandler());
        return result;
    }

    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor) {
        return on(cls, executor, EventHandler.PRIORITY.NORMAL, true);
    }

    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor, boolean ignoreCancelled) {
        return on(cls, executor, EventHandler.PRIORITY.NORMAL, ignoreCancelled);
    }

    public <T extends GeyserEvent> EventRegisterResult on(Class<? extends T> cls, EventHandler.Executor<T> executor, int priority) {
        return on(cls, executor, priority, true);
    }

    /**
     * Register all Events contained in an instantiated class. The methods must be annotated by @Event
     */
    public void registerEvents(Object obj) {
        EventHandler<?>[] handlers = pluginManager.getConnector().getEventManager().registerEvents(obj);
        pluginEventHandlers.addAll(Arrays.asList(handlers));
    }

    /**
     * Unregister all events for a plugin
     */
    public void unregisterAllEvents() {
        for (EventHandler<?> handler : pluginEventHandlers) {
            pluginManager.getConnector().getEventManager().unregister(handler);
        }
    }

    /**
     * Enable Plugin
     */
    public void enable() {
        pluginManager.getConnector().getEventManager().triggerEvent(new PluginEnableEvent(this));
    }

    /**
     * Disable Plugin
     */
    public void disable() {
        pluginManager.getConnector().getEventManager().triggerEvent(new PluginDisableEvent(this));
    }

    public GeyserConnector getConnector() {
        return pluginManager.getConnector();
    }

    public String getName() {
        Plugin pluginAnnotation = getClass().getAnnotation(Plugin.class);
        return pluginAnnotation != null ? pluginAnnotation.name() : "unknown";
    }

    public String getDescription() {
        Plugin pluginAnnotation = getClass().getAnnotation(Plugin.class);
        return pluginAnnotation != null ? pluginAnnotation.description() : "";
    }

    public String getVersion() {
        Plugin pluginAnnotation = getClass().getAnnotation(Plugin.class);
        return pluginAnnotation != null ? pluginAnnotation.version() : "unknown";
    }
}
