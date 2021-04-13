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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.object.HangingDirection;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;

import java.util.concurrent.TimeUnit;

/**
 * Item frames are an entity in Java but a block entity in Bedrock.
 */
public class ItemFrameEntity extends Entity {

    /**
     * Used to construct the block entity tag on spawning.
     */
    private final HangingDirection direction;
    /**
     * Used for getting the Bedrock block position.
     * Blocks deal with integers whereas entities deal with floats.
     */
    private Vector3i bedrockPosition;
    /**
     * Specific block 'state' we are emulating in Bedrock.
     */
    private int bedrockRuntimeId;
    /**
     * Rotation of item in frame.
     */
    private float rotation = 0.0f;
    /**
     * Cached item frame's Bedrock compound tag.
     */
    private NbtMap cachedTag;

    public ItemFrameEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation, HangingDirection direction) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        this.direction = direction;
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        NbtMapBuilder blockBuilder = NbtMap.builder()
                .putString("name", "minecraft:frame")
                .putInt("version", session.getBlockTranslator().getBlockStateVersion());
        blockBuilder.put("states", NbtMap.builder()
                .putInt("facing_direction", direction.ordinal())
                .putByte("item_frame_map_bit", (byte) 0)
                .build());
        bedrockRuntimeId = session.getBlockTranslator().getItemFrame(blockBuilder.build());
        bedrockPosition = Vector3i.from(position.getFloorX(), position.getFloorY(), position.getFloorZ());

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
            NbtMapBuilder builder = NbtMap.builder();

            builder.putByte("Count", (byte) itemData.getCount());
            if (itemData.getTag() != null) {
                builder.put("tag", itemData.getTag().toBuilder().build());
            }
            builder.putShort("Damage", (short) itemData.getDamage());
            builder.putString("Name", itemEntry.getBedrockIdentifier());
            NbtMapBuilder tag = getDefaultTag().toBuilder();
            tag.put("Item", builder.build());
            tag.putFloat("ItemDropChance", 1.0f);
            tag.putFloat("ItemRotation", rotation);
            cachedTag = tag.build();
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
            NbtMapBuilder builder = cachedTag.toBuilder();
            builder.putFloat("ItemRotation", rotation);
            cachedTag = builder.build();
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
        updateBlockPacket.setRuntimeId(session.getBlockTranslator().getBedrockAirId());
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.sendUpstreamPacket(updateBlockPacket);
        session.getItemFrameCache().remove(position, entityId);
        valid = false;
        return true;
    }

    private NbtMap getDefaultTag() {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putInt("x", bedrockPosition.getX());
        builder.putInt("y", bedrockPosition.getY());
        builder.putInt("z", bedrockPosition.getZ());
        builder.putByte("isMovable", (byte) 1);
        builder.putString("id", "ItemFrame");
        return builder.build();
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
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
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
     * Force-remove from the position-to-ID map so it doesn't cause conflicts.
     * @param session GeyserSession.
     * @param position position of the removed item frame.
     */
    public static void removePosition(GeyserSession session, Vector3i position) {
        session.getItemFrameCache().remove(position);
    }
}
