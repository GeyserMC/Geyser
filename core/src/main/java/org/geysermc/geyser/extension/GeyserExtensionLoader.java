/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.extension;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.api.Geyser;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.ExtensionEventBus;
import org.geysermc.geyser.api.extension.*;
import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.geysermc.geyser.api.extension.exception.InvalidExtensionException;
import org.geysermc.geyser.extension.event.GeyserExtensionEventBus;
import org.geysermc.geyser.text.GeyserLocale;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class GeyserExtensionLoader extends ExtensionLoader {
    private static final Pattern[] EXTENSION_FILTERS = new Pattern[] { Pattern.compile("^.+\\.jar$") };

    private final Object2ObjectMap<String, Class<?>> classes = new Object2ObjectOpenHashMap<>();
    private final Map<String, GeyserExtensionClassLoader> classLoaders = new HashMap<>();
    private final Map<Extension, GeyserExtensionContainer> extensionContainers = new HashMap<>();
    private final Path extensionsDirectory = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("extensions");

    public GeyserExtensionContainer loadExtension(Path path, GeyserExtensionDescription description) throws InvalidExtensionException {
        if (path == null) {
            throw new InvalidExtensionException("Path is null");
        }

        if (Files.notExists(path)) {
            throw new InvalidExtensionException(new NoSuchFileException(path.toString()) + " does not exist");
        }

        Path parentFile = path.getParent();
        Path dataFolder = parentFile.resolve(description.name());
        if (Files.exists(dataFolder) && !Files.isDirectory(dataFolder)) {
            throw new InvalidExtensionException("The folder " + dataFolder + " is not a directory and is the data folder for the extension " + description.name() + "!");
        }

        final GeyserExtensionClassLoader loader;
        try {
            loader = new GeyserExtensionClassLoader(this, getClass().getClassLoader(), path);
        } catch (Throwable e) {
            throw new InvalidExtensionException(e);
        }

        this.classLoaders.put(description.name(), loader);

        final Extension extension = loader.load(description);
        return this.setup(extension, description, dataFolder, new GeyserExtensionEventBus(GeyserImpl.getInstance().eventBus(), extension));
    }

    private GeyserExtensionContainer setup(Extension extension, GeyserExtensionDescription description, Path dataFolder, ExtensionEventBus eventBus) {
        GeyserExtensionLogger logger = new GeyserExtensionLogger(GeyserImpl.getInstance().getLogger(), description.name());
        return new GeyserExtensionContainer(extension, dataFolder, description, this, logger, eventBus);
    }

    public GeyserExtensionDescription extensionDescription(Path path) throws InvalidDescriptionException {
        Map<String, String> environment = new HashMap<>();
        try (FileSystem fileSystem = FileSystems.newFileSystem(path, environment, null)) {
            Path extensionYml = fileSystem.getPath("extension.yml");
            try (Reader reader = Files.newBufferedReader(extensionYml)) {
                return GeyserExtensionDescription.fromYaml(reader);
            }
        } catch (IOException ex) {
            throw new InvalidDescriptionException("Failed to load extension description for " + path, ex);
        }
    }

    public Pattern[] extensionFilters() {
        return EXTENSION_FILTERS;
    }

    public Class<?> classByName(final String name) throws ClassNotFoundException{
        Class<?> clazz = this.classes.get(name);
        if (clazz != null) {
            return clazz;
        }

        for (GeyserExtensionClassLoader loader : this.classLoaders.values()) {
            clazz = loader.findClass(name, false);
            if (clazz != null) {
                break;
            }
        }

        return clazz;
    }

    void setClass(String name, final Class<?> clazz) {
        this.classes.putIfAbsent(name, clazz);
    }

    @Override
    protected void loadAllExtensions(@NonNull ExtensionManager extensionManager) {
        try {
            if (Files.notExists(extensionsDirectory)) {
                Files.createDirectory(extensionsDirectory);
            }

            Map<String, Path> extensions = new LinkedHashMap<>();
            Map<String, GeyserExtensionContainer> loadedExtensions = new LinkedHashMap<>();

            Pattern[] extensionFilters = this.extensionFilters();
            try (Stream<Path> entries = Files.walk(extensionsDirectory)) {
                entries.forEach(path -> {
                    if (Files.isDirectory(path)) {
                        return;
                    }

                    for (Pattern filter : extensionFilters) {
                        if (!filter.matcher(path.getFileName().toString()).matches()) {
                            return;
                        }
                    }

                    try {
                        GeyserExtensionDescription description = this.extensionDescription(path);

                        String name = description.name();
                        if (extensions.containsKey(name) || extensionManager.extension(name) != null) {
                            GeyserImpl.getInstance().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.extensions.load.duplicate", name, path.toString()));
                            return;
                        }

                        // Completely different API version
                        if (description.majorApiVersion() != Geyser.api().majorApiVersion()) {
                            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_version", name, description.apiVersion()));
                            return;
                        }

                        // If the extension requires new API features, being backwards compatible
                        if (description.minorApiVersion() > Geyser.api().minorApiVersion()) {
                            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_version", name, description.apiVersion()));
                            return;
                        }

                        extensions.put(name, path);
                        loadedExtensions.put(name, this.loadExtension(path, description));
                    } catch (Exception e) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_with_name", path.getFileName(), path.toAbsolutePath()), e);
                    }
                });
            }

            for (GeyserExtensionContainer container : loadedExtensions.values()) {
                this.extensionContainers.put(container.extension(), container);
                this.register(container.extension(), extensionManager);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected boolean isEnabled(@NonNull Extension extension) {
        return this.extensionContainers.get(extension).enabled;
    }

    @Override
    protected void setEnabled(@NonNull Extension extension, boolean enabled) {
        this.extensionContainers.get(extension).enabled = enabled;
    }

    @NonNull
    @Override
    protected Path dataFolder(@NonNull Extension extension) {
        return this.extensionContainers.get(extension).dataFolder();
    }

    @NonNull
    @Override
    protected ExtensionDescription description(@NonNull Extension extension) {
        return this.extensionContainers.get(extension).description();
    }

    @NonNull
    @Override
    protected ExtensionEventBus eventBus(@NonNull Extension extension) {
        return this.extensionContainers.get(extension).eventBus();
    }

    @NonNull
    @Override
    protected ExtensionLogger logger(@NonNull Extension extension) {
        return this.extensionContainers.get(extension).logger();
    }
}
