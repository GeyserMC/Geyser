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

package org.geysermc.platform.spigot.world.manager;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.geysermc.connector.network.session.GeyserSession;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.MappingData;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.storage.BlockStorage;

import java.util.List;

/**
 * Should be used when ViaVersion is present, no NMS adapter is being used, and we are pre-1.13.
 *
 * You need ViaVersion to connect to an older server with the Geyser-Spigot plugin.
 */
public class GeyserSpigot1_12WorldManager extends GeyserSpigotWorldManager {
    public GeyserSpigot1_12WorldManager() {
        super(false);
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        Player bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername());
        // Get block entity storage
        BlockStorage storage = Via.getManager().getConnection(bukkitPlayer.getUniqueId()).get(BlockStorage.class);
        return getLegacyBlock(storage, bukkitPlayer.getWorld(), x, y, z);
    }

    @SuppressWarnings("deprecation")
    public static int getLegacyBlock(BlockStorage storage, World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        // Black magic that gets the old block state ID
        int blockId = (block.getType().getId() << 4) | (block.getData() & 0xF);
        // Convert block state from old version (1.12.2) -> 1.13 -> 1.13.1 -> 1.14 -> 1.15 -> 1.16 -> 1.16.2
        blockId = ProtocolRegistry.getProtocol(Protocol1_13To1_12_2.class).getMappingData().getNewBlockId(blockId);
        List<Pair<Integer, Protocol>> protocolList = ProtocolRegistry.getProtocolPath(CLIENT_PROTOCOL_VERSION,
                ProtocolVersion.v1_13.getVersion());
        // Translate block entity differences - some information was stored in block tags and not block states
        if (storage.isWelcome(blockId)) { // No getOrDefault method
            BlockStorage.ReplacementData data = storage.get(new Position(x, (short) y, z));
            if (data != null && data.getReplacement() != -1) {
                blockId = data.getReplacement();
            }
        }
        for (int i = protocolList.size() - 1; i >= 0; i--) {
            MappingData mappingData = protocolList.get(i).getValue().getMappingData();
            if (mappingData != null) {
                blockId = mappingData.getNewBlockStateId(blockId);
            }
        }
        return blockId;
    }

    @Override
    public void getBlocksInSection(GeyserSession session, int x, int y, int z, Chunk chunk) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return;
        }
        World world = bukkitPlayer.getWorld();
        // Get block entity storage
        BlockStorage storage = Via.getManager().getConnection(bukkitPlayer.getUniqueId()).get(BlockStorage.class);
        for (int blockY = 0; blockY < 16; blockY++) { // Cache-friendly iteration order
            for (int blockZ = 0; blockZ < 16; blockZ++) {
                for (int blockX = 0; blockX < 16; blockX++) {
                    chunk.set(blockX, blockY, blockZ, getLegacyBlock(storage, world, (x << 4) + blockX, (y << 4) + blockY, (z << 4) + blockZ));
                }
            }
        }
    }

    @Override
    public boolean isLegacy() {
        return true;
    }
}
