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
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.annotations.Event;
import org.geysermc.connector.event.events.GeyserEvent;
import org.geysermc.connector.event.events.plugin.PluginDisableEvent;
import org.geysermc.connector.event.events.plugin.PluginEnableEvent;
import org.geysermc.connector.event.handlers.EventHandler;
import org.geysermc.connector.plugin.handlers.PluginLambdaEventHandler;
import org.geysermc.connector.plugin.handlers.PluginMethodEventHandler;
import org.geysermc.connector.plugin.annotations.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * All GeyserPlugins extend from this
 */
@SuppressWarnings("unused")
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

        logger.info(String.format("Loading %s v%s", getName(), getVersion()));

        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();
    }

    // We provide some methods already provided in EventManager as we want to keep track of which EventHandlers
    // belong to a particular plugin. That way we can unregister them all easily.

    /**
     * Create a new EventHandler using a lambda
     */
    public <T extends GeyserEvent> PluginLambdaEventHandler.Builder<T> on(Class<T> cls, PluginLambdaEventHandler.Runnable<T> runnable) {
        return new PluginLambdaEventHandler.Builder<>(this, cls, runnable);
    }

    /**
     * Register an event handler
     */
    public <T extends GeyserEvent> void register(EventHandler<T> handler) {
        this.pluginEventHandlers.add(handler);
        getEventManager().register(handler);
    }

    /**
     * Unregister an event handler
     */
    public <T extends GeyserEvent> void unregister(EventHandler<T> handler) {
        this.pluginEventHandlers.remove(handler);
        getEventManager().unregister(handler);
    }

    /**
     * Register all Events contained in an instantiated class. The methods must be annotated by @Event
     */
    public void registerEvents(Object obj) {
        for (Method method : obj.getClass().getMethods()) {
            // Check that the method is annotated with @Event
            if (method.getAnnotation(Event.class) == null) {
                continue;
            }

            // Make sure it only has a single Event parameter
            if (method.getParameterCount() != 1 || !GeyserEvent.class.isAssignableFrom(method.getParameters()[0].getType())) {
                getLogger().error("Cannot register EventHander as its only parameter must be an Event: " + obj.getClass().getSimpleName() + "#" + method.getName());
                continue;
            }

            EventHandler<?> handler = new PluginMethodEventHandler<>(this, obj, method);
            register(handler);
        }
    }

    /**
     * Unregister all events for a plugin
     */
    public void unregisterAllEvents() {
        for (EventHandler<?> handler : pluginEventHandlers) {
            unregister(handler);
        }
    }

    /**
     * Enable Plugin
     */
    public void enable() {
        logger.info(String.format("Enabling %s v%s", getName(), getVersion()));
        getEventManager().triggerEvent(new PluginEnableEvent(this));
    }

    /**
     * Disable Plugin
     */
    public void disable() {
        logger.info(String.format("Disabling %s v%s", getName(), getVersion()));
        getEventManager().triggerEvent(new PluginDisableEvent(this));
    }

    public GeyserConnector getConnector() {
        return pluginManager.getConnector();
    }

    public String getName() {
        Plugin pluginAnnotation = getClass().getAnnotation(Plugin.class);
        return pluginAnnotation != null && !pluginAnnotation.name().isEmpty() ? pluginAnnotation.name().replace("..","") : "unknown";
    }

    public String getDescription() {
        Plugin pluginAnnotation = getClass().getAnnotation(Plugin.class);
        return pluginAnnotation != null ? pluginAnnotation.description() : "";
    }

    public String getVersion() {
        Plugin pluginAnnotation = getClass().getAnnotation(Plugin.class);
        return pluginAnnotation != null ? pluginAnnotation.version() : "unknown";
    }

    /**
     * Return our Event Manager
     */
    public EventManager getEventManager() {
        return pluginManager.getConnector().getEventManager();
    }

    /**
     * Return our dataFolder based upon the plugin name
     */
    public File getDataFolder() {
        return getConnector().getBootstrap().getConfigFolder().resolve("plugins").resolve(getName()).toFile();
    }

    /**
     * Return an InputStream for a resource file
     */
    public InputStream getResourceAsStream(String name) {
        try {
            URL url = getPluginClassLoader().getResource(name);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
}
