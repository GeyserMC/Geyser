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

package org.geysermc.geyser.translator.protocol.java.level;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.BlockChangeEntry.MessageType;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateSubChunkBlocksPacket;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.BitSet;
import java.util.Objects;

@Translator(packet = ClientboundSectionBlocksUpdatePacket.class)
public class JavaSectionBlocksUpdateTranslator extends PacketTranslator<ClientboundSectionBlocksUpdatePacket> {

    private static final int FLAG_ALL = 1 << UpdateBlockPacket.Flag.NEIGHBORS.ordinal() | 1 << UpdateBlockPacket.Flag.NETWORK.ordinal();

    @Override
    public void translate(GeyserSession session, ClientboundSectionBlocksUpdatePacket packet) {
        DataPalette palette = null;
        if (session.getChunkCache().isCache()) {
            palette = session.getChunkCache().getChunkSection(packet.getChunkX(), packet.getChunkY(), packet.getChunkZ(), true);
            if (palette == null) {
                return;
            }
        }

        Vector3i clientBreakPos = session.getBlockBreakHandler().getCurrentBlockPos();
        BitSet waterlogged = BlockRegistries.WATERLOGGED.get();

        UpdateSubChunkBlocksPacket updateSubChunkBlocksPacket = new UpdateSubChunkBlocksPacket();

        for (BlockChangeEntry entry : packet.getEntries()) {
            session.getWorldCache().removePrediction(entry.getPosition());

            // Hack to avoid looking up blockstates for the currently broken position each tick
            if (clientBreakPos != null && Objects.equals(clientBreakPos, entry.getPosition())) {
                session.getBlockBreakHandler().setUpdatedServerBlockStateId(entry.getBlock());
            }

            int oldBlock = palette != null
                ? palette.get(entry.getPosition().getX() & 0xF, entry.getPosition().getY() & 0xF, entry.getPosition().getZ() & 0xF)
                : session.getGeyser().getWorldManager().getBlockAt(session, entry.getPosition());
            if (entry.getBlock() == oldBlock) {
                // Skip unchanged blocks which may occur with older versions of Minecraft
                continue;
            }

            if (palette != null) {
                palette.set(entry.getPosition().getX() & 0xF, entry.getPosition().getY() & 0xF, entry.getPosition().getZ() & 0xF, entry.getBlock());
            }

            BlockState blockState = BlockState.of(entry.getBlock());
            if (blockState.is(Blocks.AIR)) {
                ItemFrameEntity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, entry.getPosition());
                if (itemFrameEntity != null) { // Item frame is still present and no block overrides that; refresh it
                    itemFrameEntity.updateBlock(true);
                    continue;
                }
            }

            // Some block may have special handling, keep it that way
            if (!(blockState.block().getClass().equals(Block.class))) {
                blockState.block().updateBlock(session, blockState, entry.getPosition());
                continue;
            }

            // Skull is gone
            session.getSkullCache().removeSkull(entry.getPosition());

            updateSubChunkBlocksPacket.getStandardBlocks().add(new org.cloudburstmc.protocol.bedrock.data.BlockChangeEntry(
                entry.getPosition(),
                session.getBlockMappings().getBedrockBlock(blockState),
                FLAG_ALL,
                -1,
                MessageType.NONE
            ));

            boolean isWaterlogged = waterlogged.get(entry.getBlock());
            if (waterlogged.get(oldBlock) != isWaterlogged) {
                updateSubChunkBlocksPacket.getExtraBlocks().add(new org.cloudburstmc.protocol.bedrock.data.BlockChangeEntry(
                    entry.getPosition(),
                    isWaterlogged ? session.getBlockMappings().getBedrockWater() : session.getBlockMappings().getBedrockAir(),
                    0,
                    -1,
                    MessageType.NONE
                ));
            }
        }

        if (!updateSubChunkBlocksPacket.getStandardBlocks().isEmpty()) {
            session.sendUpstreamPacket(updateSubChunkBlocksPacket);
        }
    }
}
