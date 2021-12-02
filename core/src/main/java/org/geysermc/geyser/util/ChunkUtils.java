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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.DoubleTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntLists;
import lombok.experimental.UtilityClass;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.level.block.entity.BedrockOnlyBlockEntity;
import org.geysermc.geyser.level.chunk.BlockStorage;
import org.geysermc.geyser.level.chunk.GeyserChunkSection;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;
import org.geysermc.geyser.registry.BlockRegistries;

import static org.geysermc.geyser.level.block.BlockStateValues.JAVA_AIR_ID;

@UtilityClass
public class ChunkUtils {
    /**
     * The minimum height Bedrock Edition will accept.
     */
    public static final int MINIMUM_ACCEPTED_HEIGHT = 0;
    public static final int MINIMUM_ACCEPTED_HEIGHT_OVERWORLD = -64;
    /**
     * The maximum chunk height Bedrock Edition will accept, from the lowest point to the highest.
     */
    public static final int MAXIMUM_ACCEPTED_HEIGHT = 256;
    public static final int MAXIMUM_ACCEPTED_HEIGHT_OVERWORLD = 384;

    /**
     * An empty subchunk.
     */
    public static final byte[] SERIALIZED_CHUNK_DATA;
    /**
     * An empty chunk that can be safely passed on to a LevelChunkPacket with subcounts set to 0.
     */
    public static final byte[] EMPTY_CHUNK_DATA;
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

        byteBuf = Unpooled.buffer();
        try {
            for (int i = 0; i < 32; i++) {
                byteBuf.writeBytes(EMPTY_BIOME_DATA);
            }

            byteBuf.writeByte(0); // Border

            EMPTY_CHUNK_DATA = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(EMPTY_CHUNK_DATA);
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
            chunkPublisherUpdatePacket.setRadius(session.getRenderDistance() << 4);
            session.sendUpstreamPacket(chunkPublisherUpdatePacket);

            session.setLastChunkPosition(newChunkPos);
        }
    }

    /**
     * Sends a block update to the Bedrock client. If chunk caching is enabled and the platform is not Spigot, this also
     * adds that block to the cache.
     * @param session the Bedrock session to send/register the block to
     * @param blockState the Java block state of the block
     * @param position the position of the block
     */
    public static void updateBlock(GeyserSession session, int blockState, Position position) {
        Vector3i pos = Vector3i.from(position.getX(), position.getY(), position.getZ());
        updateBlock(session, blockState, pos);
    }

    /**
     * Sends a block update to the Bedrock client. If chunk caching is enabled and the platform is not Spigot, this also
     * adds that block to the cache.
     * @param session the Bedrock session to send/register the block to
     * @param blockState the Java block state of the block
     * @param position the position of the block
     */
    public static void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        // Checks for item frames so they aren't tripped up and removed
        ItemFrameEntity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, position);
        if (itemFrameEntity != null) {
            if (blockState == JAVA_AIR_ID) { // Item frame is still present and no block overrides that; refresh it
                itemFrameEntity.updateBlock(true);
                // Still update the chunk cache with the new block
                session.getChunkCache().updateBlock(position.getX(), position.getY(), position.getZ(), blockState);
                return;
            }
            // Otherwise, let's still store our reference to the item frame, but let the new block take precedence for now
        }

        SkullPlayerEntity skull = session.getSkullCache().get(position);
        if (skull != null && blockState != skull.getBlockState()) {
            // Skull is gone
            skull.despawnEntity(position);
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
                data.setData(EMPTY_CHUNK_DATA);
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

    /**
     * Process the minimum and maximum heights for this dimension, and processes the world coordinate scale.
     * This must be done after the player has switched dimensions so we know what their dimension is
     */
    public static void loadDimensionTag(GeyserSession session, CompoundTag dimensionTag) {
        int minY = ((IntTag) dimensionTag.get("min_y")).getValue();
        int maxY = ((IntTag) dimensionTag.get("height")).getValue();
        // Logical height can be ignored probably - seems to be for artificial limits like the Nether.

        if (minY % 16 != 0) {
            throw new RuntimeException("Minimum Y must be a multiple of 16!");
        }
        if (maxY % 16 != 0) {
            throw new RuntimeException("Maximum Y must be a multiple of 16!");
        }

        int dimension = DimensionUtils.javaToBedrock(session.getDimension());
        boolean extendedHeight = dimension == 0;
        session.getChunkCache().setExtendedHeight(extendedHeight);

        // Yell in the console if the world height is too height in the current scenario
        // The constraints change depending on if the player is in the overworld or not, and if experimental height is enabled
        if (minY < (extendedHeight ? MINIMUM_ACCEPTED_HEIGHT_OVERWORLD : MINIMUM_ACCEPTED_HEIGHT)
                || maxY > (extendedHeight ? MAXIMUM_ACCEPTED_HEIGHT_OVERWORLD : MAXIMUM_ACCEPTED_HEIGHT)) {
            session.getGeyser().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.translator.chunk.out_of_bounds",
                    extendedHeight ? MINIMUM_ACCEPTED_HEIGHT_OVERWORLD : MINIMUM_ACCEPTED_HEIGHT,
                    extendedHeight ? MAXIMUM_ACCEPTED_HEIGHT_OVERWORLD : MAXIMUM_ACCEPTED_HEIGHT,
                    session.getDimension()));
        }

        session.getChunkCache().setMinY(minY);
        session.getChunkCache().setHeightY(maxY);

        // Load world coordinate scale for the world border
        double coordinateScale = ((DoubleTag) dimensionTag.get("coordinate_scale")).getValue();
        session.getWorldBorder().setWorldCoordinateScale(coordinateScale);
    }
}
