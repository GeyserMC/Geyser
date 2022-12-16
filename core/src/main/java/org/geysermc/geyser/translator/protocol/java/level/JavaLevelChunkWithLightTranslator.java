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

import com.github.steveice10.mc.protocol.data.game.chunk.BitStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.GlobalPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.Palette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.SingletonPalette;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.chunk.BlockStorage;
import org.geysermc.geyser.level.chunk.GeyserChunkSection;
import org.geysermc.geyser.level.chunk.bitarray.BitArray;
import org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.translator.level.block.entity.BedrockOnlyBlockEntity;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.geyser.util.ChunkUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.geysermc.geyser.util.ChunkUtils.SERIALIZED_CHUNK_DATA;
import static org.geysermc.geyser.util.ChunkUtils.indexYZXtoXZY;

@Translator(packet = ClientboundLevelChunkWithLightPacket.class)
public class JavaLevelChunkWithLightTranslator extends PacketTranslator<ClientboundLevelChunkWithLightPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLevelChunkWithLightPacket packet) {
        if (session.isSpawned()) {
            ChunkUtils.updateChunkPosition(session, session.getPlayerEntity().getPosition().toInt());
        }

        // Ensure that, if the player is using lower world heights, the position is not offset
        int javaSubChunkOffset = session.getChunkCache().getChunkMinY();
        int javaSubChunkCount = session.getChunkCache().getChunkHeightY();
        int biomeGlobalPalette = session.getBiomeGlobalPalette();

        DataPalette[] javaChunks = new DataPalette[javaSubChunkCount];
        DataPalette[] javaBiomes = new DataPalette[javaSubChunkCount];

        BedrockDimension bedrockDimension = session.getChunkCache().getBedrockDimension();
        int bedrockSubChunkOffset = bedrockDimension.minY() >> 4;
        int bedrockSubChunkCount = bedrockDimension.height() >> 4;
        int subChunkDiff = javaSubChunkOffset + bedrockSubChunkOffset;

        final boolean requestSubChunks = session.getChunkCache().isCache();
        final BlockEntityInfo[] blockEntities = packet.getBlockEntities();
        final Map<Integer, List<BlockEntityInfo>> _blockEntitiesBySection;
        final BlockEntityInfo[][] blockEntitiesBySection;
        final List<NbtMap> bedrockBlockEntities;
        final GeyserChunkSection[] sections;
        if (requestSubChunks) {
            _blockEntitiesBySection = Arrays.stream(blockEntities).collect(Collectors.groupingBy(blockEntity -> (blockEntity.getY() >> 4) - javaSubChunkOffset));
            blockEntitiesBySection = new BlockEntityInfo[javaSubChunkCount][];
            bedrockBlockEntities = null;
            sections = new GeyserChunkSection[javaChunks.length - subChunkDiff];
        } else {
            _blockEntitiesBySection = null;
            blockEntitiesBySection = null;
            bedrockBlockEntities = new ObjectArrayList<>(blockEntities.length);
            sections = new GeyserChunkSection[javaChunks.length - subChunkDiff];
        }

        int sectionCount;
        byte[] payload;
        ByteBuf byteBuf = null;

        try {
            ByteBuf in = Unpooled.wrappedBuffer(packet.getChunkData());
            for (int sectionY = 0; sectionY < javaSubChunkCount; sectionY++) {
                ChunkSection javaSection = session.getCodecHelper().readChunkSection(in, biomeGlobalPalette);
                javaBiomes[sectionY] = javaSection.getBiomeData();

                int bedrockSectionY = sectionY + (javaSubChunkOffset - (bedrockDimension.minY() >> 4));
                if (bedrockSectionY < 0 || bedrockSectionY > bedrockSubChunkCount - 1) {
                    // Ignore this chunk section since it goes outside the bounds accepted by the Bedrock client
                    continue;
                }

                // No need to encode an empty section...
                if (javaSection.isBlockCountEmpty()) {
                    continue;
                }

                javaChunks[sectionY] = javaSection.getChunkData();
                if (requestSubChunks) {
                    blockEntitiesBySection[sectionY] = _blockEntitiesBySection.getOrDefault(sectionY, Collections.emptyList()).toArray(new BlockEntityInfo[0]);
                    sections[bedrockSectionY] = new GeyserChunkSection(null);
                } else {
                    sections[bedrockSectionY] = translateSubChunk(session, Vector3i.from(packet.getX(), sectionY + javaSubChunkOffset, packet.getZ()), javaSection.getChunkData(), bedrockBlockEntities);
                }
            }
            session.getChunkCache().addToCache(packet.getX(), packet.getZ(), javaChunks, blockEntitiesBySection, requestSubChunks ? packet.getLightData() : null);

            if (!requestSubChunks) {
                final int chunkBlockX = packet.getX() << 4;
                final int chunkBlockZ = packet.getZ() << 4;
                for (BlockEntityInfo blockEntity : blockEntities) {
                    BlockEntityType type = blockEntity.getType();
                    if (type == null) {
                        // As an example: ViaVersion will send -1 if it cannot find the block entity type
                        // Vanilla Minecraft gracefully handles this
                        continue;
                    }
                    CompoundTag tag = blockEntity.getNbt();
                    int x = blockEntity.getX(); // Relative to chunk
                    int y = blockEntity.getY();
                    int z = blockEntity.getZ(); // Relative to chunk

                    // Get the Java block state ID from block entity position
                    DataPalette section = javaChunks[(y >> 4) - javaSubChunkOffset];
                    if (section != null) {
                        int blockState = section.get(x, y & 0xF, z);

                        if (type == BlockEntityType.LECTERN && BlockStateValues.getLecternBookStates().get(blockState)) {
                            // If getLecternBookStates is false, let's just treat it like a normal block entity
                            bedrockBlockEntities.add(session.getGeyser().getWorldManager().getLecternDataAt(
                                    session, x + chunkBlockX, y, z + chunkBlockZ, true));
                            continue;
                        }

                        BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(type);
                        bedrockBlockEntities.add(blockEntityTranslator.getBlockEntityTag(type, x + chunkBlockX, y, z + chunkBlockZ, tag, blockState));

                        // Check for custom skulls
                        if (session.getPreferencesCache().showCustomSkulls() && type == BlockEntityType.SKULL && tag != null && tag.contains("SkullOwner")) {
                            SkullBlockEntityTranslator.translateSkull(session, tag, x + chunkBlockX, y, z + chunkBlockZ, blockState);
                        }
                    }
                }
            }

            // Find highest section
            sectionCount = sections.length - 1;
            while (sectionCount >= 0 && sections[sectionCount] == null) {
                sectionCount--;
            }
            sectionCount++;

            // Estimate chunk size
            int size = 0;
            if (!requestSubChunks) {
                for (int i = 0; i < sectionCount; i++) {
                    GeyserChunkSection section = sections[i];
                    if (section != null) {
                        size += section.estimateNetworkSize();
                    } else {
                        size += SERIALIZED_CHUNK_DATA.length;
                    }
                }
            }
            size += ChunkUtils.EMPTY_BIOME_DATA.length * bedrockSubChunkCount + 1; // Consists only of biome data and border blocks

            // Allocate output buffer
            byteBuf = ByteBufAllocator.DEFAULT.buffer(size);

            if (!requestSubChunks) {
                for (int i = 0; i < sectionCount; i++) {
                    GeyserChunkSection section = sections[i];

                    if (section != null) {
                        section.writeToNetwork(byteBuf);
                    } else {
                        byteBuf.writeBytes(SERIALIZED_CHUNK_DATA);
                    }
                }
            }

            // As of 1.18.30, the amount of biomes read is dependent on how high Bedrock thinks the dimension is
            for (int i = 0; i < bedrockSubChunkCount; i++) {
                int biomeYOffset = bedrockSubChunkOffset + i;
                if (biomeYOffset < javaSubChunkOffset) {
                    // Ignore this biome section since it goes below the height of the Java world
                    byteBuf.writeBytes(ChunkUtils.EMPTY_BIOME_DATA);
                    continue;
                }
                if (biomeYOffset >= (javaSubChunkCount + javaSubChunkOffset)) {
                    // This biome section goes above the height of the Java world
                    // The byte written here is a header that says to carry on the biome data from the previous chunk
                    byteBuf.writeByte((127 << 1) | 1);
                    continue;
                }

                BiomeTranslator.toNewBedrockBiome(session, javaBiomes[i + (bedrockSubChunkOffset - javaSubChunkOffset)]).writeToNetwork(byteBuf);
            }

            byteBuf.writeByte(0); // Border blocks - Edu edition only

            // Encode tile entities into buffer
            if (!requestSubChunks) {
                NBTOutputStream nbtStream = NbtUtils.createNetworkWriter(new ByteBufOutputStream(byteBuf));
                for (NbtMap blockEntity : bedrockBlockEntities) {
                    nbtStream.writeTag(blockEntity);
                }
            }

            // Copy data into byte[], because the protocol lib really likes things that are s l o w
            byteBuf.readBytes(payload = new byte[byteBuf.readableBytes()]);
        } catch (IOException e) {
            session.getGeyser().getLogger().error("IO error while encoding chunk", e);
            return;
        } finally {
            if (byteBuf != null) {
                byteBuf.release(); // Release buffer to allow buffer pooling to be useful
            }
        }

        LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
        levelChunkPacket.setSubChunksLength(sectionCount);
        levelChunkPacket.setChunkX(packet.getX());
        levelChunkPacket.setChunkZ(packet.getZ());
        levelChunkPacket.setRequestSubChunks(requestSubChunks);
        levelChunkPacket.setSubChunkLimit(sectionCount - 1);
        levelChunkPacket.setData(payload);
        session.sendUpstreamPacket(levelChunkPacket);

        for (Map.Entry<Vector3i, ItemFrameEntity> entry : session.getItemFrameCache().entrySet()) {
            Vector3i position = entry.getKey();
            if ((position.getX() >> 4) == packet.getX() && (position.getZ() >> 4) == packet.getZ()) {
                // Update this item frame so it doesn't get lost in the abyss
                //TODO optimize
                entry.getValue().updateBlock(true);
            }
        }
    }

    public static GeyserChunkSection translateSubChunk(GeyserSession session, Vector3i position, DataPalette javaSection, List<NbtMap> bedrockBlockEntities) {
        Palette javaPalette = javaSection.getPalette();
        BitStorage javaData = javaSection.getStorage();

        if (javaPalette instanceof GlobalPalette) {
            // As this is the global palette, simply iterate through the whole chunk section once
            GeyserChunkSection section = new GeyserChunkSection(session.getBlockMappings().getBedrockAirId());
            for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                int javaId = javaData.get(yzx);
                int bedrockId = session.getBlockMappings().getBedrockBlockId(javaId);
                int xzy = indexYZXtoXZY(yzx);
                section.getBlockStorageArray()[0].setFullBlock(xzy, bedrockId);

                if (BlockRegistries.WATERLOGGED.get().contains(javaId)) {
                    section.getBlockStorageArray()[1].setFullBlock(xzy, session.getBlockMappings().getBedrockWaterId());
                }

                // Check if block is piston or flower to see if we'll need to create additional block entities, as they're only block entities in Bedrock
                if (BlockStateValues.getFlowerPotValues().containsKey(javaId) || BlockStateValues.getPistonValues().containsKey(javaId) || BlockStateValues.isNonWaterCauldron(javaId)) {
                    bedrockBlockEntities.add(BedrockOnlyBlockEntity.getTag(session,
                            Vector3i.from((position.getX() << 4) + (yzx & 0xF), (position.getY() << 4) + ((yzx >> 8) & 0xF), (position.getZ() << 4) + ((yzx >> 4) & 0xF)),
                            javaId
                    ));
                }
            }
            return section;
        }

        if (javaPalette instanceof SingletonPalette) {
            // There's only one block here. Very easy!
            int javaId = javaPalette.idToState(0);
            int bedrockId = session.getBlockMappings().getBedrockBlockId(javaId);
            BlockStorage blockStorage = new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(bedrockId));

            if (BlockRegistries.WATERLOGGED.get().contains(javaId)) {
                BlockStorage waterlogged = new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(session.getBlockMappings().getBedrockWaterId()));
                return new GeyserChunkSection(new BlockStorage[] {blockStorage, waterlogged});
            } else {
                return new GeyserChunkSection(new BlockStorage[] {blockStorage});
            }
        }

        IntList bedrockPalette = new IntArrayList(javaPalette.size());
        BitSet waterloggedPaletteIds = new BitSet();
        BitSet bedrockOnlyBlockEntityIds = new BitSet();

        // Iterate through palette and convert state IDs to Bedrock, doing some additional checks as we go
        for (int i = 0; i < javaPalette.size(); i++) {
            int javaId = javaPalette.idToState(i);
            bedrockPalette.add(session.getBlockMappings().getBedrockBlockId(javaId));

            if (BlockRegistries.WATERLOGGED.get().contains(javaId)) {
                waterloggedPaletteIds.set(i);
            }

            // Check if block is piston, flower or cauldron to see if we'll need to create additional block entities, as they're only block entities in Bedrock
            if (BlockStateValues.getFlowerPotValues().containsKey(javaId) || BlockStateValues.getPistonValues().containsKey(javaId) || BlockStateValues.isNonWaterCauldron(javaId)) {
                bedrockOnlyBlockEntityIds.set(i);
            }
        }

        // Add Bedrock-exclusive block entities
        // We only if the palette contained any blocks that are Bedrock-exclusive block entities to avoid iterating through the whole block data
        // for no reason, as most sections will not contain any pistons or flower pots
        if (!bedrockOnlyBlockEntityIds.isEmpty()) {
            for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                int paletteId = javaData.get(yzx);
                if (bedrockOnlyBlockEntityIds.get(paletteId)) {
                    bedrockBlockEntities.add(BedrockOnlyBlockEntity.getTag(session,
                            Vector3i.from((position.getX() << 4) + (yzx & 0xF), (position.getY() << 4) + ((yzx >> 8) & 0xF), (position.getZ() << 4) + ((yzx >> 4) & 0xF)),
                            javaPalette.idToState(paletteId)
                    ));
                }
            }
        }

        BitArray bedrockData = BitArrayVersion.forBitsCeil(javaData.getBitsPerEntry()).createArray(BlockStorage.SIZE);
        BlockStorage layer0 = new BlockStorage(bedrockData, bedrockPalette);
        BlockStorage[] layers;

        // Convert data array from YZX to XZY coordinate order
        if (waterloggedPaletteIds.isEmpty()) {
            // No blocks are waterlogged, simply convert coordinate order
            // This could probably be optimized further...
            for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                bedrockData.set(indexYZXtoXZY(yzx), javaData.get(yzx));
            }

            layers = new BlockStorage[]{ layer0 };
        } else {
            // The section contains waterlogged blocks, we need to convert coordinate order AND generate a V1 block storage for
            // layer 1 with palette ID 1 indicating water
            int[] layer1Data = new int[BlockStorage.SIZE >> 5];
            for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                int paletteId = javaData.get(yzx);
                int xzy = indexYZXtoXZY(yzx);
                bedrockData.set(xzy, paletteId);

                if (waterloggedPaletteIds.get(paletteId)) {
                    layer1Data[xzy >> 5] |= 1 << (xzy & 0x1F);
                }
            }

            // V1 palette
            IntList layer1Palette = IntList.of(
                    session.getBlockMappings().getBedrockAirId(), // Air - see BlockStorage's constructor for more information
                    session.getBlockMappings().getBedrockWaterId());

            layers = new BlockStorage[]{ layer0, new BlockStorage(BitArrayVersion.V1.createArray(BlockStorage.SIZE, layer1Data), layer1Palette) };
        }

        return new GeyserChunkSection(layers);
    }
}