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

package org.geysermc.geyser.item.mappings;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.item.mappings.versions.MappingsReader;
import org.geysermc.geyser.item.mappings.versions.MappingsReader_v1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class MappingsConfigReader {
    private final Int2ObjectMap<MappingsReader> mappingReaders = new Int2ObjectOpenHashMap<>();
    private final Path customMappingsDirectory = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("custom_mappings");

    public MappingsConfigReader() {
        this.mappingReaders.put(1, new MappingsReader_v1());
    }

    public Path[] getCustomMappingsFiles() {
        try {
            return Files.walk(this.customMappingsDirectory)
                    .filter(child -> child.toString().endsWith(".json"))
                    .toArray(Path[]::new);
        } catch (IOException e) {
            return new Path[0];
        }
    }

    public void loadMappingsFromJson(BiConsumer<String, CustomItemData> consumer) {
        Path customMappingsDirectory = this.customMappingsDirectory;
        if (!Files.exists(customMappingsDirectory)) {
            try {
                Files.createDirectories(customMappingsDirectory);
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Failed to create custom mappings directory", e);
                return;
            }
        }

        Path[] mappingsFiles = this.getCustomMappingsFiles();
        for (Path mappingsFile : mappingsFiles) {
            this.readMappingsFromJson(mappingsFile, consumer);
        }
    }

    public void readMappingsFromJson(Path file, BiConsumer<String, CustomItemData> consumer) {
        JsonNode mappingsRoot;
        try {
            mappingsRoot = GeyserImpl.JSON_MAPPER.readTree(file.toFile());
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().error("Failed to read custom mapping file: " + file, e);
            return;
        }

        if (!mappingsRoot.has("format_version")) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file + " is missing the format version field!");
            return;
        }

        int formatVersion = mappingsRoot.get("format_version").asInt();
        if (!this.mappingReaders.containsKey(formatVersion)) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file + " has an unknown format version: " + formatVersion);
            return;
        }

        this.mappingReaders.get(formatVersion).readMappings(file, mappingsRoot, consumer);
    }
}
