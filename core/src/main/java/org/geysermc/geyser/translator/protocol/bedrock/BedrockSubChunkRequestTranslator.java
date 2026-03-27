/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.HeightMapDataType;
import org.cloudburstmc.protocol.bedrock.data.SubChunkData;
import org.cloudburstmc.protocol.bedrock.data.SubChunkRequestResult;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.SubChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubChunkRequestPacket;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.chunk.BlockStorage;
import org.geysermc.geyser.level.chunk.GeyserChunk;
import org.geysermc.geyser.level.chunk.GeyserChunkSection;
import org.geysermc.geyser.level.chunk.bitarray.BitArray;
import org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BedrockChunkWantsBlockEntityTag;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.BitStorage;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.GlobalPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.Palette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.SingletonPalette;
import org.geysermc.mcprotocollib.protocol.data.game.level.LightUpdateData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;

import static org.geysermc.geyser.util.ChunkUtils.indexYZXtoXZY;

@Translator(packet = SubChunkRequestPacket.class)
public class BedrockSubChunkRequestTranslator extends PacketTranslator<SubChunkRequestPacket> {
    @Override
    public void translate(GeyserSession session, SubChunkRequestPacket packet) {
        Vector3i centerPosition = packet.getSubChunkPosition();

        SubChunkPacket subChunkPacket = new SubChunkPacket();
        subChunkPacket.setDimension(packet.getDimension());
        subChunkPacket.setCenterPosition(centerPosition);

        int yOffset = session.getChunkCache().getChunkMinY();

        BedrockDimension bedrockDimension = session.getBedrockDimension();
        int maxBedrockSectionY = (bedrockDimension.height() >> 4) - 1;
        int bedrockSubChunkMinY = bedrockDimension.minY() >> 4;
        int bedrockSubChunkMaxY = bedrockSubChunkMinY + (bedrockDimension.height() >> 4);

        ByteBuf byteBuf = null;

        try {
            for (Vector3i positionOffset : packet.getPositionOffsets()) {
                SubChunkData subChunkData = new SubChunkData();
                subChunkData.setPosition(positionOffset);
                subChunkData.setData(Unpooled.EMPTY_BUFFER);
                subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                subChunkData.setRenderHeightMapType(HeightMapDataType.NO_DATA);
                subChunkPacket.getSubChunks().add(subChunkData);

                if (packet.getDimension() != session.getBedrockDimension().bedrockId()) {
                    subChunkData.setResult(SubChunkRequestResult.INVALID_DIMENSION);
                    continue;
                }

                Vector3i position = centerPosition.add(positionOffset);
                GeyserChunk chunk = session.getChunkCache().getChunk(position.getX(), position.getZ());
                if (chunk == null) {
                    subChunkData.setResult(SubChunkRequestResult.CHUNK_NOT_FOUND);
                    continue;
                }

                int sectionY = position.getY() - yOffset;
                if (position.getY() < bedrockSubChunkMinY || position.getY() >= bedrockSubChunkMaxY) {
                    subChunkData.setResult(SubChunkRequestResult.INDEX_OUT_OF_BOUNDS);
                    continue;
                }

                if (sectionY < 0) {
                    subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                } else {
                    LightUpdateData lightData = chunk.lightData();
                    BitSet emptyLightMask = lightData.getEmptySkyYMask();
                    BitSet lightMask = lightData.getSkyYMask();
                    List<byte[]> lightData_ = lightData.getSkyUpdates();
                    if (emptyLightMask.get(sectionY + 1)) {
                        subChunkData.setHeightMapType(HeightMapDataType.TOO_HIGH);
                    } else if (lightMask.get(sectionY + 1)) {
                        byte[] belowLight;
                        if (lightMask.get(sectionY)) {
                            int belowSection = 0;
                            for (int i = 0; i < sectionY; i++) {
                                if (lightMask.get(i)) {
                                    belowSection++;
                                }
                            }
                            belowLight = lightData_.get(belowSection);
                        } else {
                            belowLight = null;
                        }
                        int lightIndex = 0;
                        for (int i = 0; i < sectionY + 1; i++) {
                            if (lightMask.get(i)) {
                                lightIndex++;
                            }
                        }
                        byte[] light = lightData_.get(lightIndex);
                        byte[] aboveLight;
                        if (lightMask.get(sectionY + 2)) {
                            int aboveSection = 0;
                            for (int i = 0; i < sectionY + 2; i++) {
                                if (lightMask.get(i)) {
                                    aboveSection++;
                                }
                            }
                            aboveLight = lightData_.get(aboveSection);
                        } else {
                            aboveLight = null;
                        }

                        byte[] heightMapData = new byte[16 * 16];
                        boolean lower = true, higher = true;
xyLoop:                 for (int i = 0; i < heightMapData.length; i++) {
                            if (aboveLight != null) {
                                int key = i;
                                int index = key >> 1;
                                int part = key & 1;
                                int value = part == 0 ? aboveLight[index] & 15 : aboveLight[index] >> 4 & 15;
                                if (value != 0xF) {
                                    heightMapData[i] = 16;
                                    lower = false;
                                    continue;
                                }
                            }
                            for (int y = 15; y != -1; y--) {
                                int key = i | y << 8;
                                int index = key >> 1;
                                int part = key & 1;
                                int value = part == 0 ? light[index] & 15 : light[index] >> 4 & 15;
                                if (value != 0xF) {
                                    heightMapData[i] = (byte) y;
                                    lower = false;
                                    higher = false;
                                    continue xyLoop;
                                }
                            }
                            if (belowLight != null) {
                                int key = i | 15 << 8;
                                int index = key >> 1;
                                int part = key & 1;
                                int value = part == 0 ? belowLight[index] & 15 : belowLight[index] >> 4 & 15;
                                if (value != 0xF) {
                                    heightMapData[i] = -1;
                                    higher = false;
                                }
                            }
                        }
                        if (lower) {
                            subChunkData.setHeightMapType(HeightMapDataType.TOO_LOW);
                        } else if (higher) {
                            subChunkData.setHeightMapType(HeightMapDataType.TOO_HIGH);
                        } else {
                            subChunkData.setHeightMapType(HeightMapDataType.HAS_DATA);
                            subChunkData.setHeightMapData(Unpooled.wrappedBuffer(heightMapData));
                        }
                    } else {
                        subChunkData.setHeightMapType(HeightMapDataType.TOO_LOW);
                    }
                }

                if (sectionY < 0 || sectionY >= chunk.sections().length) {
                    subChunkData.setResult(SubChunkRequestResult.SUCCESS_ALL_AIR);
                    continue;
                }

                final BlockEntityInfo[] blockEntities = chunk.blockEntities()[sectionY];
                final List<NbtMap> bedrockBlockEntities = new ObjectArrayList<>(blockEntities.length);

                int subChunkIndex = sectionY + yOffset;

                DataPalette javaSection = chunk.sections()[sectionY];
                Palette javaPalette = javaSection.getPalette();
                BitStorage javaData = javaSection.getStorage();
                GeyserChunkSection section;

                if (javaPalette instanceof SingletonPalette) {
                    int javaId = javaPalette.idToState(0);
                    if (javaId == Block.JAVA_AIR_ID) {
                        subChunkData.setResult(SubChunkRequestResult.SUCCESS_ALL_AIR);
                        continue;
                    }

                    int bedrockId = session.getBlockMappings().getBedrockBlockId(javaId);
                    BlockStorage blockStorage = new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(bedrockId));

                    if (BlockRegistries.WATERLOGGED.get().get(javaId)) {
                        BlockStorage waterlogged = new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(session.getBlockMappings().getBedrockWater().getRuntimeId()));
                        section = new GeyserChunkSection(new BlockStorage[] {blockStorage, waterlogged}, subChunkIndex);
                    } else {
                        section = new GeyserChunkSection(new BlockStorage[] {blockStorage}, subChunkIndex);
                    }
                } else if (javaPalette instanceof GlobalPalette) {
                    // As this is the global palette, simply iterate through the whole chunk section once
                    section = new GeyserChunkSection(session.getBlockMappings().getBedrockAir().getRuntimeId(), subChunkIndex);
                    for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                        int javaId = javaData.get(yzx);
                        BlockState state = BlockState.of(javaId);
                        int bedrockId = session.getBlockMappings().getBedrockBlockId(javaId);
                        int xzy = indexYZXtoXZY(yzx);
                        section.getBlockStorageArray()[0].setFullBlock(xzy, bedrockId);

                        if (BlockRegistries.WATERLOGGED.get().get(javaId)) {
                            section.getBlockStorageArray()[1].setFullBlock(xzy, session.getBlockMappings().getBedrockWater().getRuntimeId());
                        }

                        // Check if block is piston or flower to see if we'll need to create additional block entities, as they're only block entities in Bedrock
                        if (state.block() instanceof BedrockChunkWantsBlockEntityTag blockEntity) {
                            bedrockBlockEntities.add(blockEntity.createTag(session,
                                Vector3i.from((position.getX() << 4) + (yzx & 0xF), ((sectionY + yOffset) << 4) + ((yzx >> 8) & 0xF), (position.getZ() << 4) + ((yzx >> 4) & 0xF)),
                                state
                            ));
                        }
                    }
                } else {
                    IntList bedrockPalette = new IntArrayList(javaPalette.size());
                    BitSet waterloggedPaletteIds = new BitSet();
                    BitSet bedrockOnlyBlockEntityIds = new BitSet();

                    // Iterate through palette and convert state IDs to Bedrock, doing some additional checks as we go
                    for (int i = 0; i < javaPalette.size(); i++) {
                        int javaId = javaPalette.idToState(i);
                        bedrockPalette.add(session.getBlockMappings().getBedrockBlockId(javaId));

                        if (BlockRegistries.WATERLOGGED.get().get(javaId)) {
                            waterloggedPaletteIds.set(i);
                        }

                        // Check if block is piston, flower or cauldron to see if we'll need to create additional block entities, as they're only block entities in Bedrock
                        // TODO this needs a performance check when my head is clearer
                        BlockState state = BlockState.of(javaId);
                        if (state.block() instanceof BedrockChunkWantsBlockEntityTag) {
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
                                BlockState state = BlockState.of(javaPalette.idToState(paletteId));
                                bedrockBlockEntities.add(((BedrockChunkWantsBlockEntityTag) state.block()).createTag(session,
                                    Vector3i.from((position.getX() << 4) + (yzx & 0xF), ((sectionY + yOffset) << 4) + ((yzx >> 8) & 0xF), (position.getZ() << 4) + ((yzx >> 4) & 0xF)),
                                    state
                                ));
                            }
                        }
                    }

                    int bedrockDataBits = Integer.SIZE - Integer.numberOfLeadingZeros(javaPalette.size());
                    BitArray bedrockData = BitArrayVersion.forBitsCeil(bedrockDataBits).createArray(BlockStorage.SIZE);
                    BlockStorage layer0 = new BlockStorage(bedrockData, bedrockPalette);
                    BlockStorage[] layers;

                    // Convert data array from YZX to XZY coordinate order
                    if (waterloggedPaletteIds.isEmpty()) {
                        // No blocks are waterlogged, simply convert coordinate order
                        // This could probably be optimized further...
                        for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                            int paletteId = javaData.get(yzx);
                            int xzy = indexYZXtoXZY(yzx);
                            bedrockData.set(xzy, paletteId);
                        }

                        layers = new BlockStorage[]{layer0};
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
                            session.getBlockMappings().getBedrockAir().getRuntimeId(), // Air - see BlockStorage's constructor for more information
                            session.getBlockMappings().getBedrockWater().getRuntimeId());

                        layers = new BlockStorage[]{layer0, new BlockStorage(BitArrayVersion.V1.createArray(BlockStorage.SIZE, layer1Data), layer1Palette)};
                    }

                    section = new GeyserChunkSection(layers, subChunkIndex);
                }

                final int chunkBlockX = position.getX() << 4;
                final int chunkBlockZ = position.getZ() << 4;
                for (BlockEntityInfo blockEntity : blockEntities) {
                    BlockEntityType type = blockEntity.getType();
                    NbtMap tag = blockEntity.getNbt();
                    if (type == null) {
                        // As an example: ViaVersion will send -1 if it cannot find the block entity type
                        // Vanilla Minecraft gracefully handles this
                        continue;
                    }
                    int x = blockEntity.getX(); // Relative to chunk
                    int y = blockEntity.getY();
                    int z = blockEntity.getZ(); // Relative to chunk

                    // Get the Java block state ID from block entity position
                    BlockState blockState = BlockState.of(javaSection.get(x, y & 0xF, z));

                    // Note that, since 1.20.5, tags can be null, but Bedrock still needs a default tag to render the item
                    // Also, some properties - like banner base colors - are part of the tag and is processed here.
                    BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(type);

                    // The Java server can send block entity data for blocks that aren't actually those blocks.
                    // A Java client ignores these
                    if (type == blockState.block().blockEntityType()) {
                        bedrockBlockEntities.add(blockEntityTranslator.getBlockEntityTag(session, type, x + chunkBlockX, y, z + chunkBlockZ, tag, blockState));

                        // Check for custom skulls
                        if (session.getPreferencesCache().showCustomSkulls() && type == BlockEntityType.SKULL && tag != null && tag.containsKey("profile")) {
                            BlockDefinition blockDefinition = SkullBlockEntityTranslator.translateSkull(session, tag, Vector3i.from(x + chunkBlockX, y, z + chunkBlockZ), blockState);
                            if (blockDefinition != null) {
                                int bedrockSectionY = (y >> 4) - (bedrockDimension.minY() >> 4);
                                if (0 <= bedrockSectionY && bedrockSectionY < maxBedrockSectionY) {
                                    // Custom skull is in a section accepted by Bedrock
                                    IntList palette = section.getBlockStorageArray()[0].getPalette();
                                    if (palette instanceof IntImmutableList || palette instanceof IntLists.Singleton) {
                                        // TODO there has to be a better way to expand the palette .-.
                                        section = section.copy(subChunkIndex);
                                    }
                                    section.setFullBlock(x, y & 0xF, z, 0, blockDefinition.getRuntimeId());
                                }
                            }
                        }
                    }
                }

                if (byteBuf == null) {
                    byteBuf = ByteBufAllocator.DEFAULT.ioBuffer(section.estimateNetworkSize() + bedrockBlockEntities.size() * 64);
                } else {
                    byteBuf.clear();
                }

                section.writeToNetwork(byteBuf);
                NBTOutputStream nbtStream = NbtUtils.createNetworkWriter(new ByteBufOutputStream(byteBuf));
                for (NbtMap blockEntity : bedrockBlockEntities) {
                    nbtStream.writeTag(blockEntity);
                }

                subChunkData.setResult(SubChunkRequestResult.SUCCESS);
                subChunkData.setData(Unpooled.wrappedBuffer(ByteBufUtil.getBytes(byteBuf)));
                subChunkPacket.getSubChunks().add((SubChunkData) subChunkData.retain());
            }

            session.sendUpstreamPacket(subChunkPacket);
        } catch (IOException e) {
            session.getGeyser().getLogger().error("IO error while encoding chunk", e);
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }
}
