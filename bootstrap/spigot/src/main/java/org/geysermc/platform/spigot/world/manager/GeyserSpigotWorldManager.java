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

package org.geysermc.platform.spigot.world.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.GeyserWorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.GameRule;
import org.geysermc.connector.utils.LanguageUtils;

import java.io.InputStream;

/**
 * The base world manager to use when there is no supported NMS revision
 */
public class GeyserSpigotWorldManager extends GeyserWorldManager {
    /**
     * The current client protocol version for ViaVersion usage.
     */
    protected static final int CLIENT_PROTOCOL_VERSION = MinecraftConstants.PROTOCOL_VERSION;

    /**
     * Whether the server is pre-1.16 and therefore does not support 3D biomes on an API level guaranteed.
     */
    private final boolean use3dBiomes;
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

    public GeyserSpigotWorldManager(boolean use3dBiomes) {
        this.use3dBiomes = use3dBiomes;

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
            JsonNode biome = biomes.get(enumBiome.toString());
            if (biome != null) {
                biomeToIdMap.put(enumBiome.ordinal(), biome.intValue());
            } else {
                GeyserConnector.getInstance().getLogger().debug("No biome mapping found for " + enumBiome.toString() +
                        ", defaulting to 0");
                biomeToIdMap.put(enumBiome.ordinal(), 0);
            }
        }
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return BlockTranslator.JAVA_AIR_ID;
        }
        World world = bukkitPlayer.getWorld();
        return BlockTranslator.getJavaIdBlockMap().getOrDefault(world.getBlockAt(x, y, z).getBlockData().getAsString(), BlockTranslator.JAVA_AIR_ID);
    }

    @Override
    public void getBlocksInSection(GeyserSession session, int x, int y, int z, Chunk chunk) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return;
        }
        World world = bukkitPlayer.getWorld();
        for (int blockY = 0; blockY < 16; blockY++) { // Cache-friendly iteration order
            for (int blockZ = 0; blockZ < 16; blockZ++) {
                for (int blockX = 0; blockX < 16; blockX++) {
                    Block block = world.getBlockAt((x << 4) + blockX, (y << 4) + blockY, (z << 4) + blockZ);
                    int id = BlockTranslator.getJavaIdBlockMap().getOrDefault(block.getBlockData().getAsString(), BlockTranslator.JAVA_AIR_ID);
                    chunk.set(blockX, blockY, blockZ, id);
                }
            }
        }
    }

    @Override
    public boolean hasMoreBlockDataThanChunkCache() {
        return true;
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

    /**
     * This must be set to true if we are pre-1.13, and {@link BlockData#getAsString() does not exist}.
     *
     * This should be set to true if we are post-1.13 but before the latest version, and we should convert the old block state id
     * to the current one.
     *
     * @return whether there is a difference between client block state and server block state that requires extra processing
     */
    public boolean isLegacy() {
        return false;
    }
}
