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

package org.geysermc.geyser.registry.populator.conversion;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.geysermc.geyser.network.GameProtocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters biome definitions unknown to legacy Bedrock clients.
 * Sending unsupported biome ids can crash the client during join.
 */
public final class LegacyBiomeFallbacks {

    private LegacyBiomeFallbacks() {
    }

    public static BiomeDefinitions filterDefinitions(BiomeDefinitions definitions, int protocolVersion) {
        Map<String, BiomeDefinitionData> source = definitions.getDefinitions();
        Map<String, BiomeDefinitionData> filtered = new LinkedHashMap<>(source.size());
        for (Map.Entry<String, BiomeDefinitionData> entry : source.entrySet()) {
            if (clientSupportsBiome(entry.getKey(), protocolVersion)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        if (filtered.size() == source.size()) {
            return definitions;
        }
        return new BiomeDefinitions(filtered);
    }

    /**
     * Filter the pre-1.21.80 NBT biome list ({@code biome_definitions.dat}).
     */
    public static NbtMap filterNbtDefinitions(NbtMap definitions, int protocolVersion) {
        NbtMapBuilder builder = NbtMap.builder();
        boolean changed = false;
        for (Map.Entry<String, Object> entry : definitions.entrySet()) {
            if (clientSupportsBiome(entry.getKey(), protocolVersion)) {
                builder.put(entry.getKey(), entry.getValue());
            } else {
                changed = true;
            }
        }
        return changed ? builder.build() : definitions;
    }

    public static boolean clientSupportsBiome(String biomeIdentifier, int protocolVersion) {
        if (biomeIdentifier == null || biomeIdentifier.isEmpty()) {
            return true;
        }
        String path = biomeIdentifier.contains(":")
            ? biomeIdentifier.substring(biomeIdentifier.indexOf(':') + 1)
            : biomeIdentifier;
        return switch (path) {
            case "pale_garden" -> GameProtocol.is1_21_50orHigher(protocolVersion);
            case "sulfur_caves" -> GameProtocol.is26_10orHigher(protocolVersion);
            default -> true;
        };
    }
}
