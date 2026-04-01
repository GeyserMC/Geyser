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

package org.geysermc.geyser.translator.level;

import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.BitStorage;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.GlobalPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.Palette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.SingletonPalette;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.geysermc.geyser.level.chunk.BlockStorage;
import org.geysermc.geyser.level.chunk.bitarray.BitArray;
import org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;


public class BiomeTranslator {

    public static int loadServerBiome(RegistryEntryContext entry) {
        String javaIdentifier = entry.id().asString();
        return Registries.BIOME_IDENTIFIERS.get().getOrDefault(javaIdentifier, 0);
    }

    public static BlockStorage toNewBedrockBiome(GeyserSession session, DataPalette biomeData) {
        JavaRegistry<Integer> biomeTranslations = session.getRegistryCache().registry(JavaRegistries.BIOME);
        
        

        Palette palette = biomeData.getPalette();
        if (palette instanceof SingletonPalette) {
            int biomeId = biomeTranslations.byId(palette.idToState(0));
            return new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(biomeId));
        } else {
            BlockStorage storage;
            if (!(palette instanceof GlobalPalette)) {
                
                BitStorage bitStorage = biomeData.getStorage();
                int size = palette.size();
                BitArray bitArray = BitArrayVersion.forBitsCeil(bitStorage.getBitsPerEntry())
                        .createArray(BlockStorage.SIZE);

                IntList bedrockPalette = new IntArrayList(size);

                for (int i = 0; i < size; i++) {
                    int javaId = palette.idToState(i);
                    bedrockPalette.add(biomeTranslations.byId(javaId).intValue());
                }

                
                for (int i = 0; i < 64; i++) {
                    int idx = bitStorage.get(i);
                    int x = i & 3;
                    int y = (i >> 4) & 3;
                    int z = (i >> 2) & 3;
                    
                    
                    multiplyIdToStorage(bitArray, idx, x, y, z);
                }

                storage = new BlockStorage(bitArray, bedrockPalette);
            } else {
                storage = new BlockStorage(0);

                
                for (int i = 0; i < 64; i++) {
                    int javaId = palette.idToState(biomeData.getStorage().get(i));
                    int x = i & 3;
                    int y = (i >> 4) & 3;
                    int z = (i >> 2) & 3;
                    
                    int biomeId = biomeTranslations.byId(javaId);
                    int idx = storage.idFor(biomeId);
                    
                    
                    
                    
                    multiplyIdToStorage(storage.getBitArray(), idx, x, y, z);
                }
            }
            return storage;
        }
    }

    private static void multiplyIdToStorage(final BitArray bitArray, final int idx, final int x, final int y, final int z) {
        for (int blockX = x << 2; blockX < (x << 2) + 4; blockX++) {
            for (int blockZ = z << 2; blockZ < (z << 2) + 4; blockZ++) {
                for (int blockY = y << 2; blockY < (y << 2) + 4; blockY++) {
                    bitArray.set((blockX << 8) | (blockZ << 4) | blockY, idx);
                }
            }
        }
    }
}
