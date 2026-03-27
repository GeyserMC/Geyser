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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Translator(packet = ClientboundLevelChunkWithLightPacket.class)
public class JavaLevelChunkWithLightTranslator extends PacketTranslator<ClientboundLevelChunkWithLightPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLevelChunkWithLightPacket packet) {
        if (session.isSpawned()) {
            ChunkUtils.updateChunkPosition(session, session.getPlayerEntity().position().toInt());
        }

        // Ensure that, if the player is using lower world heights, the position is not offset
        int yOffset = session.getChunkCache().getChunkMinY();
        int chunkSize = session.getChunkCache().getChunkHeightY();

        DataPalette[] javaChunks = new DataPalette[chunkSize];
        DataPalette[] javaBiomes = new DataPalette[chunkSize];

        final Map<Integer, List<BlockEntityInfo>> blockEntitiesProto = Arrays.stream(packet.getBlockEntities())
            .collect(Collectors.groupingBy(blockEntity -> (blockEntity.getY() >> 4) - yOffset));
        final BlockEntityInfo[][] blockEntities = new BlockEntityInfo[chunkSize][];

        BedrockDimension bedrockDimension = session.getBedrockDimension();
        int maxBedrockSectionY = (bedrockDimension.height() >> 4) - 1;

        int sectionCount = 0;
        byte[] payload;
        ByteBuf byteBuf = null;

        // calculate the difference between the java dimension minY and the bedrock dimension minY as
        // the java chunk sections may need to be placed higher up in the bedrock chunk section array
        int sectionCountDiff = yOffset - (bedrockDimension.minY() >> 4);

        try {
            ByteBuf in = Unpooled.wrappedBuffer(packet.getChunkData());
            for (int sectionY = 0; sectionY < chunkSize; sectionY++) {
                ChunkSection javaSection = MinecraftTypes.readChunkSection(in, BlockRegistries.BLOCK_STATES.get().size(),
                    session.getRegistryCache().registry(JavaRegistries.BIOME).size());
                javaChunks[sectionY] = javaSection.getBlockData();
                javaBiomes[sectionY] = javaSection.getBiomeData();
                blockEntities[sectionY] = blockEntitiesProto.getOrDefault(sectionY, Collections.emptyList()).toArray(new BlockEntityInfo[0]);

                int bedrockSectionY = sectionY + sectionCountDiff;
                if (bedrockSectionY < 0 || maxBedrockSectionY < bedrockSectionY) {
                    // Ignore this chunk section since it goes outside the bounds accepted by the Bedrock client
                    continue;
                }

                // No need to encode an empty section...
                if (javaSection.isBlockCountEmpty()) {
                    continue;
                }

                sectionCount = bedrockSectionY + 1;
            }

            if (!session.getErosionHandler().isActive()) {
                session.getChunkCache().addToCache(packet.getX(), packet.getZ(), javaChunks, blockEntities, packet.getLightData());
            }

            // As of 1.18.30, the amount of biomes read is dependent on how high Bedrock thinks the dimension is
            int biomeCount = bedrockDimension.height() >> 4;

            // Allocate output buffer
            byteBuf = ByteBufAllocator.DEFAULT.ioBuffer(ChunkUtils.EMPTY_BIOME_DATA.length * biomeCount + 1);

            int dimensionOffset = bedrockDimension.minY() >> 4;
            for (int i = 0; i < biomeCount; i++) {
                int biomeYOffset = dimensionOffset + i;
                if (biomeYOffset < yOffset) {
                    // Ignore this biome section since it goes below the height of the Java world
                    byteBuf.writeBytes(ChunkUtils.EMPTY_BIOME_DATA);
                    continue;
                }
                if (biomeYOffset >= (chunkSize + yOffset)) {
                    // This biome section goes above the height of the Java world
                    // The byte written here is a header that says to carry on the biome data from the previous chunk
                    byteBuf.writeByte((127 << 1) | 1);
                    continue;
                }

                BiomeTranslator.toNewBedrockBiome(session, javaBiomes[i + (dimensionOffset - yOffset)]).writeToNetwork(byteBuf);
            }

            byteBuf.writeByte(0); // Border blocks - Edu edition only

            payload = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(payload);
        } finally {
            if (byteBuf != null) {
                byteBuf.release(); // Release buffer to allow buffer pooling to be useful
            }
        }

        LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
        levelChunkPacket.setChunkX(packet.getX());
        levelChunkPacket.setChunkZ(packet.getZ());
        levelChunkPacket.setSubChunkLimit(sectionCount - 1);
        levelChunkPacket.setRequestSubChunks(true);
        levelChunkPacket.setData(Unpooled.wrappedBuffer(payload));
        levelChunkPacket.setDimension(session.getBedrockDimension().bedrockId());
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
}
