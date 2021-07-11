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
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.events.extension.ExtensionDisableEvent;
import org.geysermc.connector.event.events.extension.ExtensionEnableEvent;

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
 * Handles 3rd party extensions for Geyser and will hook into our Event system using annotations
 */
@Getter
@ToString
public class ExtensionManager {

    private final GeyserConnector connector;
    private final File extensionPath;

    private final Map<String, Class<?>> globalClasses = new ConcurrentHashMap<>();
    private final List<GeyserExtension> extensions = new ArrayList<>();

    public ExtensionManager(GeyserConnector connector, File extensionPath) {
        this.connector = connector;
        this.extensionPath = extensionPath;
        loadExtensions();
    }

    /**
     * Load all extensions in the defined extensionPath
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void loadExtensions() {
        extensionPath.mkdirs();
        for (File entry : Objects.requireNonNull(extensionPath.listFiles())) {
            if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }
            try {
                loadExtension(entry);
            } catch (IOException | ExtensionManagerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load a specific extension and register its events
     */
    private void loadExtension(File extensionFile) throws IOException, ExtensionManagerException {
        if (!extensionFile.exists()) {
            throw new FileNotFoundException(String.format("%s does not exist", extensionFile.getName()));
        }

        ExtensionClassLoader loader;
        try {
            loader = new ExtensionClassLoader(this, getClass().getClassLoader(), extensionFile);
        } catch (ExtensionClassLoader.InvalidExtensionClassLoaderException e) {
            throw new ExtensionManagerException(e.getMessage(), e);
        }

        GeyserExtension extension;

        try {
            extension = loader.getExtensionClass().getConstructor(ExtensionManager.class, ExtensionClassLoader.class).newInstance(this, loader);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new ExtensionManagerException(e.getMessage(), e);
        }

        extensions.add(extension);
        extension.registerEvents(extension);
    }

    /**
     * Enable all Extensions
     *
     * This may eventually use dependency priority to determine order of enabling but for now relies
     * on the priority
     */
    public void enableExtensions() {
        for (GeyserExtension extension : extensions) {
            connector.getLogger().info(String.format("Enabling %s v%s", extension.getName(), extension.getVersion()));
            EventManager.getInstance().triggerEvent(new ExtensionEnableEvent(extension));
            extension.enable();
        }
    }

    /**
     * Disable all Extensions
     */
    public void disableExtensions() {
        for (GeyserExtension extension : extensions) {
            connector.getLogger().info(String.format("Disabling %s v%s", extension.getName(), extension.getVersion()));
            EventManager.getInstance().triggerEvent(new ExtensionDisableEvent(extension));
            extension.disable();
        }
    }

    @SuppressWarnings("unused")
    public static class ExtensionManagerException extends Exception {

        public ExtensionManagerException(String message, Throwable ex) {
            super(message, ex);
        }

        public ExtensionManagerException(Throwable ex) {

        }

    }
}
