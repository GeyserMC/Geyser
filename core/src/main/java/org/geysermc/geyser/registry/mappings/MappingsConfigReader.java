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

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public final class MappingsConfigReader {

    private MappingsConfigReader() {}

    public static <K, V> void loadCustomMappingsFromJson(MappingsType<K, V> type, BiConsumer<K, V> consumer) {
        Path customMappingsDirectory = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("custom_mappings");

        if (!ensureMappingsDirectory(customMappingsDirectory)) {
            return;
        }

        Path[] mappingsFiles = getCustomMappingsFiles(customMappingsDirectory);
        for (Path mappingsFile : mappingsFiles) {
            readCustomMappings(type, mappingsFile, consumer);
        }
    }

    private static Path[] getCustomMappingsFiles(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                .filter(child -> child.toString().endsWith(".json"))
                .toArray(Path[]::new);
        } catch (IOException e) {
            return new Path[0];
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean ensureMappingsDirectory(Path mappingsDirectory) {
        if (!Files.exists(mappingsDirectory)) {
            try {
                Files.createDirectories(mappingsDirectory);
                return true;
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Failed to create mappings directory", e);
                return false;
            }
        }
        return true;
    }

    private static <K, V> void readCustomMappings(MappingsType<K,V> type, Path file, BiConsumer<K, V> consumer) {
        JsonObject mappingsRoot = getMappingsRoot(file);
        if (mappingsRoot == null) {
            return;
        }

        JsonElement mappings = mappingsRoot.get(type.name());
        if (mappings == null) {
            return;
        } else if (!mappings.isJsonObject()) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file + " has an invalid " + type.name() + " mappings definition");
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
