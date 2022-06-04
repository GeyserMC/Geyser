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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.item.GeyserCustomItemManager;
import org.geysermc.geyser.item.mappings.versions.MappingsReader;
import org.geysermc.geyser.item.mappings.versions.MappingsReader_v1_0_0;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class MappingsConfigReader {
    private static final Map<String, MappingsReader> MAPPING_READERS = new HashMap<>();

    public static void init(GeyserCustomItemManager customItemManager) {
        MAPPING_READERS.put("1.0.0", new MappingsReader_v1_0_0(customItemManager));
    }

    public static Path getCustomMappingsDirectory() {
        return GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("custom_mappings");
    }

    public static Path[] getCustomMappingsFiles() {
        try {
            return Files.walk(getCustomMappingsDirectory())
                    .filter(child -> child.toString().endsWith(".json"))
                    .toArray(Path[]::new);
        } catch (IOException e) {
            return new Path[0];
        }
    }

    public static void readMappingsFromJson(Path file, BiConsumer<String, CustomItemData> consumer) {
        JsonNode mappingsRoot;
        try {
            mappingsRoot = GeyserImpl.JSON_MAPPER.readTree(file.toFile());
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().error("Failed to read custom mapping file: " + file.toString(), e);
            return;
        }

        if (!mappingsRoot.has("format_version")) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file.toString() + " is missing the format version field!");
            return;
        }

        String formatVersion = mappingsRoot.get("format_version").asText();
        if (!MAPPING_READERS.containsKey(formatVersion)) {
            GeyserImpl.getInstance().getLogger().error("Mappings file " + file.toString() + " has an unknown format version: " + formatVersion);
            return;
        }

        MAPPING_READERS.get(formatVersion).readMappings(file, mappingsRoot, consumer);
    }
}
