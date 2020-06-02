/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.object.HangingDirection;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

import java.util.concurrent.TimeUnit;

/**
 * Item frames are an entity in Java but a block entity in Bedrock.
 */
public class ItemFrameEntity extends Entity {

    /**
     * Used for getting the Bedrock block position.
     * Blocks deal with integers whereas entities deal with floats.
     */
    private final Vector3i bedrockPosition;
    /**
     * Specific block 'state' we are emulating in Bedrock.
     */
    private final int bedrockRuntimeId;
    /**
     * Rotation of item in frame.
     */
    private float rotation = 0.0f;
    /**
     * Cached item frame's Bedrock compound tag.
     */
    private CompoundTag cachedTag;

    public ItemFrameEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation, HangingDirection direction) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        CompoundTagBuilder builder = CompoundTag.builder();
        builder.tag(CompoundTag.builder()
                .stringTag("name", "minecraft:frame")
                .intTag("version", BlockTranslator.getBlockStateVersion())
                .tag(CompoundTag.builder()
                        .intTag("facing_direction", direction.ordinal())
                        .byteTag("item_frame_map_bit", (byte) 0)
                        .build("states"))
                .build("block"));
        builder.shortTag("id", (short) 199);
        bedrockRuntimeId = BlockTranslator.getItemFrame(builder.buildRootTag());
        bedrockPosition = Vector3i.from(position.getFloorX(), position.getFloorY(), position.getFloorZ());
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        session.getItemFrameCache().put(bedrockPosition, entityId);
        // Delay is required, or else loading in frames on chunk load is sketchy at best
        session.getConnector().getGeneralThreadPool().schedule(() -> {
            updateBlock(session);
            session.getConnector().getLogger().debug("Spawned item frame at location " + bedrockPosition + " with java id " + entityId);
        }, 500, TimeUnit.MILLISECONDS);
        valid = true;
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 7 && entityMetadata.getValue() != null) {
            ItemData itemData = ItemTranslator.translateToBedrock(session, (ItemStack) entityMetadata.getValue());
            ItemEntry itemEntry = ItemRegistry.getItem((ItemStack) entityMetadata.getValue());
            CompoundTagBuilder builder = CompoundTag.builder();

            String blockName = "";
            for (StartGamePacket.ItemEntry startGamePacketItemEntry : ItemRegistry.ITEMS) {
                if (startGamePacketItemEntry.getId() == (short) itemEntry.getBedrockId()) {
                    blockName = startGamePacketItemEntry.getIdentifier();
                    break;
                }
            }

            builder.byteTag("Count", (byte) itemData.getCount());
            if (itemData.getTag() != null) {
                builder.tag(itemData.getTag().toBuilder().build("tag"));
            }
            builder.shortTag("Damage", itemData.getDamage());
            builder.stringTag("Name", blockName);
            CompoundTagBuilder tag = getDefaultTag().toBuilder();
            tag.tag(builder.build("Item"));
            tag.floatTag("ItemDropChance", 1.0f);
            tag.floatTag("ItemRotation", rotation);
            cachedTag = tag.buildRootTag();
            updateBlock(session);
        }
        else if (entityMetadata.getId() == 7 && entityMetadata.getValue() == null && cachedTag != null) {
            cachedTag = getDefaultTag();
            updateBlock(session);
        }
        else if (entityMetadata.getId() == 8) {
            rotation = ((int) entityMetadata.getValue()) * 45;
            if (cachedTag == null) {
                updateBlock(session);
                return;
            }
            CompoundTagBuilder builder = cachedTag.toBuilder();
            builder.floatTag("ItemRotation", rotation);
            cachedTag = builder.buildRootTag();
            updateBlock(session);
        }
        else {
            updateBlock(session);
        }
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(bedrockPosition);
        updateBlockPacket.setRuntimeId(0);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NONE);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.sendUpstreamPacket(updateBlockPacket);
        session.getItemFrameCache().remove(position, entityId);
        valid = false;
        return true;
    }

    private CompoundTag getDefaultTag() {
        CompoundTagBuilder builder = CompoundTag.builder();
        builder.intTag("x", bedrockPosition.getX());
        builder.intTag("y", bedrockPosition.getY());
        builder.intTag("z", bedrockPosition.getZ());
        builder.byteTag("isMovable", (byte) 1);
        builder.stringTag("id", "ItemFrame");
        return builder.buildRootTag();
    }

    /**
     * Updates the item frame as a block
     * @param session GeyserSession.
     */
    public void updateBlock(GeyserSession session) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(bedrockPosition);
        updateBlockPacket.setRuntimeId(bedrockRuntimeId);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NONE);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.sendUpstreamPacket(updateBlockPacket);

        BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
        blockEntityDataPacket.setBlockPosition(bedrockPosition);
        if (cachedTag != null) {
            blockEntityDataPacket.setData(cachedTag);
        } else {
            blockEntityDataPacket.setData(getDefaultTag());
        }

        session.sendUpstreamPacket(blockEntityDataPacket);
    }

    /**
     * Finds the Java entity ID of an item frame from its Bedrock position.
     * @param position position of item frame in Bedrock.
     * @param session GeyserSession.
     * @return Java entity ID or -1 if not found.
     */
    public static long getItemFrameEntityId(GeyserSession session, Vector3i position) {
        return session.getItemFrameCache().getOrDefault(position, -1);
    }

    /**
     * Determines if the position contains an item frame.
     * Does largely the same thing as getItemFrameEntityId, but for speed purposes is implemented separately,
     * since every block destroy packet has to check for an item frame.
     * @param position position of block.
     * @param session GeyserSession.
     * @return true if position contains item frame, false if not.
     */
    public static boolean positionContainsItemFrame(GeyserSession session, Vector3i position) {
        return session.getItemFrameCache().containsKey(position);
    }

    /**
     * Force-remove from the position-to-ID map so it doesn't cause conflicts.
     * @param session GeyserSession.
     * @param position position of the removed item frame.
     */
    public static void removePosition(GeyserSession session, Vector3i position) {
        session.getItemFrameCache().remove(position);
    }
}
