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
import org.geysermc.connector.network.translators.BiomeTranslator;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.chunk.ChunkSection;
import org.geysermc.connector.utils.ChunkUtils;

@Translator(packet = ServerChunkDataPacket.class)
public class JavaChunkDataTranslator extends PacketTranslator<ServerChunkDataPacket> {
    /**
     * Determines if we should process non-full chunks
     */
    private final boolean cacheChunks;

    public JavaChunkDataTranslator() {
        cacheChunks = GeyserConnector.getInstance().getConfig().isCacheChunks();
    }

    @Override
    public void translate(ServerChunkDataPacket packet, GeyserSession session) {
        if (session.isSpawned()) {
            ChunkUtils.updateChunkPosition(session, session.getPlayerEntity().getPosition().toInt());
        }

        if (packet.getColumn().getBiomeData() == null && !cacheChunks) {
            // Non-full chunk without chunk caching
            session.getConnector().getLogger().debug("Not sending non-full chunk because chunk caching is off.");
            return;
        }

        // Merge received column with cache on network thread
        Column mergedColumn = session.getChunkCache().addToCache(packet.getColumn());
        if (mergedColumn == null) { // There were no changes?!?
            return;
        }

        boolean isNonFullChunk = packet.getColumn().getBiomeData() == null;

        GeyserConnector.getInstance().getGeneralThreadPool().execute(() -> {
            try {
                ChunkUtils.ChunkData chunkData = ChunkUtils.translateToBedrock(session, mergedColumn, isNonFullChunk);
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
                    size += (section != null ? section : session.getBlockTranslator().getEmptyChunkSection()).estimateNetworkSize();
                }
                size += 256; // Biomes
                size += 1; // Border blocks
                size += 1; // Extra data length (always 0)
                size += chunkData.getBlockEntities().length * 64; // Conservative estimate of 64 bytes per tile entity

                // Allocate output buffer
                ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(size);
                byte[] payload;
                try {
                    for (int i = 0; i < sectionCount; i++) {
                        ChunkSection section = sections[i];
                        (section != null ? section : session.getBlockTranslator().getEmptyChunkSection()).writeToNetwork(byteBuf);
                    }

                    byteBuf.writeBytes(BiomeTranslator.toBedrockBiome(mergedColumn.getBiomeData())); // Biomes - 256 bytes
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
                levelChunkPacket.setChunkX(mergedColumn.getX());
                levelChunkPacket.setChunkZ(mergedColumn.getZ());
                levelChunkPacket.setData(payload);
                session.sendUpstreamPacket(levelChunkPacket);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
