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
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.entity.BedrockOnlyBlockEntityTranslator;
import org.geysermc.connector.network.translators.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.block.entity.BedBlockEntityTranslator;
import org.geysermc.connector.world.chunk.ChunkSection;
import org.reflections.Reflections;

import java.util.Set;

import static org.geysermc.connector.network.translators.block.BlockTranslator.BEDROCK_WATER_ID;

public class ChunkUtils {

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();
        Chunk[] chunks = column.getChunks();
        chunkData.sections = new ChunkSection[chunks.length];

        CompoundTag[] blockEntities = column.getTileEntities();

        Reflections ref = new Reflections("org.geysermc.connector.network.translators.block.entity");

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
                        int id = BlockTranslator.getBedrockBlockId(blockState);

//                            Class<?> result = clazz.getAnnotation(LoadLater.class).result();
//
//                        if (BlockState.class.isAssignableFrom(result)) {
//                            Registry.registerJava(targetPacket, translator);
//
//                        } else if (com.nukkitx.nbt.tag.CompoundTag.class.isAssignableFrom(result)) {
//                            Registry.registerBedrock(targetPacket, translator);
//                        if (BlockTranslator.getBlockEntityString(blockState) != null) {
//                            System.out.println(BlockEntityUtils.getBedrockBlockEntityId(BlockTranslator.getBlockEntityString(blockState)));
//                            System.out.println(BlockEntityUtils.getBlockEntityTranslator(BlockEntityUtils.getBedrockBlockEntityId(BlockTranslator.getBlockEntityString(blockState))).getClass());
//                            if (ref.getTypesAnnotatedWith(LoadLater.class).contains(BlockEntityUtils.getBlockEntityTranslator(BlockEntityUtils.getBedrockBlockEntityId(BlockTranslator.getBlockEntityString(blockState))).getClass())) {
//                                System.out.println("Contained");
//                                BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(BlockTranslator.getBlockEntityString(blockState));
//                                Position pos = new ChunkPosition(column.getX(), column.getZ()).getBlock(x, (chunkY << 4) + y, z);
//                                chunkData.loadLater.put(blockEntityTranslator.getDefaultBedrockTag(BlockEntityUtils.getBedrockBlockEntityId(BlockTranslator.getBlockEntityString(blockState)),
//                                        pos.getX(), pos.getY(), pos.getZ()), blockState.getId());
//                            }
//                        } else {
//                            section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z), id);
//                        }
                        section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z), id);
//
//                        if (BlockTranslator.getBlockEntityString(blockState) != null && BlockTranslator.getBlockEntityString(blockState).contains("sign[")) {
//                            Position pos = new ChunkPosition(column.getX(), column.getZ()).getBlock(x, (chunkY << 4) + y, z);
//                            chunkData.signs.put(Translators.getBlockEntityTranslators().get("Sign").getDefaultBedrockTag("Sign", pos.getX(), pos.getY(), pos.getZ()), blockState.getId());
//                        } else if (BlockTranslator.getBlockEntityString(blockState) != null && BlockTranslator.getBlockEntityString(blockState).contains("end_gateway")) {
//                            Position pos = new ChunkPosition(column.getX(), column.getZ()).getBlock(x, (chunkY << 4) + y, z);
//                            chunkData.gateways.put(Translators.getBlockEntityTranslators().get("EndGateway").getDefaultBedrockTag("EndGateway", pos.getX(), pos.getY(), pos.getZ()), blockState.getId());
//                        } else if (BlockTranslator.getBedColor(blockState) > -1) {
//                            Position pos = new ChunkPosition(column.getX(), column.getZ()).getBlock(x, (chunkY << 4) + y, z);
//                            // Beds need to be updated separately to add the bed color tag
//                            // Previously this was done by matching block state but this resulted in only one bed per color+orientation showing
//                            chunkData.beds.put(pos, blockState);
//                        } else if (BlockTranslator.getSkullVariant(blockState) > 0) {
//                            Position pos = new ChunkPosition(column.getX(), column.getZ()).getBlock(x, (chunkY << 4) + y, z);
//                            //Doing the same stuff as beds
//                            chunkData.skulls.put(pos, blockState);
//                        } else {
//                            section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z), id);
//                        }

                        if (BlockTranslator.isWaterlogged(blockState)) {
                            section.getBlockStorageArray()[1].setFullBlock(ChunkSection.blockPosition(x, y, z), BEDROCK_WATER_ID);
                        }
                    }
                }
            }
        }

        com.nukkitx.nbt.tag.CompoundTag[] bedrockBlockEntities = new com.nukkitx.nbt.tag.CompoundTag[blockEntities.length];
        for (int i = 0; i < blockEntities.length; i++) {
            CompoundTag tag = blockEntities[i];
            String tagName;
            if (!tag.contains("id")) {
                GeyserConnector.getInstance().getLogger().debug("Got tag with no id: " + tag.getValue());
                tagName = "Empty";
            } else {
                tagName = (String) tag.get("id").getValue();
                System.out.println(tagName);
            }

            String id = BlockEntityUtils.getBedrockBlockEntityId(tagName);
            BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(id);
            bedrockBlockEntities[i] = blockEntityTranslator.getBlockEntityTag(tagName, tag);
        }

        chunkData.blockEntities = bedrockBlockEntities;
        return chunkData;
    }

    public static void updateChunkPosition(GeyserSession session, Vector3i position) {
        Vector2i chunkPos = session.getLastChunkPosition();
        Vector2i newChunkPos = Vector2i.from(position.getX() >> 4, position.getZ() >> 4);

        if (chunkPos == null || !chunkPos.equals(newChunkPos)) {
            NetworkChunkPublisherUpdatePacket chunkPublisherUpdatePacket = new NetworkChunkPublisherUpdatePacket();
            chunkPublisherUpdatePacket.setPosition(position);
            chunkPublisherUpdatePacket.setRadius(session.getRenderDistance() << 4);
            session.getUpstream().sendPacket(chunkPublisherUpdatePacket);

            session.setLastChunkPosition(newChunkPos);
        }
    }

    public static void updateBlock(GeyserSession session, BlockState blockState, Position position) {
        Vector3i pos = Vector3i.from(position.getX(), position.getY(), position.getZ());
        updateBlock(session, blockState, pos);
    }

    public static void updateBlock(GeyserSession session, BlockState blockState, Vector3i position) {
        int blockId = BlockTranslator.getBedrockBlockId(blockState);

        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(position);
        updateBlockPacket.setRuntimeId(blockId);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.getUpstream().sendPacket(updateBlockPacket);

        UpdateBlockPacket waterPacket = new UpdateBlockPacket();
        waterPacket.setDataLayer(1);
        waterPacket.setBlockPosition(position);
        if (BlockTranslator.isWaterlogged(blockState)) {
            waterPacket.setRuntimeId(BEDROCK_WATER_ID);
        } else {
            waterPacket.setRuntimeId(0);
        }
        session.getUpstream().sendPacket(waterPacket);

        // Since Java stores bed colors as part of the namespaced ID and Bedrock stores it as a tag
        // This is the only place I could find that interacts with the Java block state and block updates
        Reflections ref = new Reflections("org.geysermc.connector.network.translators.block.entity");
        Set<Class<? extends BedrockOnlyBlockEntityTranslator>> classes = ref.getSubTypesOf(BedrockOnlyBlockEntityTranslator.class);
        for (Class<? extends BedrockOnlyBlockEntityTranslator> aClass : classes) {
            try {
                System.out.println("Test");
                BedrockOnlyBlockEntityTranslator bedrockOnlyBlockEntityTranslator = aClass.newInstance();
                bedrockOnlyBlockEntityTranslator.checkForBlockEntity(session, blockState, position);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendEmptyChunks(GeyserSession session, Vector3i position, int radius, boolean forceUpdate) {
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                LevelChunkPacket data = new LevelChunkPacket();
                data.setChunkX(chunkX + x);
                data.setChunkZ(chunkZ + z);
                data.setSubChunksLength(0);
                data.setData(Translators.EMPTY_LEVEL_CHUNK_DATA);
                data.setCachingEnabled(false);
                session.getUpstream().sendPacket(data);

                if (forceUpdate) {
                    Vector3i pos = Vector3i.from(chunkX + x << 4, 80, chunkZ + z << 4);
                    UpdateBlockPacket blockPacket = new UpdateBlockPacket();
                    blockPacket.setBlockPosition(pos);
                    blockPacket.setDataLayer(0);
                    blockPacket.setRuntimeId(1);
                    session.getUpstream().sendPacket(blockPacket);
                }
            }
        }
    }

    public static final class ChunkData {
        public ChunkSection[] sections;

        public Object2IntMap<com.nukkitx.nbt.tag.CompoundTag> loadLater = new Object2IntOpenHashMap<>();

        public com.nukkitx.nbt.tag.CompoundTag[] blockEntities = new com.nukkitx.nbt.tag.CompoundTag[0];
//        public Object2IntMap<com.nukkitx.nbt.tag.CompoundTag> signs = new Object2IntOpenHashMap<>();
//        public Object2IntMap<com.nukkitx.nbt.tag.CompoundTag> gateways = new Object2IntOpenHashMap<>();
//        public Map<Position, BlockState> beds = new HashMap<>();
//        public Map<Position, BlockState> skulls = new HashMap<>();
    }
}
