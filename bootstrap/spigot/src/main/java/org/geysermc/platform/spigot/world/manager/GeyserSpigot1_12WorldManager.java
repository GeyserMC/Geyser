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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.storage.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

import java.util.List;

/**
 * Should be used when ViaVersion is present, no NMS adapter is being used, and we are pre-1.13.
 *
 * You need ViaVersion to connect to an older server with the Geyser-Spigot plugin.
 */
public class GeyserSpigot1_12WorldManager extends GeyserSpigotWorldManager {
    /**
     * Specific mapping data for 1.12 to 1.13. Used to convert the 1.12 block into the 1.13 block state.
     * (Block IDs did not change between server versions until 1.13 and after)
     */
    private final MappingData mappingData1_12to1_13;

    /**
     * The list of all protocols from the client's version to 1.13.
     */
    private final List<ProtocolPathEntry> protocolList;

    public GeyserSpigot1_12WorldManager(Plugin plugin) {
        super(plugin);
        this.mappingData1_12to1_13 = Via.getManager().getProtocolManager().getProtocol(Protocol1_13To1_12_2.class).getMappingData();
        this.protocolList = Via.getManager().getProtocolManager().getProtocolPath(CLIENT_PROTOCOL_VERSION,
                ProtocolVersion.v1_13.getVersion());
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        Player player = Bukkit.getPlayer(session.getPlayerEntity().getUsername());
        if (player == null) {
            return BlockTranslator.JAVA_AIR_ID;
        }
        if (!player.getWorld().isChunkLoaded(x >> 4, z >> 4)) {
            // Prevent nasty async errors if a player is loading in
            return BlockTranslator.JAVA_AIR_ID;
        }
        // Get block entity storage
        BlockStorage storage = Via.getManager().getConnectionManager().getConnectedClient(player.getUniqueId()).get(BlockStorage.class);
        Block block = player.getWorld().getBlockAt(x, y, z);
        // Black magic that gets the old block state ID
        int blockId = (block.getType().getId() << 4) | (block.getData() & 0xF);
        return getLegacyBlock(storage, blockId, x, y, z);
    }

    /**
     *
     * @param storage ViaVersion's block entity storage (used to fix block entity state differences)
     * @param blockId the pre-1.13 block id
     * @param x X coordinate of block
     * @param y Y coordinate of block
     * @param z Z coordinate of block
     * @return the block state updated to the latest Minecraft version
     */
    @SuppressWarnings("deprecation")
    public int getLegacyBlock(BlockStorage storage, int blockId, int x, int y, int z) {
        // Convert block state from old version (1.12.2) -> 1.13 -> 1.13.1 -> 1.14 -> 1.15 -> 1.16 -> 1.16.2
        blockId = mappingData1_12to1_13.getNewBlockId(blockId);
        // Translate block entity differences - some information was stored in block tags and not block states
        if (storage.isWelcome(blockId)) { // No getOrDefault method
            BlockStorage.ReplacementData data = storage.get(new Position(x, (short) y, z));
            if (data != null && data.getReplacement() != -1) {
                blockId = data.getReplacement();
            }
        }
        for (int i = protocolList.size() - 1; i >= 0; i--) {
            MappingData mappingData = protocolList.get(i).getProtocol().getMappingData();
            if (mappingData != null) {
                blockId = mappingData.getNewBlockStateId(blockId);
            }
        }
        return blockId;
    }

    @Override
    public boolean isLegacy() {
        return true;
    }
}
