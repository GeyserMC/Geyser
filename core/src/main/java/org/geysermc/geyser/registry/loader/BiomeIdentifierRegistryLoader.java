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

package org.geysermc.geyser.registry.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.geyser.GeyserImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class BiomeIdentifierRegistryLoader implements RegistryLoader<String, Object2IntMap<String>> {

    @Override
    public Object2IntMap<String> load(String input) {
        // As of Bedrock Edition 1.17.10 with the experimental toggle, any unmapped biome identifier sent to the client
        // crashes the client. Therefore, we need to have a list of all valid Bedrock biome IDs with which we can use from.
        // The server sends the corresponding Java network IDs, so we don't need to worry about that now.

        // Reference variable for Jackson to read off of
        TypeReference<Map<String, BiomeEntry>> biomeEntriesType = new TypeReference<>() { };
        Map<String, BiomeEntry> biomeEntries;

        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResource("mappings/biomes.json")) {
            biomeEntries = GeyserImpl.JSON_MAPPER.readValue(stream, biomeEntriesType);
        } catch (IOException e) {
            throw new AssertionError("Unable to load Bedrock runtime biomes", e);
        }

        Object2IntMap<String> biomes = new Object2IntOpenHashMap<>();
        for (Map.Entry<String, BiomeEntry> biome : biomeEntries.entrySet()) {
            // Java Edition identifier -> Bedrock integer ID
            biomes.put(biome.getKey(), biome.getValue().bedrockId);
        }

        return biomes;
    }

    private static class BiomeEntry {
        /**
         * The Bedrock network ID for this biome.
         */
        @JsonProperty("bedrock_id")
        private int bedrockId;
    }
}
