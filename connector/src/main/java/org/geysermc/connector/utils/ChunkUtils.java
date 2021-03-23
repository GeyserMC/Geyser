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
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Data;
import lombok.experimental.UtilityClass;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.entity.player.SkullPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.translators.LecternInventoryTranslator;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BedrockOnlyBlockEntity;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.RequiresBlockState;
import org.geysermc.connector.network.translators.world.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.network.translators.world.chunk.BlockStorage;
import org.geysermc.connector.network.translators.world.chunk.ChunkSection;
import org.geysermc.connector.network.translators.world.chunk.bitarray.BitArray;
import org.geysermc.connector.network.translators.world.chunk.bitarray.BitArrayVersion;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.geysermc.connector.network.translators.world.block.BlockTranslator.JAVA_AIR_ID;

@UtilityClass
public class ChunkUtils {
    /**
     * Temporarily stores positions of BlockState values that are needed for certain block entities actively.
     * Not used if cache chunks is enabled
     */
    public static final Object2IntMap<Position> CACHED_BLOCK_ENTITIES = new Object2IntOpenHashMap<>();

    private static int indexYZXtoXZY(int yzx) {
        return (yzx >> 8) | (yzx & 0x0F0) | ((yzx & 0x00F) << 8);
    }

    public static ChunkData translateToBedrock(GeyserSession session, Column column, boolean isNonFullChunk) {
        Chunk[] javaSections = column.getChunks();
        ChunkSection[] sections = new ChunkSection[javaSections.length];

        // Temporarily stores compound tags of Bedrock-only block entities
        List<NbtMap> bedrockOnlyBlockEntities = new ArrayList<>();

        BitSet waterloggedPaletteIds = new BitSet();
        BitSet pistonOrFlowerPaletteIds = new BitSet();

        boolean worldManagerHasMoreBlockDataThanCache = session.getConnector().getWorldManager().hasOwnChunkCache();

        // If the received packet was a full chunk update, null sections in the chunk are guaranteed to also be null in the world manager
        boolean shouldCheckWorldManagerOnMissingSections = isNonFullChunk && worldManagerHasMoreBlockDataThanCache;
        Chunk temporarySection = null;

        for (int sectionY = 0; sectionY < javaSections.length; sectionY++) {
            Chunk javaSection = javaSections[sectionY];

            // Section is null, the cache will not contain anything of use
            if (javaSection == null) {
                // The column parameter contains all data currently available from the cache. If the chunk is null and the world manager
                // reports the ability to access more data than the cache, attempt to fetch from the world manager instead.
                if (shouldCheckWorldManagerOnMissingSections) {
                    // Ensure that temporary chunk is set
                    if (temporarySection == null) {
                        temporarySection = new Chunk();
                    }

                    // Read block data in section
                    session.getConnector().getWorldManager().getBlocksInSection(session, column.getX(), sectionY, column.getZ(), temporarySection);

                    if (temporarySection.isEmpty()) {
                        // The world manager only contains air for the given section
                        // We can leave temporarySection as-is to allow it to potentially be re-used for later sections
                        continue;
                    } else {
                        javaSection = temporarySection;

                        // Section contents have been modified, we can't re-use it
                        temporarySection = null;
                    }
                } else {
                    continue;
                }
            }

            // No need to encode an empty section...
            if (javaSection.isEmpty()) {
                continue;
            }

            Palette javaPalette = javaSection.getPalette();
            BitStorage javaData = javaSection.getStorage();

            if (javaPalette instanceof GlobalPalette) {
                // As this is the global palette, simply iterate through the whole chunk section once
                ChunkSection section = new ChunkSection(session.getBlockTranslator().getBedrockAirId());
                for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
                    int javaId = javaData.get(yzx);
                    int bedrockId = session.getBlockTranslator().getBedrockBlockId(javaId);
                    int xzy = indexYZXtoXZY(yzx);
                    section.getBlockStorageArray()[0].setFullBlock(xzy, bedrockId);

                    if (BlockTranslator.isWaterlogged(javaId)) {
                        section.getBlockStorageArray()[1].setFullBlock(xzy, session.getBlockTranslator().getBedrockWaterId());
                    }

                    // Check if block is piston or flower to see if we'll need to create additional block entities, as they're only block entities in Bedrock
                    if (BlockStateValues.getFlowerPotValues().containsKey(javaId) || BlockStateValues.getPistonValues().containsKey(javaId)) {
                        bedrockOnlyBlockEntities.add(BedrockOnlyBlockEntity.getTag(session,
                                Vector3i.from((column.getX() << 4) + (yzx & 0xF), (sectionY << 4) + ((yzx >> 8) & 0xF), (column.getZ() << 4) + ((yzx >> 4) & 0xF)),
                                javaId
                        ));
                    }
                }
                sections[sectionY] = section;
                continue;
            }

            IntList bedrockPalette = new IntArrayList(javaPalette.size());
            waterloggedPaletteIds.clear();
            pistonOrFlowerPaletteIds.clear();

            // Iterate through palette and convert state IDs to Bedrock, doing some additional checks as we go
            for (int i = 0; i < javaPalette.size(); i++) {
                int javaId = javaPalette.idToState(i);
                bedrockPalette.add(session.getBlockTranslator().getBedrockBlockId(javaId));

                if (BlockTranslator.isWaterlogged(javaId)) {
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
                                Vector3i.from((column.getX() << 4) + (yzx & 0xF), (sectionY << 4) + ((yzx >> 8) & 0xF), (column.getZ() << 4) + ((yzx >> 4) & 0xF)),
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
                layer1Palette.add(session.getBlockTranslator().getBedrockAirId()); // Air - see BlockStorage's constructor for more information
                layer1Palette.add(session.getBlockTranslator().getBedrockWaterId());

                layers = new BlockStorage[]{ layer0, new BlockStorage(BitArrayVersion.V1.createArray(BlockStorage.SIZE, layer1Data), layer1Palette) };
            }

            sections[sectionY] = new ChunkSection(layers);
        }

        CompoundTag[] blockEntities = column.getTileEntities();
        NbtMap[] bedrockBlockEntities = new NbtMap[blockEntities.length + bedrockOnlyBlockEntities.size()];
        int i = 0;
        while (i < blockEntities.length) {
            CompoundTag tag = blockEntities[i];
            String tagName;
            if (tag.contains("id")) {
                tagName = (String) tag.get("id").getValue();
            } else {
                tagName = "Empty";
                // Sometimes legacy tags have their ID be a StringTag with empty value
                for (Tag subTag : tag) {
                    if (subTag instanceof StringTag) {
                        StringTag stringTag = (StringTag) subTag;
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
            Position pos = new Position((int) tag.get("x").getValue(), (int) tag.get("y").getValue(), (int) tag.get("z").getValue());

            // Get Java blockstate ID from block entity position
            int blockState = 0;
            Chunk section = column.getChunks()[pos.getY() >> 4];
            if (section != null) {
                blockState = section.get(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
            }

            if (tagName.equals("minecraft:lectern") && BlockStateValues.getLecternBookStates().get(blockState)) {
                // If getLecternBookStates is false, let's just treat it like a normal block entity
                bedrockBlockEntities[i] = session.getConnector().getWorldManager().getLecternDataAt(session, pos.getX(), pos.getY(), pos.getZ(), true);
                i++;
                continue;
            }

            BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(id);
            bedrockBlockEntities[i] = blockEntityTranslator.getBlockEntityTag(tagName, tag, blockState);

            // Check for custom skulls
            if (SkullBlockEntityTranslator.ALLOW_CUSTOM_SKULLS && tag.contains("SkullOwner")) {
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
        long frameEntityId = ItemFrameEntity.getItemFrameEntityId(session, position);
        if (frameEntityId != -1) {
            // TODO: Very occasionally the item frame doesn't sync up when destroyed
            Entity entity = session.getEntityCache().getEntityByJavaId(frameEntityId);
            if (blockState == JAVA_AIR_ID && entity != null) { // Item frame is still present and no block overrides that; refresh it
                ((ItemFrameEntity) entity).updateBlock(session);
                return;
            }

            // Otherwise the item frame is gone
            if (entity != null) {
                session.getEntityCache().removeEntity(entity, false);
            } else {
                ItemFrameEntity.removePosition(session, position);
            }
        }

        SkullPlayerEntity skull = session.getSkullCache().get(position);
        if (skull != null && blockState != skull.getBlockState()) {
            // Skull is gone
            skull.despawnEntity(session, position);
        }

        int blockId = session.getBlockTranslator().getBedrockBlockId(blockState);

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
        if (BlockTranslator.isWaterlogged(blockState)) {
            waterPacket.setRuntimeId(session.getBlockTranslator().getBedrockWaterId());
        } else {
            waterPacket.setRuntimeId(session.getBlockTranslator().getBedrockAirId());
        }
        session.sendUpstreamPacket(waterPacket);

        BlockStateValues.getLecternBookStates().compute(blockState, (key, newLecternHasBook) -> {
            // Determine if this block is a lectern
            if (newLecternHasBook != null) {
                boolean lecternCachedHasBook = session.getLecternCache().contains(position);
                if (!session.getConnector().getWorldManager().shouldExpectLecternHandled() && lecternCachedHasBook != newLecternHasBook) {
                    // Refresh the block entirely - it either has a book or no longer has a book
                    NbtMap newLecternTag;
                    if (newLecternHasBook) {
                        newLecternTag = session.getConnector().getWorldManager().getLecternDataAt(session, position.getX(), position.getY(), position.getZ(), false);
                    } else {
                        session.getLecternCache().remove(position);
                        newLecternTag = LecternInventoryTranslator.getBaseLecternTag(position.getX(), position.getY(), position.getZ(), 0).build();
                    }
                    BlockEntityUtils.updateBlockEntity(session, newLecternTag, position);
                } else {
                    // As of right now, no tag can be added asynchronously
                    session.getConnector().getWorldManager().getLecternDataAt(session, position.getX(), position.getY(), position.getZ(), false);
                }
            } else {
                // Lectern has been destroyed, if it existed
                session.getLecternCache().remove(position);
            }
            return newLecternHasBook;
        });

        // Since Java stores bed colors/skull information as part of the namespaced ID and Bedrock stores it as a tag
        // This is the only place I could find that interacts with the Java block state and block updates
        // Iterates through all block entity translators and determines if the block state needs to be saved
        for (RequiresBlockState requiresBlockState : BlockEntityTranslator.REQUIRES_BLOCK_STATE_LIST) {
            if (requiresBlockState.isBlock(blockState)) {
                // Flower pots are block entities only in Bedrock and are not updated anywhere else like note blocks
                if (requiresBlockState instanceof BedrockOnlyBlockEntity) {
                    ((BedrockOnlyBlockEntity) requiresBlockState).updateBlock(session, blockState, position);
                    break;
                }
                if (!session.getConnector().getConfig().isCacheChunks()) {
                    // Blocks aren't saved to a chunk cache; resort to this smaller cache
                    CACHED_BLOCK_ENTITIES.put(new Position(position.getX(), position.getY(), position.getZ()), blockState);
                }
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
                data.setData(session.getBlockTranslator().getEmptyChunkData());
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

    @Data
    public static final class ChunkData {
        private final ChunkSection[] sections;

        private final NbtMap[] blockEntities;
    }
}
