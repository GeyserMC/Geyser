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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.BitStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.GlobalPalette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.Palette;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.experimental.UtilityClass;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.entity.player.SkullPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.block.entity.BedrockOnlyBlockEntity;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.chunk.BlockStorage;
import org.geysermc.connector.network.translators.world.chunk.ChunkSection;
import org.geysermc.connector.network.translators.world.chunk.bitarray.BitArray;
import org.geysermc.connector.network.translators.world.chunk.bitarray.BitArrayVersion;
import org.geysermc.connector.registry.BlockRegistries;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.geysermc.connector.network.translators.world.block.BlockStateValues.JAVA_AIR_ID;

@UtilityClass
public class ChunkUtils {
    /**
     * The minimum height Bedrock Edition will accept.
     */
    public static final int MINIMUM_ACCEPTED_HEIGHT = 0;
    private static final int CAVES_AND_CLIFFS_MINIMUM_HEIGHT = -64;
    public static final int MINIMUM_ACCEPTED_HEIGHT_OVERWORLD = GeyserConnector.getInstance().getConfig().isExtendedWorldHeight() ?
            CAVES_AND_CLIFFS_MINIMUM_HEIGHT  : MINIMUM_ACCEPTED_HEIGHT;
    /**
     * The maximum chunk height Bedrock Edition will accept, from the lowest point to the highest.
     */
    private static final int MAXIMUM_ACCEPTED_HEIGHT = 256;
    private static final int CAVES_AND_CLIFFS_MAXIMUM_HEIGHT = 384;
    private static final int MAXIMUM_ACCEPTED_HEIGHT_OVERWORLD = GeyserConnector.getInstance().getConfig().isExtendedWorldHeight() ?
            CAVES_AND_CLIFFS_MAXIMUM_HEIGHT : MAXIMUM_ACCEPTED_HEIGHT;

    public static final byte[] EMPTY_CHUNK_DATA;
    public static final byte[] EMPTY_BIOME_DATA;

    static {
        if (GeyserConnector.getInstance().getConfig().isExtendedWorldHeight()) {
            ByteBuf byteBuf = Unpooled.buffer();
            try {
                BlockStorage blockStorage = new BlockStorage(0);
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
        } else {
            EMPTY_BIOME_DATA = null; // Unused
            EMPTY_CHUNK_DATA = new byte[257]; // 256 bytes for biomes, one for borders
        }
    }

    private static int indexYZXtoXZY(int yzx) {
        return (yzx >> 8) | (yzx & 0x0F0) | ((yzx & 0x00F) << 8);
    }

    public static ChunkData translateToBedrock(GeyserSession session, Column column, int yOffset) {
        Chunk[] javaSections = column.getChunks();
        ChunkSection[] sections = new ChunkSection[javaSections.length - yOffset];

        // Temporarily stores compound tags of Bedrock-only block entities
        List<NbtMap> bedrockOnlyBlockEntities = new ArrayList<>();

        BitSet waterloggedPaletteIds = new BitSet();
        BitSet pistonOrFlowerPaletteIds = new BitSet();

        boolean overworld = session.getChunkCache().isExtendedHeight();
        int maxBedrockSectionY = ((overworld ? MAXIMUM_ACCEPTED_HEIGHT_OVERWORLD : MAXIMUM_ACCEPTED_HEIGHT) >> 4) - 1;

        for (int sectionY = 0; sectionY < javaSections.length; sectionY++) {
            int bedrockSectionY = sectionY + (yOffset - ((overworld ? MINIMUM_ACCEPTED_HEIGHT_OVERWORLD : MINIMUM_ACCEPTED_HEIGHT) >> 4));
            if (bedrockSectionY < 0 || maxBedrockSectionY < bedrockSectionY) {
                // Ignore this chunk section since it goes outside the bounds accepted by the Bedrock client
                continue;
            }

            Chunk javaSection = javaSections[sectionY];

            // No need to encode an empty section...
            if (javaSection == null || javaSection.isEmpty()) {
                continue;
            }

            Palette javaPalette = javaSection.getPalette();
            BitStorage javaData = javaSection.getStorage();

            if (javaPalette instanceof GlobalPalette) {
                // As this is the global palette, simply iterate through the whole chunk section once
                ChunkSection section = new ChunkSection(session.getBlockMappings().getBedrockAirId());
                for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                    int javaId = javaData.get(yzx);
                    int bedrockId = session.getBlockMappings().getBedrockBlockId(javaId);
                    int xzy = indexYZXtoXZY(yzx);
                    section.getBlockStorageArray()[0].setFullBlock(xzy, bedrockId);

                    if (BlockRegistries.WATERLOGGED.get().contains(javaId)) {
                        section.getBlockStorageArray()[1].setFullBlock(xzy, session.getBlockMappings().getBedrockWaterId());
                    }

                    // Check if block is piston or flower to see if we'll need to create additional block entities, as they're only block entities in Bedrock
                    if (BlockStateValues.getFlowerPotValues().containsKey(javaId) || BlockStateValues.getPistonValues().containsKey(javaId)) {
                        bedrockOnlyBlockEntities.add(BedrockOnlyBlockEntity.getTag(session,
                                Vector3i.from((column.getX() << 4) + (yzx & 0xF), ((sectionY + yOffset) << 4) + ((yzx >> 8) & 0xF), (column.getZ() << 4) + ((yzx >> 4) & 0xF)),
                                javaId
                        ));
                    }
                }
                sections[bedrockSectionY] = section;
                continue;
            }

            IntList bedrockPalette = new IntArrayList(javaPalette.size());
            waterloggedPaletteIds.clear();
            pistonOrFlowerPaletteIds.clear();

            // Iterate through palette and convert state IDs to Bedrock, doing some additional checks as we go
            for (int i = 0; i < javaPalette.size(); i++) {
                int javaId = javaPalette.idToState(i);
                bedrockPalette.add(session.getBlockMappings().getBedrockBlockId(javaId));

                if (BlockRegistries.WATERLOGGED.get().contains(javaId)) {
                    waterloggedPaletteIds.set(i);
                }

                // Check if block is piston or flower to see if we'll need to create additional block entities, as they're only block entities in Bedrock
                if (BlockStateValues.getFlowerPotValues().containsKey(javaId) || BlockStateValues.getPistonValues().containsKey(javaId)) {
                    pistonOrFlowerPaletteIds.set(i);
                }
            }

            // Add Bedrock-exclusive block entities
            // We only if the palette contained any blocks that are Bedrock-exclusive block entities to avoid iterating through the whole block data
            // for no reason, as most sections will not contain any pistons or flower pots
            if (!pistonOrFlowerPaletteIds.isEmpty()) {
                for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                    int paletteId = javaData.get(yzx);
                    if (pistonOrFlowerPaletteIds.get(paletteId)) {
                        bedrockOnlyBlockEntities.add(BedrockOnlyBlockEntity.getTag(session,
                                Vector3i.from((column.getX() << 4) + (yzx & 0xF), ((sectionY + yOffset) << 4) + ((yzx >> 8) & 0xF), (column.getZ() << 4) + ((yzx >> 4) & 0xF)),
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
                IntList layer1Palette = new IntArrayList(2);
                layer1Palette.add(session.getBlockMappings().getBedrockAirId()); // Air - see BlockStorage's constructor for more information
                layer1Palette.add(session.getBlockMappings().getBedrockWaterId());

                layers = new BlockStorage[]{ layer0, new BlockStorage(BitArrayVersion.V1.createArray(BlockStorage.SIZE, layer1Data), layer1Palette) };
            }

            sections[bedrockSectionY] = new ChunkSection(layers);
        }

        CompoundTag[] blockEntities = column.getTileEntities();
        NbtMap[] bedrockBlockEntities = new NbtMap[blockEntities.length + bedrockOnlyBlockEntities.size()];
        int i = 0;
        while (i < blockEntities.length) {
            CompoundTag tag = blockEntities[i];
            String tagName;
            Tag idTag = tag.get("id");
            if (idTag != null) {
                tagName = (String) idTag.getValue();
            } else {
                tagName = "Empty";
                // Sometimes legacy tags have their ID be a StringTag with empty value
                for (Tag subTag : tag) {
                    if (subTag instanceof StringTag stringTag) {
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
            int x = (int) tag.get("x").getValue();
            int y = (int) tag.get("y").getValue();
            int z = (int) tag.get("z").getValue();

            // Get Java blockstate ID from block entity position
            int blockState = 0;
            Chunk section = column.getChunks()[(y >> 4) - yOffset];
            if (section != null) {
                blockState = section.get(x & 0xF, y & 0xF, z & 0xF);
            }

            if (tagName.equals("minecraft:lectern") && BlockStateValues.getLecternBookStates().get(blockState)) {
                // If getLecternBookStates is false, let's just treat it like a normal block entity
                bedrockBlockEntities[i] = session.getConnector().getWorldManager().getLecternDataAt(session, x, y, z, true);
                i++;
                continue;
            }

            BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(id);
            bedrockBlockEntities[i] = blockEntityTranslator.getBlockEntityTag(tagName, tag, blockState);

            // Check for custom skulls
            if (session.getPreferencesCache().showCustomSkulls() && tag.contains("SkullOwner")) {
                SkullBlockEntityTranslator.spawnPlayer(session, tag, blockState);
            }
            i++;
        }

        // Append Bedrock-exclusive block entities to output array
        for (NbtMap tag : bedrockOnlyBlockEntities) {
            bedrockBlockEntities[i] = tag;
            i++;
        }

        return new ChunkData(sections, bedrockBlockEntities);
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
                itemFrameEntity.updateBlock(session);
                // Still update the chunk cache with the new block
                session.getChunkCache().updateBlock(position.getX(), position.getY(), position.getZ(), blockState);
                return;
            }
            // Otherwise, let's still store our reference to the item frame, but let the new block take precedence for now
        }

        SkullPlayerEntity skull = session.getSkullCache().get(position);
        if (skull != null && blockState != skull.getBlockState()) {
            // Skull is gone
            skull.despawnEntity(session, position);
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
        boolean extendedHeight = dimension == 0 && session.getConnector().getConfig().isExtendedWorldHeight();
        session.getChunkCache().setExtendedHeight(extendedHeight);

        // Yell in the console if the world height is too height in the current scenario
        // The constraints change depending on if the player is in the overworld or not, and if experimental height is enabled
        if (minY < (extendedHeight ? MINIMUM_ACCEPTED_HEIGHT_OVERWORLD : MINIMUM_ACCEPTED_HEIGHT)
                || maxY > (extendedHeight ? MAXIMUM_ACCEPTED_HEIGHT_OVERWORLD : MAXIMUM_ACCEPTED_HEIGHT)) {
            if (minY >= CAVES_AND_CLIFFS_MINIMUM_HEIGHT && maxY <= CAVES_AND_CLIFFS_MAXIMUM_HEIGHT && dimension == 0 && !session.getConnector().getConfig().isExtendedWorldHeight()) {
                // This dimension uses heights that would be fixed by enabling the experimental toggle
                session.getConnector().getLogger().warning(
                        LanguageUtils.getLocaleStringLog("geyser.network.translator.chunk.out_of_bounds.caves_and_cliffs",
                                "extended-world-height"));
            } else {
                session.getConnector().getLogger().warning(LanguageUtils.getLocaleStringLog("geyser.network.translator.chunk.out_of_bounds",
                        extendedHeight ? MINIMUM_ACCEPTED_HEIGHT_OVERWORLD : MINIMUM_ACCEPTED_HEIGHT,
                        extendedHeight ? MAXIMUM_ACCEPTED_HEIGHT_OVERWORLD : MAXIMUM_ACCEPTED_HEIGHT,
                        session.getDimension()));
            }
        }

        session.getChunkCache().setMinY(minY);
        session.getChunkCache().setHeightY(maxY);

        // Load world coordinate scale for the world border
        double coordinateScale = ((DoubleTag) dimensionTag.get("coordinate_scale")).getValue();
        session.getWorldBorder().setWorldCoordinateScale(coordinateScale);
    }

    public record ChunkData(ChunkSection[] sections, NbtMap[] blockEntities) {
    }
}
