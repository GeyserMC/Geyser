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
 */

package org.geysermc.platform.spigot.world;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.GeyserWorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.GameRule;
import org.geysermc.connector.utils.LanguageUtils;
import us.myles.ViaVersion.protocols.protocol1_13_1to1_13.Protocol1_13_1To1_13;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.data.MappingData;

import java.io.InputStream;

public class GeyserSpigotWorldManager extends GeyserWorldManager {

    private final boolean isLegacy;
    private final boolean use3dBiomes;
    /**
     * You need ViaVersion to connect to an older server with Geyser.
     * However, we still check for ViaVersion in case there's some other way that gets Geyser on a pre-1.13 Bukkit server
     */
    private final boolean isViaVersion;
    /**
     * Stores a list of {@link Biome} ordinal numbers to Minecraft biome numeric IDs.
     *
     * Working with the Biome enum in Spigot poses two problems:
     * 1: The Biome enum values change in both order and names over the years.
     * 2: There is no way to get the Minecraft biome ID from the name itself with Spigot.
     * To solve both of these problems, we store a JSON file of every Biome enum that has existed,
     * along with its 1.16 biome number.
     *
     * The key is the Spigot Biome ordinal; the value is the Minecraft Java biome numerical ID
     */
    private final Int2IntMap biomeToIdMap = new Int2IntOpenHashMap(Biome.values().length);

    public GeyserSpigotWorldManager(boolean isLegacy, boolean use3dBiomes, boolean isViaVersion) {
        this.isLegacy = isLegacy;
        this.use3dBiomes = use3dBiomes;
        this.isViaVersion = isViaVersion;

        // Load the values into the biome-to-ID map
        InputStream biomeStream = FileUtils.getResource("biomes.json");
        JsonNode biomes;
        try {
            biomes = GeyserConnector.JSON_MAPPER.readTree(biomeStream);
        } catch (Exception e) {
            throw new AssertionError(LanguageUtils.getLocaleStringLog("geyser.toolbox.fail.runtime_java"), e);
        }
        // Only load in the biomes that are present in this version of Minecraft
        for (Biome enumBiome : Biome.values()) {
            if (biomes.has(enumBiome.toString())) {
                biomeToIdMap.put(enumBiome.ordinal(), biomes.get(enumBiome.toString()).intValue());
            } else {
                GeyserConnector.getInstance().getLogger().debug("No biome mapping found for " + enumBiome.toString() +
                        ", defaulting to 0");
                biomeToIdMap.put(enumBiome.ordinal(), 0);
            }
        }
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        if (session.getPlayerEntity() == null) {
            return BlockTranslator.AIR;
        }
        if (Bukkit.getPlayer(session.getPlayerEntity().getUsername()) == null) {
            return BlockTranslator.AIR;
        }
        if (isLegacy) {
            return getLegacyBlock(session, x, y, z, isViaVersion);
        }
        //TODO possibly: detect server version for all versions and use ViaVersion for block state mappings like below
        return BlockTranslator.getJavaIdBlockMap().getOrDefault(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z).getBlockData().getAsString(), 0);
    }

    @SuppressWarnings("deprecation")
    public static int getLegacyBlock(GeyserSession session, int x, int y, int z, boolean isViaVersion) {
        if (isViaVersion) {
            Block block = Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z);
            // Black magic that gets the old block state ID
            int oldBlockId = (block.getType().getId() << 4) | (block.getData() & 0xF);
            // Convert block state from old version -> 1.13 -> 1.13.1 -> 1.14 -> 1.15 -> 1.16 -> 1.16.2
            int thirteenBlockId = us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData.blockMappings.getNewId(oldBlockId);
            int thirteenPointOneBlockId = Protocol1_13_1To1_13.getNewBlockStateId(thirteenBlockId);
            int fourteenBlockId = us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData.blockStateMappings.getNewId(thirteenPointOneBlockId);
            int fifteenBlockId = us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData.blockStateMappings.getNewId(fourteenBlockId);
            int sixteenBlockId = us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData.blockStateMappings.getNewId(fifteenBlockId);
            return MappingData.blockStateMappings.getNewId(sixteenBlockId);
        } else {
            return BlockTranslator.AIR;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int[] getBiomeDataAt(GeyserSession session, int x, int z) {
        if (session.getPlayerEntity() == null) {
            return new int[1024];
        }
        int[] biomeData = new int[1024];
        World world = Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld();
        int chunkX = x << 4;
        int chunkZ = z << 4;
        int chunkXmax = chunkX + 16;
        int chunkZmax = chunkZ + 16;
        // 3D biomes didn't exist until 1.15
        if (use3dBiomes) {
            for (int localX = chunkX; localX < chunkXmax; localX += 4) {
                for (int localY = 0; localY < 255; localY += + 4) {
                    for (int localZ = chunkZ; localZ < chunkZmax; localZ += 4) {
                        // Index is based on wiki.vg's index requirements
                        final int i = ((localY >> 2) & 63) << 4 | ((localZ >> 2) & 3) << 2 | ((localX >> 2) & 3);
                        biomeData[i] = biomeToIdMap.getOrDefault(world.getBiome(localX, localY, localZ).ordinal(), 0);
                    }
                }
            }
        } else {
            // Looks like the same code, but we're not checking the Y coordinate here
            for (int localX = chunkX; localX < chunkXmax; localX += 4) {
                for (int localY = 0; localY < 255; localY += + 4) {
                    for (int localZ = chunkZ; localZ < chunkZmax; localZ += 4) {
                        // Index is based on wiki.vg's index requirements
                        final int i = ((localY >> 2) & 63) << 4 | ((localZ >> 2) & 3) << 2 | ((localX >> 2) & 3);
                        biomeData[i] = biomeToIdMap.getOrDefault(world.getBiome(localX, localZ).ordinal(), 0);
                    }
                }
            }
        }
        return biomeData;
    }

    public Boolean getGameRuleBool(GeyserSession session, GameRule gameRule) {
        return Boolean.parseBoolean(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getGameRuleValue(gameRule.getJavaID()));
    }

    @Override
    public int getGameRuleInt(GeyserSession session, GameRule gameRule) {
        return Integer.parseInt(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getGameRuleValue(gameRule.getJavaID()));
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        return Bukkit.getPlayer(session.getPlayerEntity().getUsername()).hasPermission(permission);
    }
}
