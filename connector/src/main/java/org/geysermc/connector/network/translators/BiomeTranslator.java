/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators;

import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;

import java.io.InputStream;
import java.util.Arrays;

// Based off of ProtocolSupport's LegacyBiomeData.java:
// https://github.com/ProtocolSupport/ProtocolSupport/blob/b2cad35977f3fcb65bee57b9e14fc9c975f71d32/src/protocolsupport/protocol/typeremapper/legacy/LegacyBiomeData.java
// Array index formula by https://wiki.vg/Chunk_Format
public class BiomeTranslator {

    public static final CompoundTag BIOMES;

    private BiomeTranslator() {
    }

    public static void init() {
        // no-op
    }

    static {
        /* Load biomes */
        InputStream stream = FileUtils.getResource("bedrock/biome_definitions.dat");

        CompoundTag biomesTag;

        try (NBTInputStream biomenbtInputStream = NbtUtils.createNetworkReader(stream)){
            biomesTag = (CompoundTag) biomenbtInputStream.readTag();
            BIOMES = biomesTag;
        } catch (Exception ex) {
            GeyserConnector.getInstance().getLogger().warning("Failed to get biomes from biome definitions, is there something wrong with the file?");
            throw new AssertionError(ex);
        }
    }

    public static byte[] toBedrockBiome(int[] biomeData) {
        byte[] bedrockData = new byte[256];
        if (biomeData == null) {
            return bedrockData;
        }

        for (int z = 0; z < 16; z += 4) {
            for (int x = 0; x < 16; x += 4) {
                byte biomeId = biomeID(biomeData, x, z);
                fillArray(z, x, bedrockData, biomeId);
                fillArray(z + 1, x, bedrockData, biomeId);
                fillArray(z + 2, x, bedrockData, biomeId);
                fillArray(z + 3, x, bedrockData, biomeId);
            }
        }
        return bedrockData;
    }

    private static void fillArray(int z, int x, byte[] legacyBiomeData, int biomeId) {
        int offset = (z << 4) | x;
        Arrays.fill(legacyBiomeData, offset, offset + 4, (byte) biomeId);
    }

    private static byte biomeID(int[] biomeData, int x, int z) {
        return (byte) biomeData[((z >> 2) & 3) << 2 | ((x >> 2) & 3)];
    }
}
