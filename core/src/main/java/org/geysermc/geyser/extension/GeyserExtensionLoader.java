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
import org.geysermc.api.util.ApiVersion;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.GeyserApi;
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
import org.geysermc.geyser.util.ThrowingBiConsumer;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class GeyserExtensionLoader extends ExtensionLoader {
    private static final Pattern EXTENSION_FILTER = Pattern.compile("^.+\\.jar$");

    private final Object2ObjectMap<String, Class<?>> classes = new Object2ObjectOpenHashMap<>();
    private final Map<String, GeyserExtensionClassLoader> classLoaders = new HashMap<>();
    private final Map<Extension, GeyserExtensionContainer> extensionContainers = new HashMap<>();
    private final Path extensionsDirectory = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("extensions");

    public GeyserExtensionContainer loadExtension(Path path, GeyserExtensionDescription description) throws Throwable {
        if (path == null) {
            throw new InvalidExtensionException("Path is null");
        }

        if (Files.notExists(path)) {
            throw new InvalidExtensionException(new NoSuchFileException(path.toString()) + " does not exist");
        }

        Path parentFile = path.getParent();

        
        Path oldDataFolder = parentFile.resolve(description.name());
        Path dataFolder = parentFile.resolve(description.id());

        if (Files.exists(oldDataFolder) && Files.isDirectory(oldDataFolder) && !oldDataFolder.equals(dataFolder)) {
            try {
                Files.move(oldDataFolder, dataFolder, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new InvalidExtensionException("Failed to move data folder for extension " + description.name(), e);
            }
        }

        if (Files.exists(dataFolder) && !Files.isDirectory(dataFolder)) {
            throw new InvalidExtensionException("The folder " + dataFolder + " is not a directory and is the data folder for the extension " + description.name() + "!");
        }

        final GeyserExtensionClassLoader loader;
        try {
            loader = new GeyserExtensionClassLoader(this, getClass().getClassLoader(), path, description);
        } catch (Throwable e) {
            throw new InvalidExtensionException(e);
        }

        this.classLoaders.put(description.id(), loader);

        try {
            final Extension extension = loader.load();
            return this.setup(extension, description, dataFolder, new GeyserExtensionEventBus(GeyserImpl.getInstance().eventBus(), extension));
        } catch (Throwable e) {
            
            this.classLoaders.remove(description.id()).close();
            throw e;
        }
    }

    private GeyserExtensionContainer setup(Extension extension, GeyserExtensionDescription description, Path dataFolder, ExtensionEventBus eventBus) {
        GeyserExtensionLogger logger = new GeyserExtensionLogger(GeyserImpl.getInstance().getLogger(), description.id());
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

    public Pattern extensionFilter() {
        return EXTENSION_FILTER;
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
        GeyserLogger logger = GeyserImpl.getInstance().getLogger();
        try {
            if (Files.notExists(extensionsDirectory)) {
                Files.createDirectory(extensionsDirectory);
            }

            Map<String, Path> extensions = new LinkedHashMap<>();
            Map<String, GeyserExtensionContainer> loadedExtensions = new LinkedHashMap<>();
            Map<String, GeyserExtensionDescription> descriptions = new LinkedHashMap<>();
            Map<String, Path> extensionPaths = new LinkedHashMap<>();

            Path updateDirectory = extensionsDirectory.resolve("update");
            if (Files.isDirectory(updateDirectory)) {
                
                Map<String, List<Path>> extensionFiles = new HashMap<>();
                this.processExtensionsFolder(extensionsDirectory, (path, description) -> {
                    extensionFiles.computeIfAbsent(description.id(), k -> new ArrayList<>()).add(path);
                }, (path, e) -> {
                    
                });

                
                this.processExtensionsFolder(updateDirectory, (path, description) -> {
                    
                    List<Path> oldExtensionFiles = extensionFiles.get(description.id());
                    if (oldExtensionFiles != null) {
                        for (Path oldExtensionFile : oldExtensionFiles) {
                            Files.delete(oldExtensionFile);
                        }
                    }

                    
                    Files.move(path, extensionsDirectory.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }, (path, e) -> {
                    logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.update.failed", path.getFileName()), e);
                });
            }

            
            this.processExtensionsFolder(extensionsDirectory, (path, description) -> {
                String id = description.id();
                descriptions.put(id, description);
                extensionPaths.put(id, path);

            }, (path, e) -> {
                logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_with_name", path.getFileName(), path.toAbsolutePath()), e);
            });

            
            Map<String, List<String>> loadOrderGraph = new HashMap<>();

            
            for (String id : descriptions.keySet()) {
                loadOrderGraph.putIfAbsent(id, new ArrayList<>());
            }

            for (GeyserExtensionDescription description : descriptions.values()) {
                for (Map.Entry<String, GeyserExtensionDescription.Dependency> dependency : description.dependencies().entrySet()) {
                    String from = null;
                    String to = null; 

                    
                    if (!descriptions.containsKey(dependency.getKey())) {
                        if (dependency.getValue().isRequired()) { 
                            
                            logger.error(
                                GeyserLocale.getLocaleStringLog(
                                    "geyser.extensions.load.failed_dependency_missing",
                                    description.id(),
                                    dependency.getKey()
                                )
                            );

                            descriptions.remove(description.id()); 
                        }

                        continue;
                    }

                    if (
                        !(description.humanApiVersion() >= 2 &&
                            description.majorApiVersion() >= 9 &&
                            description.minorApiVersion() >= 0)
                    ) {
                        logger.error(
                            GeyserLocale.getLocaleStringLog(
                                "geyser.extensions.load.failed_cannot_use_dependencies",
                                description.id(),
                                description.apiVersion()
                            )
                        );

                        descriptions.remove(description.id()); 

                        continue;
                    }

                    
                    switch (dependency.getValue().getLoad()) {
                        case BEFORE -> {
                            from = dependency.getKey();
                            to = description.id();
                        }
                        case AFTER -> {
                            from = description.id();
                            to = dependency.getKey();
                        }
                    }

                    loadOrderGraph.get(from).add(to);
                }
            }

            Set<String> visited = new HashSet<>();
            List<String> visiting = new ArrayList<>();
            List<String> loadOrder = new ArrayList<>();

            AtomicReference<Consumer<String>> sortMethod = new AtomicReference<>(); 
            sortMethod.set((node) -> {
                if (visiting.contains(node)) {
                    logger.error(
                        GeyserLocale.getLocaleStringLog(
                            "geyser.extensions.load.failed_cyclical_dependencies",
                            node,
                            visiting.get(visiting.indexOf(node) - 1)
                        )
                    );

                    visiting.remove(node);
                    return;
                }

                if (visited.contains(node)) return;

                visiting.add(node);
                for (String neighbor : loadOrderGraph.get(node)) {
                    sortMethod.get().accept(neighbor);
                }
                visiting.remove(node);
                visited.add(node);
                loadOrder.add(node);
            });

            for (String ext : descriptions.keySet()) {
                if (!visited.contains(ext)) {
                    
                    sortMethod.get().accept(ext);
                }
            }
            Collections.reverse(loadOrder); 

            
            for (String id : loadOrder) {
                
                Path path = extensionPaths.get(id);
                GeyserExtensionDescription description = descriptions.get(id);

                String name = description.name();
                if (extensions.containsKey(id) || extensionManager.extension(id) != null) {
                    logger.warning(GeyserLocale.getLocaleStringLog("geyser.extensions.load.duplicate", name, path.toString()));
                    return;
                }

                
                ApiVersion.Compatibility compatibility = GeyserApi.api().geyserApiVersion().supportsRequestedVersion(
                    description.humanApiVersion(),
                    description.majorApiVersion(),
                    description.minorApiVersion()
                );

                if (compatibility != ApiVersion.Compatibility.COMPATIBLE) {
                    
                    if (compatibility == ApiVersion.Compatibility.HUMAN_DIFFER && description.humanApiVersion() == 1) {
                        logger.warning("The extension %s requested the Base API version %s, which is deprecated in favor of specifying the Geyser API version. Please update the extension, or contact its developer."
                            .formatted(name, description.apiVersion()));
                    } else {
                        logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_version", name, description.apiVersion()));
                        return;
                    }
                }

                try {
                    GeyserExtensionContainer container = this.loadExtension(path, description);
                    extensions.put(id, path);
                    loadedExtensions.put(id, container);
                } catch (Throwable e) {
                    logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_with_name", path.getFileName(), path.toAbsolutePath()), e);
                }
            }

            
            for (GeyserExtensionContainer container : loadedExtensions.values()) {
                this.extensionContainers.put(container.extension(), container);
                this.register(container.extension(), extensionManager);
            }
        } catch (IOException ex) {
            logger.error("Unable to read extensions.", ex);
        }
    }

    
    private void processExtensionsFolder(Path directory, ThrowingBiConsumer<Path, GeyserExtensionDescription> accept, BiConsumer<Path, Throwable> reject) throws IOException {
        List<Path> extensionPaths = Files.list(directory).toList();
        Pattern extensionFilter = this.extensionFilter();
        extensionPaths.forEach(path -> {
            if (Files.isDirectory(path)) {
                return;
            }

            
            if (!extensionFilter.matcher(path.getFileName().toString()).matches()) {
                return;
            }

            try {
                
                GeyserExtensionDescription description = this.extensionDescription(path);

                accept.acceptThrows(path, description);
            } catch (Throwable e) {
                reject.accept(path, e);
            }
        });
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
