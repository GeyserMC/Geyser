/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
 *
 */

package org.geysermc.platform.spigot.world;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Block;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import us.myles.ViaVersion.protocols.protocol1_13_1to1_13.Protocol1_13_1To1_13;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData;

@AllArgsConstructor
public class GeyserSpigotWorldManager extends WorldManager {

    private final boolean isLegacy;
    private final boolean use3dBiomes;
    // You need ViaVersion to connect to an older server with Geyser.
    // However, we still check for ViaVersion in case there's some other way that gets Geyser on a pre-1.13 Bukkit server
    private final boolean isViaVersion;

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        if (session.getPlayerEntity() == null) {
            return BlockTranslator.AIR;
        }
        if (isLegacy) {
            return getLegacyBlock(session, x, y, z, isViaVersion);
        }
        return BlockTranslator.getJavaIdBlockMap().get(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z).getBlockData().getAsString());
    }

    @SuppressWarnings("deprecation")
    public static int getLegacyBlock(GeyserSession session, int x, int y, int z, boolean isViaVersion) {
        if (isViaVersion) {
            Block block = Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z);
            // Black magic that gets the old block state ID
            int oldBlockId = (block.getType().getId() << 4) | (block.getData() & 0xF);
            // Convert block state from old version -> 1.13 -> 1.13.1 -> 1.14 -> 1.15 -> 1.16
            int thirteenBlockId = us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData.blockMappings.getNewId(oldBlockId);
            int thirteenPointOneBlockId = Protocol1_13_1To1_13.getNewBlockStateId(thirteenBlockId);
            int fourteenBlockId = us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData.blockStateMappings.getNewId(thirteenPointOneBlockId);
            int fifteenBlockId = us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData.blockStateMappings.getNewId(fourteenBlockId);
            return MappingData.blockStateMappings.getNewId(fifteenBlockId);
        } else {
            return BlockTranslator.AIR;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int[] getBiomeDataAt(GeyserSession session, int x, int z) {
        if (session.getPlayerEntity() == null) {
            return null;
        }
        int[] biomeData = new int[1024];
        ChunkSnapshot chunk = Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getChunkAt(x, z).getChunkSnapshot(true, true, true);
        for (int localX = 0; localX < 16; localX = localX + 4) {
            for (int localY = 0; localY < 255; localY = localY + 4) {
                for (int localZ = 0; localZ < 16; localZ = localZ + 4) {
                    // Index is based on wiki.vg's index requirements
                    final int i = ((localY >> 2) & 63) << 4 | ((localZ >> 2) & 3) << 2 | ((localX >> 2) & 3);
                    // 3D biomes didn't exist until 1.15
                    if (use3dBiomes) {
                        biomeData[i] = chunk.getBiome(localX, localY, localZ).ordinal();
                    } else {
                        biomeData[i] = chunk.getBiome(localX, localZ).ordinal();
                    }
                }
            }
        }
        return biomeData;
    }
}
