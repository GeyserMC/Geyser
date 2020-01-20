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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.network.translators.block.entity.BlockEntityTranslator;
import org.geysermc.connector.world.chunk.ChunkPosition;
import org.geysermc.connector.world.chunk.ChunkSection;

public class ChunkUtils {

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();
        Chunk[] chunks = column.getChunks();
        chunkData.sections = new ChunkSection[chunks.length];

        CompoundTag[] blockEntities = column.getTileEntities();
        for (int chunkY = 0; chunkY < chunks.length; chunkY++) {
            chunkData.sections[chunkY] = new ChunkSection();
            Chunk chunk = chunks[chunkY];

            if (chunk == null || chunk.isEmpty())
                continue;

            ChunkSection section = chunkData.sections[chunkY];
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState blockState = chunk.get(x, y, z);
                        BlockEntry block = TranslatorsInit.getBlockTranslator().getBlockEntry(blockState);
                        if (block.getJavaIdentifier().contains("sign[")) {
                            Position pos = new ChunkPosition(column.getX(), column.getZ()).getBlock(x, (chunkY << 4) + y, z);
                            chunkData.signs.put(block.getJavaId(), TranslatorsInit.getBlockEntityTranslators().get("Sign").getDefaultBedrockTag(pos.getX(), pos.getY(), pos.getZ()));
                        } else {
                            section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z), block.getBedrockRuntimeId());
                        }

                        if (block.isWaterlogged()) {
                            BlockEntry water = TranslatorsInit.getBlockTranslator().getBlockEntry("minecraft:water[level=0]");
                            section.getBlockStorageArray()[1].setFullBlock(ChunkSection.blockPosition(x, y, z), water.getBedrockRuntimeId());
                        }
                    }
                }
            }
        }

        com.nukkitx.nbt.tag.CompoundTag[] bedrockBlockEntities = new com.nukkitx.nbt.tag.CompoundTag[blockEntities.length];
        for (int i = 0; i < bedrockBlockEntities.length; i++) {
            CompoundTag tag = blockEntities[i];
            String tagName;
            if (!tag.contains("id")) {
                GeyserLogger.DEFAULT.debug("Got tag with no id: " + tag.getValue());
                tagName = "Empty";
            } else {
                tagName = (String) tag.get("id").getValue();
            }

            String id = BlockEntityUtils.getBedrockBlockEntityId(tagName);
            BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(id);
            bedrockBlockEntities[i] = blockEntityTranslator.getBlockEntityTag(tag);
        }

        chunkData.blockEntities = bedrockBlockEntities;
        return chunkData;
    }

    public static void updateBlock(GeyserSession session, BlockState blockState, Position position) {
        BlockEntry blockEntry = TranslatorsInit.getBlockTranslator().getBlockEntry(blockState);
        Vector3i pos = Vector3i.from(position.getX(), position.getY(), position.getZ());

        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(pos);
        updateBlockPacket.setRuntimeId(blockEntry.getBedrockRuntimeId());
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.getUpstream().sendPacket(updateBlockPacket);

        UpdateBlockPacket waterPacket = new UpdateBlockPacket();
        waterPacket.setDataLayer(1);
        waterPacket.setBlockPosition(pos);
        if (blockEntry.isWaterlogged()) {
            BlockEntry water = TranslatorsInit.getBlockTranslator().getBlockEntry("minecraft:water[level=0]");
            waterPacket.setRuntimeId(water.getBedrockRuntimeId());
        } else {
            waterPacket.setRuntimeId(0);
        }
        session.getUpstream().sendPacket(waterPacket);
    }

    public static void sendEmptyChunks(GeyserSession session, Vector3i position, int radius, boolean forceUpdate) {
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        NetworkChunkPublisherUpdatePacket chunkPublisherUpdatePacket = new NetworkChunkPublisherUpdatePacket();
        chunkPublisherUpdatePacket.setPosition(position);
        chunkPublisherUpdatePacket.setRadius(radius + 1 << 4);
        session.getUpstream().sendPacket(chunkPublisherUpdatePacket);
        session.setLastChunkPosition(null);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                LevelChunkPacket data = new LevelChunkPacket();
                data.setChunkX(chunkX + x);
                data.setChunkZ(chunkZ + z);
                data.setSubChunksLength(0);
                data.setData(TranslatorsInit.EMPTY_LEVEL_CHUNK_DATA);
                data.setCachingEnabled(false);
                session.getUpstream().sendPacket(data);

                if (forceUpdate) {
                    Vector3i pos = Vector3i.from(chunkX + x << 4, 80, chunkZ + z << 4);
                    UpdateBlockPacket blockPacket = new UpdateBlockPacket();
                    blockPacket.setBlockPosition(pos);
                    blockPacket.setDataLayer(1);
                    blockPacket.setRuntimeId(1);
                    session.getUpstream().sendPacket(blockPacket);
                }
            }
        }
    }

    public static final class ChunkData {
        public ChunkSection[] sections;

        public byte[] biomes = new byte[256];
        public com.nukkitx.nbt.tag.CompoundTag[] blockEntities = new com.nukkitx.nbt.tag.CompoundTag[0];

        public Int2ObjectMap<com.nukkitx.nbt.tag.CompoundTag> signs = new Int2ObjectOpenHashMap<>();
    }
}
