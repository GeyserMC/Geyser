/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.world;

import com.github.steveice10.opennbt.tag.builtin.*;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.chunk.BlockStorage;
import org.geysermc.connector.network.translators.world.chunk.ChunkSection;
import org.geysermc.connector.registry.Registries;

import java.util.Arrays;

// Based off of ProtocolSupport's LegacyBiomeData.java:
// https://github.com/ProtocolSupport/ProtocolSupport/blob/b2cad35977f3fcb65bee57b9e14fc9c975f71d32/src/protocolsupport/protocol/typeremapper/legacy/LegacyBiomeData.java
// Array index formula by https://wiki.vg/Chunk_Format
public class BiomeTranslator {

    public static void loadServerBiomes(GeyserSession session, CompoundTag codec) {
        Int2IntMap biomeTranslations = session.getBiomeTranslations();
        biomeTranslations.clear();

        CompoundTag worldGen = codec.get("minecraft:worldgen/biome");
        ListTag serverBiomes = worldGen.get("value");

        for (Tag tag : serverBiomes) {
            CompoundTag biomeTag = (CompoundTag) tag;

            String javaIdentifier = ((StringTag) biomeTag.get("name")).getValue();
            int bedrockId = Registries.BIOME_IDENTIFIERS.get().getOrDefault(javaIdentifier, 0);
            int javaId = ((IntTag) biomeTag.get("id")).getValue();

            if (javaId != bedrockId) {
                // When we see the Java ID, we should instead apply the Bedrock ID
                biomeTranslations.put(javaId, bedrockId);
            }
        }
    }

    public static byte[] toBedrockBiome(GeyserSession session, int[] biomeData) {
        byte[] bedrockData = new byte[256];
        if (biomeData == null) {
            return bedrockData;
        }
        Int2IntMap biomeTranslations = session.getBiomeTranslations();

        for (int y = 0; y < 16; y += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int x = 0; x < 16; x += 4) {
                    int javaId = biomeData[((y >> 2) & 63) << 4 | ((z >> 2) & 3) << 2 | ((x >> 2) & 3)];
                    byte biomeId = (byte) biomeTranslations.getOrDefault(javaId, javaId);
                    int offset = ((z + (y / 4)) << 4) | x;
                    Arrays.fill(bedrockData, offset, offset + 4, biomeId);
                }
            }
        }
        return bedrockData;
    }

    public static BlockStorage toNewBedrockBiome(GeyserSession session, int[] biomeData, int ySection) {
        Int2IntMap biomeTranslations = session.getBiomeTranslations();
        // As of 1.17.10: the client expects the same format as a chunk but filled with biomes
        BlockStorage storage = new BlockStorage(0);

        int biomeY = ySection << 2;
        int javaOffsetY = biomeY << 4;
        // Each section of biome corresponding to a chunk section contains 4 * 4 * 4 entries
        for (int i = 0; i < 64; i++) {
            int javaId = biomeData[javaOffsetY | i];
            int x = i & 3;
            int y = (i >> 4) & 3;
            int z = (i >> 2) & 3;
            // Get the Bedrock biome ID override, or this ID if it's the same
            int biomeId = biomeTranslations.getOrDefault(javaId, javaId);
            int idx = storage.idFor(biomeId);
            // Convert biome coordinates into block coordinates
            // Bedrock expects a full 4096 blocks
            for (int blockX = x << 2; blockX < (x << 2) + 4; blockX++) {
                for (int blockZ = z << 2; blockZ < (z << 2) + 4; blockZ++) {
                    for (int blockY = y << 2; blockY < (y << 2) + 4; blockY++) {
                        storage.getBitArray().set(ChunkSection.blockPosition(blockX, blockY, blockZ), idx);
                    }
                }
            }
        }

        return storage;
    }
}
