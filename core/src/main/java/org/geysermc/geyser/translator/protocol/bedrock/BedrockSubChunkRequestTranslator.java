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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import com.github.steveice10.mc.protocol.data.game.level.LightUpdateData;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.data.HeightMapDataType;
import com.nukkitx.protocol.bedrock.data.SubChunkData;
import com.nukkitx.protocol.bedrock.data.SubChunkRequestResult;
import com.nukkitx.protocol.bedrock.packet.SubChunkPacket;
import com.nukkitx.protocol.bedrock.packet.SubChunkRequestPacket;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.chunk.GeyserChunk;
import org.geysermc.geyser.level.chunk.GeyserChunkSection;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.java.level.JavaLevelChunkWithLightTranslator;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.geyser.util.DimensionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;

@Translator(packet = SubChunkRequestPacket.class)
public class BedrockSubChunkRequestTranslator extends PacketTranslator<SubChunkRequestPacket> {
    @Override
    public void translate(GeyserSession session, SubChunkRequestPacket packet) {
        Vector3i centerPosition = packet.getSubChunkPosition();

        SubChunkPacket subChunkPacket = new SubChunkPacket();
        subChunkPacket.setDimension(packet.getDimension());
        subChunkPacket.setCenterPosition(centerPosition);

        int javaSubChunkOffset = session.getChunkCache().getChunkMinY();

        BedrockDimension bedrockDimension = session.getChunkCache().getBedrockDimension();
        int bedrockSubChunkMinY = bedrockDimension.minY() >> 4;
        int bedrockSubChunkMaxY = bedrockSubChunkMinY + (bedrockDimension.height() >> 4);

        ByteBuf byteBuf = null;

        try {
            for (Vector3i positionOffset : packet.getPositionOffsets()) {
                SubChunkData subChunkData = new SubChunkData();
                subChunkData.setPosition(positionOffset);
                subChunkPacket.getSubChunks().add(subChunkData);

                // Should never happen, but if no caching is enabled, send undefined result
                if (!session.getChunkCache().isCache()) {
                    subChunkData.setResult(SubChunkRequestResult.UNDEFINED);
                    subChunkData.setData(new byte[0]);
                    subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                    continue;
                }

                // Check dimension
                if (packet.getDimension() != DimensionUtils.javaToBedrock(session.getDimension())) {
                    subChunkData.setResult(SubChunkRequestResult.INVALID_DIMENSION);
                    subChunkData.setData(new byte[0]);
                    subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                    continue;
                }

                // Check if chunk is cached
                Vector3i position = centerPosition.add(positionOffset);
                GeyserChunk chunk = session.getChunkCache().getChunk(position.getX(), position.getZ());
                if (chunk == null) {
                    subChunkData.setResult(SubChunkRequestResult.CHUNK_NOT_FOUND);
                    subChunkData.setData(new byte[0]);
                    subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                    continue;
                }

                // Check if chunk y index is in range, adjust for Java vs. Bedrock y offset
                int sectionY = position.getY() - javaSubChunkOffset;
                if (position.getY() < bedrockSubChunkMinY || position.getY() >= bedrockSubChunkMaxY) {
                    subChunkData.setResult(SubChunkRequestResult.INDEX_OUT_OF_BOUNDS);
                    subChunkData.setData(new byte[0]);
                    subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                    continue;
                }

                // Ignore if its belows Java Edition min height
                if (sectionY < 0) {
                    subChunkData.setHeightMapType(HeightMapDataType.NO_DATA);
                } else {
                    // This will calculate a light-blocking height map, based on Java Editions
                    // sky-light
                    LightUpdateData lightUpdateData = chunk.lightData();
                    BitSet emptyLightMask = lightUpdateData.getEmptySkyYMask();
                    BitSet lightMask = lightUpdateData.getSkyYMask();
                    List<byte[]> lightData = lightUpdateData.getSkyUpdates();
                    // Check if its empty (aka. the height map is too high/section is underground)
                    if (emptyLightMask.get(sectionY + 1)) {
                        subChunkData.setHeightMapType(HeightMapDataType.TOO_HIGH);
                    } else if (lightMask.get(sectionY + 1)) {
                        // If there is light data, get the light data for below the current section or null
                        byte[] belowLight;
                        if (lightMask.get(sectionY)) {
                            int belowSection = 0;
                            for (int i = 0; i < sectionY; i++) {
                                if (lightMask.get(i)) {
                                    belowSection++;
                                }
                            }
                            belowLight = lightData.get(belowSection);
                        } else {
                            belowLight = null;
                        }

                        // Get the light data for the current section
                        int lightIndex = 0;
                        for (int i = 0; i < sectionY + 1; i++) {
                            if (lightMask.get(i)) {
                                lightIndex++;
                            }
                        }
                        byte[] light = lightData.get(lightIndex);

                        // Get the light data for above the current section or null
                        byte[] aboveLight;
                        if (lightMask.get(sectionY + 2)) {
                            int aboveSection = 0;
                            for (int i = 0; i < sectionY + 2; i++) {
                                if (lightMask.get(i)) {
                                    aboveSection++;
                                }
                            }
                            aboveLight = lightData.get(aboveSection);
                        } else {
                            aboveLight = null;
                        }

                        // Iterate through all columns, and get the row where sky-light is blocked
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

                        // Check if everything is lower, or higher, as there is no need to send the height map data
                        if (lower) {
                            subChunkData.setHeightMapType(HeightMapDataType.TOO_LOW);
                        } else if (higher) {
                            subChunkData.setHeightMapType(HeightMapDataType.TOO_HIGH);
                        } else {
                            subChunkData.setHeightMapType(HeightMapDataType.HAS_DATA);
                            subChunkData.setHeightMapData(heightMapData);
                        }
                    } else {
                        subChunkData.setHeightMapType(HeightMapDataType.TOO_LOW);
                    }
                }

                DataPalette javaSection = sectionY < 0 || sectionY >= chunk.sections().length ? null : chunk.sections()[sectionY];
                if (javaSection == null) {
                    subChunkData.setResult(SubChunkRequestResult.SUCCESS_ALL_AIR);
                    subChunkData.setData(new byte[0]);
                    continue;
                }

                final BlockEntityInfo[] blockEntities = chunk.blockEntities()[sectionY];
                final List<NbtMap> bedrockBlockEntities = new ObjectArrayList<>();

                GeyserChunkSection section = JavaLevelChunkWithLightTranslator.translateSubChunk(session, position, javaSection, bedrockBlockEntities);

                final int chunkBlockX = position.getX() << 4;
                final int chunkBlockZ = position.getZ() << 4;
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
                    int blockState = javaSection.get(x, y & 0xF, z);

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

                if (byteBuf == null) {
                    byteBuf = ByteBufAllocator.DEFAULT.buffer(section.estimateNetworkSize() + bedrockBlockEntities.size() * 64);
                } else {
                    byteBuf.clear();
                }

                section.writeToNetwork(byteBuf);
                NBTOutputStream nbtStream = NbtUtils.createNetworkWriter(new ByteBufOutputStream(byteBuf));
                for (NbtMap blockEntity : bedrockBlockEntities) {
                    nbtStream.writeTag(blockEntity);
                }

                subChunkData.setResult(SubChunkRequestResult.SUCCESS);
                subChunkData.setData(ByteBufUtil.getBytes(byteBuf));
                subChunkPacket.getSubChunks().add(subChunkData);
            }

            session.sendUpstreamPacket(subChunkPacket);
        } catch (IOException ex) {
            session.getGeyser().getLogger().error("IO error while encoding chunk", ex);
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }
}
