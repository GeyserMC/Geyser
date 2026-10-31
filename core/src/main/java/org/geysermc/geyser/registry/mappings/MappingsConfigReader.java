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

package org.geysermc.geyser.registry.mappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomMappingsEvent;
import org.geysermc.geyser.event.type.GeyserDefineCustomMappingsEventImpl;
import org.geysermc.geyser.util.WebUtils;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public final class MappingsConfigReader {
    /**
     * Mappings files from outside the custom_mappings folder, registered through {@link GeyserDefineCustomMappingsEvent}.
     */
    private static List<Path> registeredMappingsFiles = List.of();

    private MappingsConfigReader() {}

    /**
     * Collects the mappings files from the custom-mappings-urls config option and {@link GeyserDefineCustomMappingsEvent}.
     * Must be called before custom mappings are first read.
     */
    public static void defineCustomMappings(GeyserImpl geyser) {
        GeyserDefineCustomMappingsEventImpl event = new GeyserDefineCustomMappingsEventImpl();
        for (String url : geyser.config().advanced().customMappingsUrls()) {
            event.registerUrl(url);
        }
        geyser.eventBus().fire(event);
        registeredMappingsFiles = List.copyOf(event.mappingsFiles());
    }

    public static <K, V> void loadCustomMappingsFromJson(MappingsType<K, V> type, BiConsumer<K, V> consumer) {
        getCustomMappingsDirectoryAndEnsureItExists().ifPresent(customMappingsDirectory -> {
            Path[] mappingsFiles = getCustomMappingsFiles(customMappingsDirectory);
            for (Path mappingsFile : mappingsFiles) {
                readCustomMappings(type, mappingsFile, consumer);
            }
        });
        for (Path mappingsFile : registeredMappingsFiles) {
            readCustomMappings(type, mappingsFile, consumer);
        }
    }

    /**
     * Downloads a mappings file into the remote mappings cache. The cached copy is only replaced by a download
     * that is a mappings file, so it stays usable when the url is down or serves something else.
     *
     * @param url the url of the mappings file
     * @return the cached copy, or null if the download failed and there is no cached copy
     */
    public static @Nullable Path downloadCustomMappings(String url) {
        Path cacheDirectory = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("remote_mappings");
        Path cached = cacheDirectory.resolve(url.hashCode() + ".json");
        Path download = cacheDirectory.resolve(url.hashCode() + ".json.download");
        try {
            Files.createDirectories(cacheDirectory);
            HttpURLConnection con = (HttpURLConnection) URI.create(url).toURL().openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent", WebUtils.getUserAgent());

            int responseCode = con.getResponseCode();
            if (responseCode >= 400) {
                throw new IOException("Invalid response code " + responseCode);
            }

            try (InputStream in = con.getInputStream()) {
                Files.copy(in, download, StandardCopyOption.REPLACE_EXISTING);
            }
            if (getMappingsRoot(download) == null) {
                throw new IOException("Not a mappings file");
            }
            Files.move(download, cached, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return cached;
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(download);
            } catch (IOException ignored) {
            }

            if (Files.exists(cached)) {
                GeyserImpl.getInstance().getLogger().warning("Failed to download custom mappings from " + url + " (" + e.getMessage() + "), using the last downloaded copy");
                return cached;
            }
            GeyserImpl.getInstance().getLogger().error("Failed to download custom mappings from " + url, e);
            return null;
        }
    }

    public static Optional<Path> getCustomMappingsDirectoryAndEnsureItExists() {
        Path customMappingsDirectory = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("custom_mappings");
        if (!ensureMappingsDirectory(customMappingsDirectory)) {
            return Optional.empty();
        }
        return Optional.of(customMappingsDirectory);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean ensureMappingsDirectory(Path mappingsDirectory) {
        if (!Files.exists(mappingsDirectory)) {
            try {
                Files.createDirectories(mappingsDirectory);
                return true;
            } catch (IOException exception) {
                GeyserImpl.getInstance().getLogger().error("Failed to create mappings directory", exception);
                return false;
            }
        }
        return true;
    }

    private static Path[] getCustomMappingsFiles(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                .filter(child -> child.toString().endsWith(".json"))
                .toArray(Path[]::new);
        } catch (IOException exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to gather custom mappings files in directory " + directory, exception);
            return new Path[0];
        }
    }

    @VisibleForTesting
    public static <K, V> void readCustomMappings(MappingsType<K,V> type, Path file, BiConsumer<K, V> consumer) {
        JsonObject mappingsRoot = getMappingsRoot(file);
        if (mappingsRoot == null) {
            return;
        }

        JsonElement mappings = mappingsRoot.get(type.name());
        if (mappings == null) {
            return;
        } else if (!mappings.isJsonObject()) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file + " has an invalid " + type.name() + " mappings definition");
            return;
        }

        // getMappingsRoot makes sure format_version exists
        int formatVersion = mappingsRoot.get("format_version").getAsInt();
        MappingsReader<K, V> reader = type.readers().getOrDefault(formatVersion, null);
        if (reader == null) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file + " has an unsupported format version (" + formatVersion + ") for " + type.name() + " mappings");
            return;
        }
        reader.read(file, mappings.getAsJsonObject(), consumer);
    }

    private static @Nullable JsonObject getMappingsRoot(Path file) {
        JsonObject mappingsRoot;
        try (FileReader reader = new FileReader(file.toFile())) {
            mappingsRoot = (JsonObject) new JsonParser().parse(reader);
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().error("Failed to read custom mapping file: " + file, e);
            return null;
        }

        if (!mappingsRoot.has("format_version")) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file + " is missing the format version field!");
            return null;
        }

        return mappingsRoot;
    }
}
