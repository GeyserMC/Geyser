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

#include "io.netty.buffer.ByteBuf"
#include "io.netty.buffer.ByteBufAllocator"
#include "io.netty.buffer.ByteBufOutputStream"
#include "io.netty.buffer.Unpooled"
#include "it.unimi.dsi.fastutil.ints.IntArrayList"
#include "it.unimi.dsi.fastutil.ints.IntImmutableList"
#include "it.unimi.dsi.fastutil.ints.IntList"
#include "it.unimi.dsi.fastutil.ints.IntLists"
#include "it.unimi.dsi.fastutil.objects.ObjectArrayList"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NBTOutputStream"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtUtils"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket"
#include "org.geysermc.geyser.entity.type.ItemFrameEntity"
#include "org.geysermc.geyser.level.BedrockDimension"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.chunk.BlockStorage"
#include "org.geysermc.geyser.level.chunk.GeyserChunkSection"
#include "org.geysermc.geyser.level.chunk.bitarray.BitArray"
#include "org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion"
#include "org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.translator.level.BiomeTranslator"
#include "org.geysermc.geyser.translator.level.block.entity.BedrockChunkWantsBlockEntityTag"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.geyser.translator.level.block.entity.SkullBlockEntityTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.BlockEntityUtils"
#include "org.geysermc.geyser.util.ChunkUtils"
#include "org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.chunk.BitStorage"
#include "org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection"
#include "org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette"
#include "org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.GlobalPalette"
#include "org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.Palette"
#include "org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.SingletonPalette"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket"

#include "java.io.IOException"
#include "java.util.BitSet"
#include "java.util.List"
#include "java.util.Map"

#include "static org.geysermc.geyser.util.ChunkUtils.EMPTY_BLOCK_STORAGE"
#include "static org.geysermc.geyser.util.ChunkUtils.EMPTY_CHUNK_SECTION_SIZE"
#include "static org.geysermc.geyser.util.ChunkUtils.indexYZXtoXZY"

@Translator(packet = ClientboundLevelChunkWithLightPacket.class)
public class JavaLevelChunkWithLightTranslator extends PacketTranslator<ClientboundLevelChunkWithLightPacket> {

    override public void translate(GeyserSession session, ClientboundLevelChunkWithLightPacket packet) {
        if (session.isSpawned()) {
            ChunkUtils.updateChunkPosition(session, session.getPlayerEntity().position().toInt());
        }


        int yOffset = session.getChunkCache().getChunkMinY();
        int chunkSize = session.getChunkCache().getChunkHeightY();

        DataPalette[] javaChunks = new DataPalette[chunkSize];
        DataPalette[] javaBiomes = new DataPalette[chunkSize];

        final BlockEntityInfo[] blockEntities = packet.getBlockEntities();
        final List<NbtMap> bedrockBlockEntities = new ObjectArrayList<>(blockEntities.length);

        BitSet waterloggedPaletteIds = new BitSet();
        BitSet bedrockOnlyBlockEntityIds = new BitSet();

        BedrockDimension bedrockDimension = session.getBedrockDimension();
        int maxBedrockSectionY = (bedrockDimension.height() >> 4) - 1;

        int sectionCount;
        byte[] payload;
        ByteBuf byteBuf = null;



        int sectionCountDiff = yOffset - (bedrockDimension.minY() >> 4);
        GeyserChunkSection[] sections = new GeyserChunkSection[chunkSize + sectionCountDiff];

        try {
            ByteBuf in = Unpooled.wrappedBuffer(packet.getChunkData());
            for (int sectionY = 0; sectionY < chunkSize; sectionY++) {
                ChunkSection javaSection = MinecraftTypes.readChunkSection(in, BlockRegistries.BLOCK_STATES.get().size(),
                    session.getRegistryCache().registry(JavaRegistries.BIOME).size());
                javaChunks[sectionY] = javaSection.getBlockData();
                javaBiomes[sectionY] = javaSection.getBiomeData();

                int bedrockSectionY = sectionY + sectionCountDiff;
                int subChunkIndex = sectionY + yOffset;
                if (bedrockSectionY < 0 || maxBedrockSectionY < bedrockSectionY) {

                    continue;
                }


                if (javaSection.isBlockCountEmpty()) {
                    continue;
                }

                Palette javaPalette = javaSection.getBlockData().getPalette();
                BitStorage javaData = javaSection.getBlockData().getStorage();

                if (javaPalette instanceof GlobalPalette) {

                    GeyserChunkSection section = new GeyserChunkSection(session.getBlockMappings().getBedrockAir().getRuntimeId(), subChunkIndex);
                    for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                        int javaId = javaData.get(yzx);
                        BlockState state = BlockState.of(javaId);
                        int bedrockId = session.getBlockMappings().getBedrockBlockId(javaId);
                        int xzy = indexYZXtoXZY(yzx);
                        section.getBlockStorageArray()[0].setFullBlock(xzy, bedrockId);

                        if (BlockRegistries.WATERLOGGED.get().get(javaId)) {
                            section.getBlockStorageArray()[1].setFullBlock(xzy, session.getBlockMappings().getBedrockWater().getRuntimeId());
                        }


                        if (state.block() instanceof BedrockChunkWantsBlockEntityTag blockEntity) {
                            bedrockBlockEntities.add(blockEntity.createTag(session,
                                    Vector3i.from((packet.getX() << 4) + (yzx & 0xF), ((sectionY + yOffset) << 4) + ((yzx >> 8) & 0xF), (packet.getZ() << 4) + ((yzx >> 4) & 0xF)),
                                    state
                            ));
                        }
                    }
                    sections[bedrockSectionY] = section;
                    continue;
                }

                if (javaPalette instanceof SingletonPalette) {

                    int javaId = javaPalette.idToState(0);
                    int bedrockId = session.getBlockMappings().getBedrockBlockId(javaId);
                    BlockStorage blockStorage = new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(bedrockId));

                    if (BlockRegistries.WATERLOGGED.get().get(javaId)) {
                        BlockStorage waterlogged = new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(session.getBlockMappings().getBedrockWater().getRuntimeId()));
                        sections[bedrockSectionY] = new GeyserChunkSection(new BlockStorage[] {blockStorage, waterlogged}, subChunkIndex);
                    } else {
                        sections[bedrockSectionY] = new GeyserChunkSection(new BlockStorage[] {blockStorage}, subChunkIndex);
                    }

                    continue;
                }

                IntList bedrockPalette = new IntArrayList(javaPalette.size());
                int airPaletteId = -1;
                waterloggedPaletteIds.clear();
                bedrockOnlyBlockEntityIds.clear();


                for (int i = 0; i < javaPalette.size(); i++) {
                    int javaId = javaPalette.idToState(i);
                    bedrockPalette.add(session.getBlockMappings().getBedrockBlockId(javaId));

                    if (BlockRegistries.WATERLOGGED.get().get(javaId)) {
                        waterloggedPaletteIds.set(i);
                    }

                    if (javaId == Block.JAVA_AIR_ID) {
                        airPaletteId = i;
                    }



                    BlockState state = BlockState.of(javaId);
                    if (state.block() instanceof BedrockChunkWantsBlockEntityTag) {
                        bedrockOnlyBlockEntityIds.set(i);
                    }
                }




                if (!bedrockOnlyBlockEntityIds.isEmpty()) {
                    for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                        int paletteId = javaData.get(yzx);
                        if (bedrockOnlyBlockEntityIds.get(paletteId)) {
                            BlockState state = BlockState.of(javaPalette.idToState(paletteId));
                            bedrockBlockEntities.add(((BedrockChunkWantsBlockEntityTag) state.block()).createTag(session,
                                    Vector3i.from((packet.getX() << 4) + (yzx & 0xF), ((sectionY + yOffset) << 4) + ((yzx >> 8) & 0xF), (packet.getZ() << 4) + ((yzx >> 4) & 0xF)),
                                    state
                            ));
                        }
                    }
                }

                int bedrockDataBits = Integer.SIZE - Integer.numberOfLeadingZeros(javaPalette.size());
                BitArray bedrockData = BitArrayVersion.forBitsCeil(bedrockDataBits).createArray(BlockStorage.SIZE);
                BlockStorage layer0 = new BlockStorage(bedrockData, bedrockPalette);
                BlockStorage[] layers;


                if (waterloggedPaletteIds.isEmpty()) {


                    for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                        int paletteId = javaData.get(yzx);
                        int xzy = indexYZXtoXZY(yzx);
                        bedrockData.set(xzy, paletteId);
                    }

                    layers = new BlockStorage[]{ layer0 };
                } else {


                    int[] layer1Data = new int[BlockStorage.SIZE >> 5];
                    for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                        int paletteId = javaData.get(yzx);
                        int xzy = indexYZXtoXZY(yzx);
                        bedrockData.set(xzy, paletteId);

                        if (waterloggedPaletteIds.get(paletteId)) {
                            layer1Data[xzy >> 5] |= 1 << (xzy & 0x1F);
                        }
                    }
                    

                    IntList layer1Palette = IntList.of(
                            session.getBlockMappings().getBedrockAir().getRuntimeId(),
                            session.getBlockMappings().getBedrockWater().getRuntimeId());

                    layers = new BlockStorage[]{ layer0, new BlockStorage(BitArrayVersion.V1.createArray(BlockStorage.SIZE, layer1Data), layer1Palette) };
                }

                sections[bedrockSectionY] = new GeyserChunkSection(layers, subChunkIndex);
            }

            if (!session.getErosionHandler().isActive()) {
                session.getChunkCache().addToCache(packet.getX(), packet.getZ(), javaChunks);
            }

            final int chunkBlockX = packet.getX() << 4;
            final int chunkBlockZ = packet.getZ() << 4;
            for (BlockEntityInfo blockEntity : blockEntities) {
                BlockEntityType type = blockEntity.getType();
                NbtMap tag = blockEntity.getNbt();
                if (type == null) {


                    continue;
                }
                int x = blockEntity.getX();
                int y = blockEntity.getY();
                int z = blockEntity.getZ();


                DataPalette section = javaChunks[(y >> 4) - yOffset];
                BlockState blockState = BlockState.of(section.get(x, y & 0xF, z));



                BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(type);



                if (type == blockState.block().blockEntityType()) {
                    bedrockBlockEntities.add(blockEntityTranslator.getBlockEntityTag(session, type, x + chunkBlockX, y, z + chunkBlockZ, tag, blockState));


                    if (session.getPreferencesCache().showCustomSkulls() && type == BlockEntityType.SKULL && tag != null && tag.containsKey("profile")) {
                        BlockDefinition blockDefinition = SkullBlockEntityTranslator.translateSkull(session, tag, Vector3i.from(x + chunkBlockX, y, z + chunkBlockZ), blockState);
                        if (blockDefinition != null) {
                            int bedrockSectionY = (y >> 4) - (bedrockDimension.minY() >> 4);
                            int subChunkIndex = (y >> 4) + (bedrockDimension.minY() >> 4);
                            if (0 <= bedrockSectionY && bedrockSectionY < maxBedrockSectionY) {

                                GeyserChunkSection bedrockSection = sections[bedrockSectionY];
                                IntList palette = bedrockSection.getBlockStorageArray()[0].getPalette();
                                if (palette instanceof IntImmutableList || palette instanceof IntLists.Singleton) {

                                    bedrockSection = bedrockSection.copy(subChunkIndex);
                                    sections[bedrockSectionY] = bedrockSection;
                                }
                                bedrockSection.setFullBlock(x, y & 0xF, z, 0, blockDefinition.getRuntimeId());
                            }
                        }
                    }
                }
            }


            sectionCount = sections.length - 1;
            while (sectionCount >= 0 && sections[sectionCount] == null) {
                sectionCount--;
            }
            sectionCount++;


            int biomeCount = bedrockDimension.height() >> 4;


            int size = 0;
            for (int i = 0; i < sectionCount; i++) {
                GeyserChunkSection section = sections[i];
                if (section != null) {
                    size += section.estimateNetworkSize();
                } else {
                    size += EMPTY_CHUNK_SECTION_SIZE;
                }
            }
            size += ChunkUtils.EMPTY_BIOME_DATA.length * biomeCount;
            size += 1;
            size += bedrockBlockEntities.size() * 64;


            byteBuf = ByteBufAllocator.DEFAULT.ioBuffer(size);
            for (int i = 0; i < sectionCount; i++) {
                GeyserChunkSection section = sections[i];
                if (section != null) {
                    section.writeToNetwork(byteBuf);
                } else {
                    int subChunkIndex = (i + (bedrockDimension.minY() >> 4));
                    new GeyserChunkSection(EMPTY_BLOCK_STORAGE, subChunkIndex).writeToNetwork(byteBuf);
                }
            }

            int dimensionOffset = bedrockDimension.minY() >> 4;
            for (int i = 0; i < biomeCount; i++) {
                int biomeYOffset = dimensionOffset + i;
                if (biomeYOffset < yOffset) {

                    byteBuf.writeBytes(ChunkUtils.EMPTY_BIOME_DATA);
                    continue;
                }
                if (biomeYOffset >= (chunkSize + yOffset)) {


                    byteBuf.writeByte((127 << 1) | 1);
                    continue;
                }

                BiomeTranslator.toNewBedrockBiome(session, javaBiomes[i + (dimensionOffset - yOffset)]).writeToNetwork(byteBuf);
            }

            byteBuf.writeByte(0);


            NBTOutputStream nbtStream = NbtUtils.createNetworkWriter(new ByteBufOutputStream(byteBuf));
            for (NbtMap blockEntity : bedrockBlockEntities) {
                nbtStream.writeTag(blockEntity);
            }
            payload = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(payload);
        } catch (IOException e) {
            session.getGeyser().getLogger().error("IO error while encoding chunk", e);
            return;
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }

        LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
        levelChunkPacket.setSubChunksLength(sectionCount);
        levelChunkPacket.setCachingEnabled(false);
        levelChunkPacket.setChunkX(packet.getX());
        levelChunkPacket.setChunkZ(packet.getZ());
        levelChunkPacket.setData(Unpooled.wrappedBuffer(payload));
        levelChunkPacket.setDimension(session.getBedrockDimension().bedrockId());
        session.sendUpstreamPacket(levelChunkPacket);

        for (Map.Entry<Vector3i, ItemFrameEntity> entry : session.getItemFrameCache().entrySet()) {
            Vector3i position = entry.getKey();
            if ((position.getX() >> 4) == packet.getX() && (position.getZ() >> 4) == packet.getZ()) {


                entry.getValue().updateBlock(true);
            }
        }
    }
}
