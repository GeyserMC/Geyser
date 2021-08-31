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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.chunk.ChunkSection;
import org.geysermc.connector.network.translators.world.BiomeTranslator;
import org.geysermc.connector.utils.ChunkUtils;

import static org.geysermc.connector.utils.ChunkUtils.MINIMUM_ACCEPTED_HEIGHT;
import static org.geysermc.connector.utils.ChunkUtils.MINIMUM_ACCEPTED_HEIGHT_OVERWORLD;

@Translator(packet = ServerChunkDataPacket.class)
public class JavaChunkDataTranslator extends PacketTranslator<ServerChunkDataPacket> {
    // Caves and cliffs supports 3D biomes by implementing a very similar palette system to blocks
    private static final boolean NEW_BIOME_WRITE = GeyserConnector.getInstance().getConfig().isExtendedWorldHeight();

    @Override
    public void translate(GeyserSession session, ServerChunkDataPacket packet) {
        if (session.isSpawned()) {
            ChunkUtils.updateChunkPosition(session, session.getPlayerEntity().getPosition().toInt());
        }

        session.getChunkCache().addToCache(packet.getColumn());
        Column column = packet.getColumn();

        // Ensure that, if the player is using lower world heights, the position is not offset
        int yOffset = session.getChunkCache().getChunkMinY();

        GeyserConnector.getInstance().getGeneralThreadPool().execute(() -> {
            try {
                if (session.isClosed()) {
                    return;
                }
                ChunkUtils.ChunkData chunkData = ChunkUtils.translateToBedrock(session, column, yOffset);
                ChunkSection[] sections = chunkData.getSections();

                // Find highest section
                int sectionCount = sections.length - 1;
                while (sectionCount >= 0 && sections[sectionCount] == null) {
                    sectionCount--;
                }
                sectionCount++;

                // Estimate chunk size
                int size = 0;
                for (int i = 0; i < sectionCount; i++) {
                    ChunkSection section = sections[i];
                    size += (section != null ? section : session.getBlockMappings().getEmptyChunkSection()).estimateNetworkSize();
                }
                if (NEW_BIOME_WRITE) {
                    size += ChunkUtils.EMPTY_CHUNK_DATA.length; // Consists only of biome data
                } else {
                    size += 256; // Biomes pre-1.18
                }
                size += 1; // Border blocks
                size += 1; // Extra data length (always 0)
                size += chunkData.getBlockEntities().length * 64; // Conservative estimate of 64 bytes per tile entity

                // Allocate output buffer
                ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(size);
                byte[] payload;
                try {
                    for (int i = 0; i < sectionCount; i++) {
                        ChunkSection section = sections[i];
                        (section != null ? section : session.getBlockMappings().getEmptyChunkSection()).writeToNetwork(byteBuf);
                    }

                    if (NEW_BIOME_WRITE) {
                        // At this point we're dealing with Bedrock chunk sections
                        boolean overworld = session.getChunkCache().isExtendedHeight();
                        int dimensionOffset = (overworld ? MINIMUM_ACCEPTED_HEIGHT_OVERWORLD : MINIMUM_ACCEPTED_HEIGHT) >> 4;
                        for (int i = 0; i < sectionCount; i++) {
                            int biomeYOffset = dimensionOffset + i;
                            if (biomeYOffset < yOffset) {
                                // Ignore this biome section since it goes below the height of the Java world
                                byteBuf.writeBytes(ChunkUtils.EMPTY_BIOME_DATA);
                                continue;
                            }
                            BiomeTranslator.toNewBedrockBiome(session, column.getBiomeData(), i + (dimensionOffset - yOffset)).writeToNetwork(byteBuf);
                        }

                        // As of 1.17.10, Bedrock hardcodes to always read 32 biome sections
                        int remainingEmptyBiomes = 32 - sectionCount;
                        for (int i = 0; i < remainingEmptyBiomes; i++) {
                            byteBuf.writeBytes(ChunkUtils.EMPTY_BIOME_DATA);
                        }
                    } else {
                        byteBuf.writeBytes(BiomeTranslator.toBedrockBiome(session, column.getBiomeData())); // Biomes - 256 bytes
                    }
                    byteBuf.writeByte(0); // Border blocks - Edu edition only
                    VarInts.writeUnsignedInt(byteBuf, 0); // extra data length, 0 for now

                    // Encode tile entities into buffer
                    NBTOutputStream nbtStream = NbtUtils.createNetworkWriter(new ByteBufOutputStream(byteBuf));
                    for (NbtMap blockEntity : chunkData.getBlockEntities()) {
                        nbtStream.writeTag(blockEntity);
                    }

                    // Copy data into byte[], because the protocol lib really likes things that are s l o w
                    byteBuf.readBytes(payload = new byte[byteBuf.readableBytes()]);
                } finally {
                    byteBuf.release(); // Release buffer to allow buffer pooling to be useful
                }

                LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
                levelChunkPacket.setSubChunksLength(sectionCount);
                levelChunkPacket.setCachingEnabled(false);
                levelChunkPacket.setChunkX(column.getX());
                levelChunkPacket.setChunkZ(column.getZ());
                levelChunkPacket.setData(payload);
                session.sendUpstreamPacket(levelChunkPacket);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
