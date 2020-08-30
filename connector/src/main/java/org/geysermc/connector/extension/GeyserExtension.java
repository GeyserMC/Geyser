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

package org.geysermc.connector.extension;

import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.annotations.GeyserEventHandler;
import org.geysermc.connector.event.GeyserEvent;
import org.geysermc.connector.event.handlers.EventHandler;
import org.geysermc.connector.extension.handlers.ExtensionLambdaEventHandler;
import org.geysermc.connector.extension.handlers.ExtensionMethodEventHandler;
import org.geysermc.connector.extension.annotations.Extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * All GeyserExtensions extend from this
 */
@SuppressWarnings("unused")
@Getter
public abstract class GeyserExtension {
    // List of EventHandlers associated with this Extension
    private final List<EventHandler<?>> extensionEventHandlers = new ArrayList<>();

    private final ExtensionManager extensionManager;
    private final ExtensionClassLoader extensionClassLoader;
    private final ExtensionLogger logger;

    public GeyserExtension(ExtensionManager extensionManager, ExtensionClassLoader extensionClassLoader) {
        this.extensionManager = extensionManager;
        this.extensionClassLoader = extensionClassLoader;
        this.logger = new ExtensionLogger(this);

        logger.info(String.format("Loading %s v%s", getName(), getVersion()));

        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();
    }

    // We provide some methods already provided in EventManager as we want to keep track of which EventHandlers
    // belong to a particular extension. That way we can unregister them all easily.

    /**
     * Create a new EventHandler using a lambda
     *
     * @param cls Event class to await
     * @param consumer code to execute
     * @param <T> Event class
     * @return The event handler
     */
    public <T extends GeyserEvent> ExtensionLambdaEventHandler<T> on(Class<T> cls, Consumer<T> consumer) {
        return on(cls, (event, handler) -> consumer.accept(event));
    }

    public <T extends GeyserEvent> ExtensionLambdaEventHandler<T> on(Class<T> cls, BiConsumer<T, EventHandler<T>> consumer) {
        return new ExtensionLambdaEventHandler<>(this, cls, consumer);
    }

    /**
     * Register an event handler
     *
     * @param handler EventHandler to register
     * @param <T> Event class
     */
    public <T extends GeyserEvent> void register(EventHandler<T> handler) {
        this.extensionEventHandlers.add(handler);
        getEventManager().register(handler);
    }

    /**
     * Unregister an event handler
     *
     * @param handler EventHandler to unregister
     * @param <T> Event class
     */
    public <T extends GeyserEvent> void unregister(EventHandler<T> handler) {
        this.extensionEventHandlers.remove(handler);
        getEventManager().unregister(handler);
    }

    /**
     * Register all Events contained in an instantiated class. The methods must be annotated by {@code GeyserEvent}
     * @param obj Class to register events
     */
    public void registerEvents(Object obj) {
        for (Method method : obj.getClass().getMethods()) {
            // Check that the method is annotated with @Event
            if (method.getAnnotation(GeyserEventHandler.class) == null) {
                continue;
            }

            // Make sure it only has a single Event parameter
            if (method.getParameterCount() != 1 || !GeyserEvent.class.isAssignableFrom(method.getParameters()[0].getType())) {
                getLogger().error("Cannot register EventHander as its only parameter must be an Event: " + obj.getClass().getSimpleName() + "#" + method.getName());
                continue;
            }

            EventHandler<?> handler = new ExtensionMethodEventHandler<>(this, obj, method);
            register(handler);
        }
    }

    /**
     *  Unregister all events for a extension
     */
    public void unregisterAllEvents() {
        for (EventHandler<?> handler : extensionEventHandlers) {
            unregister(handler);
        }
    }

    /**
     * Enable Extension
     *
     * Override this to catch when the extension is enabled
     */
    public void enable() {
    }

    /**
     * Disable Extension
     *
     * Override this to catch when the extension is disabled
     */
    public void disable() {
    }

    public GeyserConnector getConnector() {
        return extensionManager.getConnector();
    }

    public String getName() {
        Extension extensionAnnotation = getClass().getAnnotation(Extension.class);
        return extensionAnnotation != null && !extensionAnnotation.name().isEmpty() ? extensionAnnotation.name().replace("..","") : "unknown";
    }

    public String getDescription() {
        Extension extensionAnnotation = getClass().getAnnotation(Extension.class);
        return extensionAnnotation != null ? extensionAnnotation.description() : "";
    }

    public String getVersion() {
        Extension extensionAnnotation = getClass().getAnnotation(Extension.class);
        return extensionAnnotation != null ? extensionAnnotation.version() : "unknown";
    }

    /**
     * Return our Event Manager
     * @return Event Manager
     */
    public EventManager getEventManager() {
        return extensionManager.getConnector().getEventManager();
    }

    /**
     * Return our dataFolder based upon the extension name
     * @return File to datafolder
     */
    public File getDataFolder() {
        return getConnector().getBootstrap().getConfigFolder().resolve("extensions").resolve(getName()).toFile();
    }

    /**
     * Return an InputStream for a resource file
     *
     * @param name Name of file
     * @return InputStream to resource or null
     */
    public InputStream getResourceAsStream(String name) {
        try {
            URL url = getExtensionClassLoader().getResource(name);

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
