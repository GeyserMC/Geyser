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

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.ExtensionEventBus;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.ExtensionLoader;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.extension.ExtensionManager;
import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.geysermc.geyser.api.extension.exception.InvalidExtensionException;
import org.geysermc.geyser.extension.event.GeyserExtensionEventBus;
import org.geysermc.geyser.text.GeyserLocale;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class GeyserExtensionLoader extends ExtensionLoader {
    private static final Path EXTENSION_DIRECTORY = Paths.get("extensions");
    private static final Pattern API_VERSION_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+$");

    private final Map<String, Class<?>> classes = new HashMap<>();
    private final Map<String, GeyserExtensionClassLoader> classLoaders = new HashMap<>();
    private final Map<Extension, GeyserExtensionContainer> extensionContainers = new HashMap<>();

    public GeyserExtensionContainer loadExtension(Path path, GeyserExtensionDescription description) throws InvalidExtensionException, InvalidDescriptionException {
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
            loader = new GeyserExtensionClassLoader(this, getClass().getClassLoader(), description, path);
        } catch (Throwable e) {
            throw new InvalidExtensionException(e);
        }

        this.classLoaders.put(description.name(), loader);
        return this.setup(loader.extension(), description, dataFolder, new GeyserExtensionEventBus(GeyserImpl.getInstance().eventBus(), loader.extension()));
    }

    private GeyserExtensionContainer setup(Extension extension, GeyserExtensionDescription description, Path dataFolder, ExtensionEventBus eventBus) {
        GeyserExtensionLogger logger = new GeyserExtensionLogger(GeyserImpl.getInstance().getLogger(), description.name());
        return new GeyserExtensionContainer(extension, dataFolder, description, this, logger, eventBus);
    }

    public GeyserExtensionDescription extensionDescription(Path path) throws InvalidDescriptionException {
        Map<String, String> environment = new HashMap<>();
        try (FileSystem fileSystem = FileSystems.newFileSystem(path, environment, null)) {
            Path extensionYml = fileSystem.getPath("extension.yml");
            return GeyserExtensionDescription.fromYaml(Files.newBufferedReader(extensionYml));
        } catch (IOException ex) {
            throw new InvalidDescriptionException("Failed to load extension description for " + path, ex);
        }
    }

    public Pattern[] extensionFilters() {
        return new Pattern[] { Pattern.compile("^.+\\.jar$") };
    }

    public Class<?> classByName(final String name) throws ClassNotFoundException{
        Class<?> clazz = this.classes.get(name);
        try {
            for (GeyserExtensionClassLoader loader : this.classLoaders.values()) {
                try {
                    clazz = loader.findClass(name,false);
                } catch(NullPointerException ignored) {
                }
            }
            return clazz;
        } catch (NullPointerException s) {
            return null;
        }
    }

    void setClass(String name, final Class<?> clazz) {
        if (!this.classes.containsKey(name)) {
            this.classes.put(name,clazz);
        }
    }

    @Override
    protected void loadAllExtensions(@NonNull ExtensionManager extensionManager) {
        // noinspection ConstantConditions
        if (!GeyserImpl.VERSION.contains(".")) {
            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_version_number"));
            return;
        }

        String[] apiVersion = GeyserImpl.VERSION.split("\\.");

        try {
            if (Files.notExists(EXTENSION_DIRECTORY)) {
                Files.createDirectory(EXTENSION_DIRECTORY);
            }

            Map<String, Path> extensions = new LinkedHashMap<>();
            Map<String, GeyserExtensionContainer> loadedExtensions = new LinkedHashMap<>();

            Pattern[] extensionFilters = this.extensionFilters();

            Files.walk(EXTENSION_DIRECTORY).forEach(path -> {
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
                    if (description == null) {
                        return;
                    }

                    String name = description.name();
                    if (extensions.containsKey(name) || extensionManager.extension(name) != null) {
                        GeyserImpl.getInstance().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.extensions.load.duplicate", name, path.toString()));
                        return;
                    }

                    try {
                        // Check the format: majorVersion.minorVersion.patch
                        if (!API_VERSION_PATTERN.matcher(description.apiVersion()).matches()) {
                            throw new IllegalArgumentException();
                        }
                    } catch (NullPointerException | IllegalArgumentException e) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_format", name, apiVersion[0] + "." + apiVersion[1]));
                        return;
                    }

                    String[] versionArray = description.apiVersion().split("\\.");

                    // Completely different API version
                    if (!Objects.equals(Integer.valueOf(versionArray[0]), Integer.valueOf(apiVersion[0]))) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_version", name, apiVersion[0] + "." + apiVersion[1]));
                        return;
                    }

                    // If the extension requires new API features, being backwards compatible
                    if (Integer.parseInt(versionArray[1]) > Integer.parseInt(apiVersion[1])) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_version", name, apiVersion[0] + "." + apiVersion[1]));
                        return;
                    }

                    extensions.put(name, path);
                    loadedExtensions.put(name, this.loadExtension(path, description));
                } catch (Exception e) {
                    GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_with_name", path.getFileName(), path.toAbsolutePath()), e);
                }
            });

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
