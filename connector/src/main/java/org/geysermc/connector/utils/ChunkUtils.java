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
import com.github.steveice10.mc.protocol.data.game.chunk.FlexibleStorage;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BedrockOnlyBlockEntity;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.RequiresBlockState;
import org.geysermc.connector.network.translators.world.chunk.BlockStorage;
import org.geysermc.connector.network.translators.world.chunk.ChunkSection;
import org.geysermc.connector.network.translators.world.chunk.bitarray.BitArray;
import org.geysermc.connector.network.translators.world.chunk.bitarray.BitArrayVersion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.geysermc.connector.network.translators.world.block.BlockTranslator.*;

@UtilityClass
public class ChunkUtils {

    /**
     * Temporarily stores positions of BlockState values that are needed for certain block entities actively
     */
    public static final Object2IntMap<Position> CACHED_BLOCK_ENTITIES = new Object2IntOpenHashMap<>();

    private static final NbtMap EMPTY_TAG = NbtMap.builder().build();
    public static final byte[] EMPTY_LEVEL_CHUNK_DATA;

    public static final BlockStorage EMPTY_STORAGE = new BlockStorage();
    public static final ChunkSection EMPTY_SECTION = new ChunkSection(new BlockStorage[]{ EMPTY_STORAGE });

    static {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(new byte[258]); // Biomes + Border Size + Extra Data Size

            try (NBTOutputStream stream = NbtUtils.createNetworkWriter(outputStream)) {
                stream.writeTag(EMPTY_TAG);
            }

            EMPTY_LEVEL_CHUNK_DATA = outputStream.toByteArray();
        } catch (IOException e) {
            throw new AssertionError("Unable to generate empty level chunk data");
        }
    }

    private static int indexYZXtoXZY(int yzx) {
        return (yzx >> 8) | (yzx & 0x0F0) | ((yzx & 0x00F) << 8);
    }

    public static ChunkData translateToBedrock(GeyserSession session, Column column) {
        ChunkData chunkData = new ChunkData();
        Chunk[] javaChunks = column.getChunks();
        chunkData.sections = new ChunkSection[javaChunks.length];

        // Temporarily stores compound tags of Bedrock-only block entities
        List<NbtMap> bedrockOnlyBlockEntities = new ArrayList<>();

        BitSet waterloggedPaletteIds = new BitSet();
        BitSet pistonOrFlowerPaletteIds = new BitSet();

        for (int chunkY = 0; chunkY < javaChunks.length; chunkY++) {
            Chunk javaChunk = javaChunks[chunkY];

            // Chunk is null, the cache will not contain anything of use
            //TODO: on spigot, it's possible that caching will be disabled but the block data will be accessible from the world manager anyway
            // maybe fall back to slow implementation?
            if (javaChunk == null || javaChunk.isEmpty()) {
                continue;
            }

            List<Integer> javaPalette = javaChunk.getPalette();
            IntList bedrockPalette = new IntArrayList(javaPalette.size());
            waterloggedPaletteIds.clear();
            pistonOrFlowerPaletteIds.clear();
            for (int i = 0; i < javaPalette.size(); i++) {
                int javaId = javaPalette.get(i);
                bedrockPalette.add(BlockTranslator.getBedrockBlockId(javaId));

                if (BlockTranslator.isWaterlogged(javaId)) {
                    waterloggedPaletteIds.set(i);
                }

                // Check if block is piston or flower - only block entities in Bedrock
                if (BlockStateValues.getFlowerPotValues().containsKey(javaId) || BlockStateValues.getPistonValues().containsKey(javaId)) {
                    pistonOrFlowerPaletteIds.set(i);
                }
            }

            FlexibleStorage javaArray = javaChunk.getStorage();

            // Add Bedrock-exclusive block entities
            if (!pistonOrFlowerPaletteIds.isEmpty()) {
                for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                    int paletteId = javaArray.get(yzx);
                    if (pistonOrFlowerPaletteIds.get(paletteId)) {
                        bedrockOnlyBlockEntities.add(BedrockOnlyBlockEntity.getTag(
                                Vector3i.from((column.getX() << 4) + (yzx & 0xF), (chunkY << 4) + ((yzx >> 8) & 0xF), (column.getZ() << 4) + ((yzx >> 4) & 0xF)),
                                javaPalette.get(paletteId)
                        ));
                    }
                }
            }

            BitArrayVersion bedrockVersion = BitArrayVersion.forBitsCeil(javaArray.getBitsPerEntry());
            BitArray bedrockArray = bedrockVersion.createPalette(BlockStorage.SIZE);

            BlockStorage layer0 = new BlockStorage(bedrockArray, bedrockPalette);
            ChunkSection section;

            // Convert palettized array from YZX to XZY coordinate order
            if (waterloggedPaletteIds.isEmpty()) {
                // This could probably be optimized further...
                for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                    bedrockArray.set(indexYZXtoXZY(yzx), javaArray.get(yzx));
                }

                section = new ChunkSection(new BlockStorage[]{ layer0 });
            } else {
                // Generate V1 blocks storage for layer 1 with palette ID 1 indicating water
                int[] layer1Data = new int[BlockStorage.SIZE >> 5];
                for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                    int paletteId = javaArray.get(yzx);
                    int xzy = indexYZXtoXZY(yzx);
                    bedrockArray.set(xzy, paletteId);

                    if (waterloggedPaletteIds.get(paletteId)) {
                        layer1Data[xzy >> 5] |= 1 << (xzy & 0x1F);
                    }
                }

                IntList layer1Ids = new IntArrayList(2);
                layer1Ids.add(0); // Air
                layer1Ids.add(BEDROCK_WATER_ID);

                section = new ChunkSection(new BlockStorage[]{
                        layer0,
                        new BlockStorage(BitArrayVersion.V1.createPalette(BlockStorage.SIZE, layer1Data), layer1Ids)
                });
            }

            chunkData.sections[chunkY] = section;
        }

        CompoundTag[] blockEntities = column.getTileEntities();
        NbtMap[] bedrockBlockEntities = new NbtMap[blockEntities.length + bedrockOnlyBlockEntities.size()];
        int i = 0;
        while (i < blockEntities.length) {
            CompoundTag tag = blockEntities[i];
            String tagName;
            if (tag.contains("id")) {
                tagName = (String) tag.get("id").getValue();
            } else {
                tagName = "Empty";
                // Sometimes legacy tags have their ID be a StringTag with empty value
                for (Tag subTag : tag) {
                    if (subTag instanceof StringTag) {
                        StringTag stringTag = (StringTag) subTag;
                        if (stringTag.getValue().isEmpty()) {
                            tagName = stringTag.getName();
                            break;
                        }
                    }
                }
                if (tagName.equals("Empty")) {
                    GeyserConnector.getInstance().getLogger().debug("Got tag with no id: " + tag.getValue());
                }
            }

            String id = BlockEntityUtils.getBedrockBlockEntityId(tagName);
            BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(id);
            Position pos = new Position((int) tag.get("x").getValue(), (int) tag.get("y").getValue(), (int) tag.get("z").getValue());

            // Get Java blockstate ID from block entity position
            int blockState = 0;
            Chunk chunk = column.getChunks()[pos.getY() >> 4];
            if (chunk != null) {
                blockState = chunk.get(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
            }

            bedrockBlockEntities[i] = blockEntityTranslator.getBlockEntityTag(tagName, tag, blockState);
            i++;
        }
        for (NbtMap tag : bedrockOnlyBlockEntities) {
            bedrockBlockEntities[i] = tag;
            i++;
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
            session.sendUpstreamPacket(chunkPublisherUpdatePacket);

            session.setLastChunkPosition(newChunkPos);
        }
    }

    public static void updateBlock(GeyserSession session, int blockState, Position position) {
        Vector3i pos = Vector3i.from(position.getX(), position.getY(), position.getZ());
        updateBlock(session, blockState, pos);
    }

    public static void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        // Checks for item frames so they aren't tripped up and removed
        if (ItemFrameEntity.positionContainsItemFrame(session, position) && blockState == AIR) {
            ((ItemFrameEntity) session.getEntityCache().getEntityByJavaId(ItemFrameEntity.getItemFrameEntityId(session, position))).updateBlock(session);
            return;
        } else if (ItemFrameEntity.positionContainsItemFrame(session, position)) {
            Entity entity = session.getEntityCache().getEntityByJavaId(ItemFrameEntity.getItemFrameEntityId(session, position));
            if (entity != null) {
                session.getEntityCache().removeEntity(entity, false);
            } else {
                ItemFrameEntity.removePosition(session, position);
            }
        }

        int blockId = BlockTranslator.getBedrockBlockId(blockState);

        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(position);
        updateBlockPacket.setRuntimeId(blockId);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        session.sendUpstreamPacket(updateBlockPacket);

        UpdateBlockPacket waterPacket = new UpdateBlockPacket();
        waterPacket.setDataLayer(1);
        waterPacket.setBlockPosition(position);
        if (BlockTranslator.isWaterlogged(blockState)) {
            waterPacket.setRuntimeId(BEDROCK_WATER_ID);
        } else {
            waterPacket.setRuntimeId(0);
        }
        session.sendUpstreamPacket(waterPacket);

        // Since Java stores bed colors/skull information as part of the namespaced ID and Bedrock stores it as a tag
        // This is the only place I could find that interacts with the Java block state and block updates
        // Iterates through all block entity translators and determines if the block state needs to be saved
        for (RequiresBlockState requiresBlockState : BlockEntityTranslator.REQUIRES_BLOCK_STATE_LIST) {
            if (requiresBlockState.isBlock(blockState)) {
                // Flower pots are block entities only in Bedrock and are not updated anywhere else like note blocks
                if (requiresBlockState instanceof BedrockOnlyBlockEntity) {
                    ((BedrockOnlyBlockEntity) requiresBlockState).updateBlock(session, blockState, position);
                    break;
                }
                CACHED_BLOCK_ENTITIES.put(new Position(position.getX(), position.getY(), position.getZ()), blockState);
                break; //No block will be a part of two classes
            }
        }
        session.getChunkCache().updateBlock(position.getX(), position.getY(), position.getZ(), blockState);
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
                data.setData(EMPTY_LEVEL_CHUNK_DATA);
                data.setCachingEnabled(false);
                session.sendUpstreamPacket(data);

                if (forceUpdate) {
                    Vector3i pos = Vector3i.from(chunkX + x << 4, 80, chunkZ + z << 4);
                    UpdateBlockPacket blockPacket = new UpdateBlockPacket();
                    blockPacket.setBlockPosition(pos);
                    blockPacket.setDataLayer(0);
                    blockPacket.setRuntimeId(1);
                    session.sendUpstreamPacket(blockPacket);
                }
            }
        }
    }

    public static final class ChunkData {
        public ChunkSection[] sections;

        @Getter
        private NbtMap[] blockEntities = new NbtMap[0];
    }
}
