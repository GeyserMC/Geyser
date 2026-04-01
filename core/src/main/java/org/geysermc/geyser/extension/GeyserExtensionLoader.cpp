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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "lombok.RequiredArgsConstructor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.api.util.ApiVersion"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.event.ExtensionEventBus"
#include "org.geysermc.geyser.api.extension.Extension"
#include "org.geysermc.geyser.api.extension.ExtensionDescription"
#include "org.geysermc.geyser.api.extension.ExtensionLoader"
#include "org.geysermc.geyser.api.extension.ExtensionLogger"
#include "org.geysermc.geyser.api.extension.ExtensionManager"
#include "org.geysermc.geyser.api.extension.exception.InvalidDescriptionException"
#include "org.geysermc.geyser.api.extension.exception.InvalidExtensionException"
#include "org.geysermc.geyser.extension.event.GeyserExtensionEventBus"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.ThrowingBiConsumer"

#include "java.io.IOException"
#include "java.io.Reader"
#include "java.nio.file.FileSystem"
#include "java.nio.file.FileSystems"
#include "java.nio.file.Files"
#include "java.nio.file.NoSuchFileException"
#include "java.nio.file.Path"
#include "java.nio.file.StandardCopyOption"
#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.HashMap"
#include "java.util.HashSet"
#include "java.util.LinkedHashMap"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Set"
#include "java.util.concurrent.atomic.AtomicReference"
#include "java.util.function.BiConsumer"
#include "java.util.function.Consumer"
#include "java.util.regex.Pattern"

@RequiredArgsConstructor
public class GeyserExtensionLoader extends ExtensionLoader {
    private static final Pattern EXTENSION_FILTER = Pattern.compile("^.+\\.jar$");

    private final Object2ObjectMap<std::string, Class<?>> classes = new Object2ObjectOpenHashMap<>();
    private final Map<std::string, GeyserExtensionClassLoader> classLoaders = new HashMap<>();
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
        Map<std::string, std::string> environment = new HashMap<>();
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

    public Class<?> classByName(final std::string name) throws ClassNotFoundException{
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

    void setClass(std::string name, final Class<?> clazz) {
        this.classes.putIfAbsent(name, clazz);
    }

    override protected void loadAllExtensions(ExtensionManager extensionManager) {
        GeyserLogger logger = GeyserImpl.getInstance().getLogger();
        try {
            if (Files.notExists(extensionsDirectory)) {
                Files.createDirectory(extensionsDirectory);
            }

            Map<std::string, Path> extensions = new LinkedHashMap<>();
            Map<std::string, GeyserExtensionContainer> loadedExtensions = new LinkedHashMap<>();
            Map<std::string, GeyserExtensionDescription> descriptions = new LinkedHashMap<>();
            Map<std::string, Path> extensionPaths = new LinkedHashMap<>();

            Path updateDirectory = extensionsDirectory.resolve("update");
            if (Files.isDirectory(updateDirectory)) {

                Map<std::string, List<Path>> extensionFiles = new HashMap<>();
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
                std::string id = description.id();
                descriptions.put(id, description);
                extensionPaths.put(id, path);

            }, (path, e) -> {
                logger.error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_with_name", path.getFileName(), path.toAbsolutePath()), e);
            });


            Map<std::string, List<std::string>> loadOrderGraph = new HashMap<>();


            for (std::string id : descriptions.keySet()) {
                loadOrderGraph.putIfAbsent(id, new ArrayList<>());
            }

            for (GeyserExtensionDescription description : descriptions.values()) {
                for (Map.Entry<std::string, GeyserExtensionDescription.Dependency> dependency : description.dependencies().entrySet()) {
                    std::string from = null;
                    std::string to = null;


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

            Set<std::string> visited = new HashSet<>();
            List<std::string> visiting = new ArrayList<>();
            List<std::string> loadOrder = new ArrayList<>();

            AtomicReference<Consumer<std::string>> sortMethod = new AtomicReference<>();
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
                for (std::string neighbor : loadOrderGraph.get(node)) {
                    sortMethod.get().accept(neighbor);
                }
                visiting.remove(node);
                visited.add(node);
                loadOrder.add(node);
            });

            for (std::string ext : descriptions.keySet()) {
                if (!visited.contains(ext)) {

                    sortMethod.get().accept(ext);
                }
            }
            Collections.reverse(loadOrder);


            for (std::string id : loadOrder) {

                Path path = extensionPaths.get(id);
                GeyserExtensionDescription description = descriptions.get(id);

                std::string name = description.name();
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

    override protected bool isEnabled(Extension extension) {
        return this.extensionContainers.get(extension).enabled;
    }

    override protected void setEnabled(Extension extension, bool enabled) {
        this.extensionContainers.get(extension).enabled = enabled;
    }


    override protected Path dataFolder(Extension extension) {
        return this.extensionContainers.get(extension).dataFolder();
    }


    override protected ExtensionDescription description(Extension extension) {
        return this.extensionContainers.get(extension).description();
    }


    override protected ExtensionEventBus eventBus(Extension extension) {
        return this.extensionContainers.get(extension).eventBus();
    }


    override protected ExtensionLogger logger(Extension extension) {
        return this.extensionContainers.get(extension).logger();
    }
}
