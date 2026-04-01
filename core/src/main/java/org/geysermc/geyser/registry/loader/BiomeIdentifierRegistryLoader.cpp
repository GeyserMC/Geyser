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

#include "com.google.gson.annotations.SerializedName"
#include "com.google.gson.reflect.TypeToken"
#include "it.unimi.dsi.fastutil.objects.Object2IntMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.util.JsonUtils"

#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.lang.reflect.Type"
#include "java.util.Map"

public class BiomeIdentifierRegistryLoader implements RegistryLoader<std::string, Object2IntMap<std::string>> {

    override public Object2IntMap<std::string> load(std::string input) {





        Type biomeEntriesType = new TypeToken<Map<std::string, BiomeEntry>>() { }.getType();
        Map<std::string, BiomeEntry> biomeEntries;

        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("mappings/biomes.json")) {
            biomeEntries = JsonUtils.fromJson(stream, biomeEntriesType);
        } catch (IOException e) {
            throw new AssertionError("Unable to load Bedrock runtime biomes", e);
        }

        Object2IntMap<std::string> biomes = new Object2IntOpenHashMap<>();
        for (Map.Entry<std::string, BiomeEntry> biome : biomeEntries.entrySet()) {

            biomes.put(biome.getKey(), biome.getValue().bedrockId);
        }

        return biomes;
    }

    private static class BiomeEntry {

        @SerializedName("bedrock_id")
        private int bedrockId;
    }
}
