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

package org.geysermc.connector.utils;

import org.geysermc.connector.network.translators.world.chunk.BlockStorage;

import java.util.Arrays;

// Based off of ProtocolSupport's LegacyBiomeData.java:
// https://github.com/ProtocolSupport/ProtocolSupport/blob/b2cad35977f3fcb65bee57b9e14fc9c975f71d32/src/protocolsupport/protocol/typeremapper/legacy/LegacyBiomeData.java
// Array index formula by https://wiki.vg/Chunk_Format
public class BiomeUtils {
    public static byte[] toBedrockBiome(int[] biomeData) {
        byte[] bedrockData = new byte[256];
        if (biomeData == null) {
            return bedrockData;
        }

        for (int y = 0; y < 16; y += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int x = 0; x < 16; x += 4) {
                    byte biomeId = (byte) biomeID(biomeData, x, y, z);
                    int offset = ((z + (y / 4)) << 4) | x;
                    Arrays.fill(bedrockData, offset, offset + 4, biomeId);
                }
            }
        }
        return bedrockData;
    }

    public static BlockStorage toNewBedrockBiome(int[] biomeData, int ySection) {
        BlockStorage storage = new BlockStorage(0);
        int blockY = ySection << 4;
        int i = 0;
        // Iterate over biomes like a chunk, grab the biome from Java, and add it to Bedrock's biome palette
        // Might be able to be optimized by iterating over Java's biome data section?? Unsure.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = blockY; y < (blockY + 16); y++) {
                    int biomeId = biomeID(biomeData, x, y, z);
                    storage.setFullBlock(i, biomeId);
                    i++;
                }
            }
        }
        return storage;
    }

    private static int biomeID(int[] biomeData, int x, int y, int z) {
        int biomeId = biomeData[((y >> 2) & 63) << 4 | ((z >> 2) & 3) << 2 | ((x >> 2) & 3)];
        if (biomeId == 0) {
            biomeId = 42; // Ocean
        } else if (biomeId >= 40 && biomeId <= 43) { // Java has multiple End dimensions that Bedrock doesn't recognize
            biomeId = 9;
        } else if (biomeId >= 170) { // Nether biomes. Dunno why it's like this :microjang:
            biomeId += 8;
        }
        return biomeId;
    }
}
