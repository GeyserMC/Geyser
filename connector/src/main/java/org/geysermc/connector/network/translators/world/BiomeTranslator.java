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

import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.SingletonPalette;
import com.github.steveice10.opennbt.tag.builtin.*;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.chunk.BlockStorage;
import org.geysermc.connector.network.translators.world.chunk.GeyserChunkSection;
import org.geysermc.connector.network.translators.world.chunk.bitarray.SingletonBitArray;
import org.geysermc.connector.registry.Registries;
import org.geysermc.connector.utils.MathUtils;

// Based off of ProtocolSupport's LegacyBiomeData.java:
// https://github.com/ProtocolSupport/ProtocolSupport/blob/b2cad35977f3fcb65bee57b9e14fc9c975f71d32/src/protocolsupport/protocol/typeremapper/legacy/LegacyBiomeData.java
// Array index formula by https://wiki.vg/Chunk_Format
public class BiomeTranslator {

    public static void loadServerBiomes(GeyserSession session, CompoundTag codec) {
        Int2IntMap biomeTranslations = session.getBiomeTranslations();
        biomeTranslations.clear();

        CompoundTag worldGen = codec.get("minecraft:worldgen/biome");
        ListTag serverBiomes = worldGen.get("value");
        session.setBiomeGlobalPalette(MathUtils.getGlobalPaletteForSize(serverBiomes.size()));

        for (Tag tag : serverBiomes) {
            CompoundTag biomeTag = (CompoundTag) tag;

            String javaIdentifier = ((StringTag) biomeTag.get("name")).getValue();
            int bedrockId = Registries.BIOME_IDENTIFIERS.get().getOrDefault(javaIdentifier, -1);
            int javaId = ((IntTag) biomeTag.get("id")).getValue();

            if (bedrockId == -1) {
                // There is no matching Bedrock variation for this biome; let's set the closest match based on biome category
                String category = ((StringTag) ((CompoundTag) biomeTag.get("element")).get("category")).getValue();
                String replacementBiome = switch (category) {
                    case "extreme_hills" -> "minecraft:mountains";
                    case "icy" -> "minecraft:ice_spikes";
                    case "mesa" -> "minecraft:badlands";
                    case "mushroom" -> "minecraft:mushroom_fields";
                    case "nether" -> "minecraft:nether_wastes";
                    default -> "minecraft:ocean"; // Typically ID 0 so a good default
                    case "taiga", "jungle", "plains", "savanna", "the_end", "beach", "ocean", "desert", "river", "swamp" -> "minecraft:" + category;
                };
                bedrockId = Registries.BIOME_IDENTIFIERS.get().getInt(replacementBiome);
            }

            // When we see the Java ID, we should instead apply the Bedrock ID
            biomeTranslations.put(javaId, bedrockId);

            if (javaId == 0) {
                // Matches Java behavior when it sees an invalid biome - it just replaces it with ID 0
                biomeTranslations.defaultReturnValue(bedrockId);
            }
        }
    }

    public static BlockStorage toNewBedrockBiome(GeyserSession session, DataPalette biomeData) {
        Int2IntMap biomeTranslations = session.getBiomeTranslations();
        // As of 1.17.10: the client expects the same format as a chunk but filled with biomes
        // As of 1.18 this is the same as Java Edition

        if (biomeData.getPalette() instanceof SingletonPalette palette) {
            int biomeId = biomeTranslations.get(palette.idToState(0));
            return new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(biomeId));
        } else {
            BlockStorage storage = new BlockStorage(0);

            // Each section of biome corresponding to a chunk section contains 4 * 4 * 4 entries
            for (int i = 0; i < 64; i++) {
                int javaId = biomeData.getPalette().idToState(biomeData.getStorage().get(i));
                int x = i & 3;
                int y = (i >> 4) & 3;
                int z = (i >> 2) & 3;
                // Get the Bedrock biome ID override
                int biomeId = biomeTranslations.get(javaId);
                int idx = storage.idFor(biomeId);
                // Convert biome coordinates into block coordinates
                // Bedrock expects a full 4096 blocks
                for (int blockX = x << 2; blockX < (x << 2) + 4; blockX++) {
                    for (int blockZ = z << 2; blockZ < (z << 2) + 4; blockZ++) {
                        for (int blockY = y << 2; blockY < (y << 2) + 4; blockY++) {
                            storage.getBitArray().set(GeyserChunkSection.blockPosition(blockX, blockY, blockZ), idx);
                        }
                    }
                }
            }
            return storage;
        }
    }
}
