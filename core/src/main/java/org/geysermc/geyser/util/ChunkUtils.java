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

package org.geysermc.geyser.util;

import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntLists;
import lombok.experimental.UtilityClass;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.chunk.BlockStorage;
import org.geysermc.geyser.level.chunk.GeyserChunkSection;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.level.block.entity.BedrockOnlyBlockEntity;

import static org.geysermc.geyser.level.block.BlockStateValues.JAVA_AIR_ID;

@UtilityClass
public class ChunkUtils {
    /**
     * An empty subchunk.
     */
    public static final byte[] SERIALIZED_CHUNK_DATA;
    public static final byte[] EMPTY_BIOME_DATA;

    static {
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            new GeyserChunkSection(new BlockStorage[0])
                    .writeToNetwork(byteBuf);
            SERIALIZED_CHUNK_DATA = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(SERIALIZED_CHUNK_DATA);
        } finally {
            byteBuf.release();
        }

        byteBuf = Unpooled.buffer();
        try {
            BlockStorage blockStorage = new BlockStorage(SingletonBitArray.INSTANCE, IntLists.singleton(0));
            blockStorage.writeToNetwork(byteBuf);

            EMPTY_BIOME_DATA = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(EMPTY_BIOME_DATA);
        } finally {
            byteBuf.release();
        }
    }

    public static int indexYZXtoXZY(int yzx) {
        return (yzx >> 8) | (yzx & 0x0F0) | ((yzx & 0x00F) << 8);
    }

    public static void updateChunkPosition(GeyserSession session, Vector3i position) {
        Vector2i chunkPos = session.getLastChunkPosition();
        Vector2i newChunkPos = Vector2i.from(position.getX() >> 4, position.getZ() >> 4);

        if (chunkPos == null || !chunkPos.equals(newChunkPos)) {
            NetworkChunkPublisherUpdatePacket chunkPublisherUpdatePacket = new NetworkChunkPublisherUpdatePacket();
            chunkPublisherUpdatePacket.setPosition(position);
            chunkPublisherUpdatePacket.setRadius(session.getServerRenderDistance() << 4);
            session.sendUpstreamPacket(chunkPublisherUpdatePacket);

            session.setLastChunkPosition(newChunkPos);
        }
    }

    /**
     * Sends a block update to the Bedrock client. If the platform is not Spigot, this also
     * adds that block to the cache.
     * @param session the Bedrock session to send/register the block to
     * @param blockState the Java block state of the block
     * @param position the position of the block
     */
    public static void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        updateBlockClientSide(session, blockState, position);
        session.getChunkCache().updateBlock(position.getX(), position.getY(), position.getZ(), blockState);
    }

    /**
     * Updates a block, but client-side only.
     */
    public static void updateBlockClientSide(GeyserSession session, int blockState, Vector3i position) {
        // Checks for item frames so they aren't tripped up and removed
        ItemFrameEntity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, position);
        if (itemFrameEntity != null) {
            if (blockState == JAVA_AIR_ID) { // Item frame is still present and no block overrides that; refresh it
                itemFrameEntity.updateBlock(true);
                // Still update the chunk cache with the new block if updateBlock is called
                return;
            }
            // Otherwise, let's still store our reference to the item frame, but let the new block take precedence for now
        }

        if (BlockStateValues.getSkullVariant(blockState) == -1) {
            // Skull is gone
            session.getSkullCache().removeSkull(position);
        }

        // Prevent moving_piston from being placed
        // It's used for extending piston heads, but it isn't needed on Bedrock and causes pistons to flicker
        if (!BlockStateValues.isMovingPiston(blockState)) {
            int blockId = session.getBlockMappings().getBedrockBlockId(blockState);

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
            if (BlockRegistries.WATERLOGGED.get().contains(blockState)) {
                waterPacket.setRuntimeId(session.getBlockMappings().getBedrockWaterId());
            } else {
                waterPacket.setRuntimeId(session.getBlockMappings().getBedrockAirId());
            }
            session.sendUpstreamPacket(waterPacket);
        }

        BlockStateValues.getLecternBookStates().handleBlockChange(session, blockState, position);

        // Iterates through all Bedrock-only block entity translators and determines if a manual block entity packet
        // needs to be sent
        for (BedrockOnlyBlockEntity bedrockOnlyBlockEntity : BlockEntityUtils.BEDROCK_ONLY_BLOCK_ENTITIES) {
            if (bedrockOnlyBlockEntity.isBlock(blockState)) {
                // Flower pots are block entities only in Bedrock and are not updated anywhere else like note blocks
                bedrockOnlyBlockEntity.updateBlock(session, blockState, position);
                break; //No block will be a part of two classes
            }
        }
    }

    public static void sendEmptyChunk(GeyserSession session, int chunkX, int chunkZ, boolean forceUpdate) {
        BedrockDimension bedrockDimension = session.getChunkCache().getBedrockDimension();
        int bedrockSubChunkCount = bedrockDimension.height() >> 4;

        byte[] payload;

        // Allocate output buffer
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(ChunkUtils.EMPTY_BIOME_DATA.length * bedrockSubChunkCount + 1); // Consists only of biome data and border blocks
        try {
            byteBuf.writeBytes(EMPTY_BIOME_DATA);
            for (int i = 1; i < bedrockSubChunkCount; i++) {
                byteBuf.writeByte((127 << 1) | 1);
            }

            byteBuf.writeByte(0); // Border blocks - Edu edition only

            payload = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(payload);
        } finally {
            byteBuf.release();
        }

        LevelChunkPacket data = new LevelChunkPacket();
        data.setChunkX(chunkX);
        data.setChunkZ(chunkZ);
        data.setSubChunksLength(0);
        data.setData(payload);
        data.setCachingEnabled(false);
        session.sendUpstreamPacket(data);

        if (forceUpdate) {
            Vector3i pos = Vector3i.from(chunkX << 4, 80, chunkZ << 4);
            UpdateBlockPacket blockPacket = new UpdateBlockPacket();
            blockPacket.setBlockPosition(pos);
            blockPacket.setDataLayer(0);
            blockPacket.setRuntimeId(1);
            session.sendUpstreamPacket(blockPacket);
        }
    }

    public static void sendEmptyChunks(GeyserSession session, Vector3i position, int radius, boolean forceUpdate) {
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                sendEmptyChunk(session, chunkX + x, chunkZ + z, forceUpdate);
            }
        }
    }

    /**
     * Process the minimum and maximum heights for this dimension, and processes the world coordinate scale.
     * This must be done after the player has switched dimensions so we know what their dimension is
     */
    public static void loadDimension(GeyserSession session) {
        JavaDimension dimension = session.getDimensions().get(session.getDimension());
        session.setDimensionType(dimension);
        int minY = dimension.minY();
        int maxY = dimension.maxY();

        if (minY % 16 != 0) {
            throw new RuntimeException("Minimum Y must be a multiple of 16!");
        }
        if (maxY % 16 != 0) {
            throw new RuntimeException("Maximum Y must be a multiple of 16!");
        }

        BedrockDimension bedrockDimension = session.getChunkCache().getBedrockDimension();
        // Yell in the console if the world height is too height in the current scenario
        // The constraints change depending on if the player is in the overworld or not, and if experimental height is enabled
        // (Ignore this for the Nether. We can't change that at the moment without the workaround. :/ )
        if (minY < bedrockDimension.minY() || (bedrockDimension.doUpperHeightWarn() && maxY > bedrockDimension.height())) {
            session.getGeyser().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.translator.chunk.out_of_bounds",
                    String.valueOf(bedrockDimension.minY()),
                    String.valueOf(bedrockDimension.height()),
                    session.getDimension()));
        }

        session.getChunkCache().setMinY(minY);
        session.getChunkCache().setHeightY(maxY);

        session.getWorldBorder().setWorldCoordinateScale(dimension.worldCoordinateScale());
    }
}
