package org.geysermc.connector.network.translators;
import java.util.Arrays;

//Based off of ProtocolSupport's LegacyBiomeData.java https://github.com/ProtocolSupport/ProtocolSupport/blob/b2cad35977f3fcb65bee57b9e14fc9c975f71d32/src/protocolsupport/protocol/typeremapper/legacy/LegacyBiomeData.java
//Array index formula by https://wiki.vg/Chunk_Format

public class BiomeTranslator {

    public static byte[] toBedrockBiome(int[] biomeData) {
        byte[] bedrockData = new byte[256];
        if(biomeData == null) {
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

    protected static void fillArray(int z, int x, byte[] legacyBiomeData, int biomeId) {
        int offset = (z << 4) | x;
        Arrays.fill(legacyBiomeData, offset, offset + 4, (byte) biomeId);
    }

    protected static byte biomeID(int[] biomeData, int x, int z) {
        return (byte) biomeData[((z >> 2) & 3) << 2 | ((x >> 2) & 3)];
    }
}
