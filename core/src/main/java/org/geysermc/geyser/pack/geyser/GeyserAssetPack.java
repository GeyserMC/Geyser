/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.pack.geyser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.configuration.GeyserConfigDirectory;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.loader.ResourcePackLoader;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

public record GeyserAssetPack(Manifest manifest, Path root) {
    public static final String EXTENSION = ".geyserpack";

    private static final String MANIFEST_PATH = "geyser_manifest.json";
    private static final String JSON_GLOB = "**.json";

    public void resourcePacks(Consumer<Path> consumer) {
        listDirectory("packs", ResourcePackLoader.PACK_GLOB, consumer);
    }

    public void mappings(Consumer<Path> consumer) {
        listDirectory("mappings", JSON_GLOB, consumer);
    }

    public void locales(Consumer<Path> consumer) {
        listDirectory("locales", JSON_GLOB, consumer);
    }

    private void listDirectory(String directory, String glob, Consumer<Path> consumer) {
        Path path = root.resolve(directory);
        if (!Files.isDirectory(path)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(path, glob)) {
            files.forEach(consumer);
        } catch (IOException exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to read files in " + path + "!", exception);
        }
    }

    public static List<GeyserAssetPack> load(GeyserImpl geyser) {
        return load(geyser.configDir(), geyser.getLogger());
    }

    public static List<GeyserAssetPack> load(GeyserConfigDirectory configDir, GeyserLogger logger) {
        Path cache = configDir.createIfAbsent(GeyserConfigDirectory.CACHE_GEYSER_PACKS);
        Path packsDirectory = configDir.createIfAbsent(GeyserConfigDirectory.PACKS);

        try (Stream<Path> packs = Files.list(packsDirectory)) {
            return packs.filter(GeyserAssetPack::isGeyserAssetPack) // Iterate over all files in the packs directory, filtering for Geyser packs
                .flatMap(pack -> tryLoad(pack, logger)) // Try to load all Geyser packs - read their manifest to get their UUID and version
                .sorted(Pending.VERSION_COMPARATOR) // Sort the packs by version - highest version first
                .distinct() // Drop packs with equal UUID, stream is sorted so highest version always wins
                .flatMap(pending -> pending.extract(cache, logger)) // Now extract the remaining packs into the cache folder
                .toList(); // Profit
        } catch (IOException exception) {
            logger.error("Failed to list packs directory when reading Geyser packs!" ,exception);
            return List.of();
        }
    }

    private static Stream<GeyserAssetPack.Pending> tryLoad(Path pack, GeyserLogger logger) {
        try (FileSystem system = FileSystems.newFileSystem(pack)) {
            Path manifestPath = system.getPath(MANIFEST_PATH);
            if (!Files.isRegularFile(manifestPath)) {
                throw new IOException("Missing " + MANIFEST_PATH);
            }

            try {
                return Stream.of(new GeyserAssetPack.Pending(Manifest.tryLoad(manifestPath), pack));
            } catch (IOException exception) {
                logger.error("Failed to load " + MANIFEST_PATH + " in Geyser pack " + pack + ": ", exception);
                return Stream.empty();
            }
        } catch (ProviderNotFoundException exception) {
            logger.error("Geyser pack " + pack + " is not a valid ZIP file!");
        } catch (IOException exception) {
            logger.error("Failed to load Geyser pack " + pack + ": ", exception);
        }

        return Stream.empty();
    }

    private static boolean isGeyserAssetPack(Path path) {
        return path.toString().endsWith(EXTENSION) && Files.isRegularFile(path);
    }

    private record Pending(Manifest manifest, Path source) {
        public static final Comparator<Pending> VERSION_COMPARATOR = Comparator.<Pending>comparingInt(pending -> pending.manifest.version).reversed();

        public Stream<GeyserAssetPack> extract(Path cache, GeyserLogger logger) {
            try (FileSystem system = FileSystems.newFileSystem(source)) {
                Path packRoot = system.getPath("/");
                Path exportedRoot = cache.resolve(manifest.uuid.toString());
                Path existingManifest = exportedRoot.resolve(MANIFEST_PATH);

                // If we already have an extracted copy of the manifest, read it and compare it
                if (Files.isRegularFile(existingManifest)) {
                    try {
                        Manifest existing = Manifest.tryLoad(existingManifest);
                        if (existing.uuidAndVersionEqual(manifest)) {
                            // If the existing manifest's UUID and version both match ours, assume the entire pack cache is correct

                            logger.debug("Existing manifest for " + source + " is up-to-date, using it");
                            return Stream.of(new GeyserAssetPack(existing, exportedRoot));
                        }
                        logger.debug("Existing manifest for " + source + " is outdated, overwriting it");
                    } catch (IOException exception) {
                        logger.debug("Failed parsing existing manifest for " + source + ", overwriting it");
                    }

                    // Manifest didn't match what we expected (pack source was updated, manifest was corrupted, etc.)
                    // Delete the old extracted files before extracting the new ones
                    try (Stream<Path> existingFiles = Files.walk(exportedRoot)) {
                        existingFiles.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException exception) {
                                    logger.error("Failed to delete existing extracted copy of " + source + ": ", exception);
                                }
                            });
                    }
                }

                Files.createDirectories(exportedRoot);

                try (Stream<Path> files = Files.walk(packRoot)) {
                    files.filter(Files::isRegularFile).forEach(path -> {
                        try {
                            Path destination = exportedRoot.resolve(packRoot.relativize(path).toString());
                            Files.createDirectories(destination.getParent());
                            Files.copy(path, destination);
                        } catch (IOException exception) {
                            logger.error("Failed to extract " + path + " from " + source + "!", exception);
                        }
                    });
                }

                return Stream.of(new GeyserAssetPack(manifest, exportedRoot));
            } catch (IOException exception) {
                logger.error("Failed to extract " + source + "!", exception);
                return Stream.empty();
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Pending otherPending && otherPending.manifest.uuid.equals(manifest.uuid);
        }

        @Override
        public int hashCode() {
            return manifest.uuid.hashCode();
        }
    }

    public record Manifest(UUID uuid, int version) {

        public boolean uuidAndVersionEqual(Manifest other) {
            return other.uuid.equals(uuid) && other.version == version;
        }

        public static Manifest tryLoad(Path manifest) throws IOException {
            JsonObject root = readMappingsRootSafely(manifest);
            try {
                UUID uuid = MappingsUtil.readOrThrow(root, "uuid", NodeReader.UUID, "reading pack manifest");
                int version = MappingsUtil.readOrThrow(root, "version", NodeReader.NON_NEGATIVE_INT, "reading pack manifest");
                return new Manifest(uuid, version);
            } catch (InvalidCustomMappingsFileException exception) {
                throw new IOException("Malformed manifest: ", exception);
            }
        }

        private static JsonObject readMappingsRootSafely(Path file) throws IOException {
            JsonElement root;
            try {
                root = new JsonParser().parse(Files.readString(file));
            } catch (JsonParseException exception) {
                throw new IOException("Malformed manifest: ", exception);
            }

            if (!root.isJsonObject()) {
                throw new IllegalStateException("Malformed manifest: is not a JSON object!");
            }

            JsonObject mappingsRoot = root.getAsJsonObject();
            JsonElement formatVersion = mappingsRoot.get("format_version");
            if (formatVersion == null || !formatVersion.isJsonPrimitive() || formatVersion.getAsInt() != 1) {
                throw new IllegalStateException("Manifest must specify \"format_version\" as a number, and it must be 1");
            }
            return mappingsRoot;
        }
    }
}
