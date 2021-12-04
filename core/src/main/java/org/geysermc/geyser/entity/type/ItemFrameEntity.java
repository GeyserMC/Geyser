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

package org.geysermc.geyser.entity.type;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import com.nukkitx.protocol.bedrock.v465.Bedrock_v465;
import lombok.Getter;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.UUID;

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
    private NbtMap cachedTag;
    /**
     * The item currently in the item frame. Used for block picking.
     */
    @Getter
    private ItemStack heldItem = null;
    /**
     * Determines if this entity needs updated on the client end/
     */
    private boolean changed = true;

    public ItemFrameEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, Direction direction) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, 0f);

        NbtMapBuilder blockBuilder = NbtMap.builder()
                .putString("name", this.definition.entityType() == EntityType.GLOW_ITEM_FRAME ? "minecraft:glow_frame" : "minecraft:frame")
                .putInt("version", session.getBlockMappings().getBlockStateVersion());
        NbtMapBuilder statesBuilder = NbtMap.builder()
                .putInt("facing_direction", direction.ordinal())
                .putByte("item_frame_map_bit", (byte) 0);
        if (session.getUpstream().getProtocolVersion() >= Bedrock_v465.V465_CODEC.getProtocolVersion()) {
            statesBuilder.putByte("item_frame_photo_bit", (byte) 0);
        }
        blockBuilder.put("states", statesBuilder.build());

        bedrockRuntimeId = session.getBlockMappings().getItemFrame(blockBuilder.build());
        bedrockPosition = Vector3i.from(position.getFloorX(), position.getFloorY(), position.getFloorZ());

        session.getItemFrameCache().put(bedrockPosition, this);
    }

    @Override
    protected void initializeMetadata() {
        // lol nah don't do anything
        // This isn't a real entity for Bedrock so it isn't going to do anything
    }

    @Override
    public void spawnEntity() {
        updateBlock(true);
        session.getGeyser().getLogger().debug("Spawned item frame at location " + bedrockPosition + " with java id " + entityId);
        valid = true;
    }

    public void setItemInFrame(EntityMetadata<ItemStack, ?> entityMetadata) {
        if (entityMetadata.getValue() != null) {
            this.heldItem = entityMetadata.getValue();
            ItemData itemData = ItemTranslator.translateToBedrock(session, heldItem);
            ItemMapping mapping = session.getItemMappings().getMapping(entityMetadata.getValue());
            NbtMapBuilder builder = NbtMap.builder();

            builder.putByte("Count", (byte) itemData.getCount());
            if (itemData.getTag() != null) {
                builder.put("tag", itemData.getTag());
            }
            builder.putShort("Damage", (short) itemData.getDamage());
            builder.putString("Name", mapping.getBedrockIdentifier());
            NbtMapBuilder tag = getDefaultTag().toBuilder();
            tag.put("Item", builder.build());
            tag.putFloat("ItemDropChance", 1.0f);
            tag.putFloat("ItemRotation", rotation);
            cachedTag = tag.build();
            changed = true;
        } else if (cachedTag != null) {
            cachedTag = getDefaultTag();
            changed = true;
        }
    }

    public void setItemRotation(IntEntityMetadata entityMetadata) {
        rotation = entityMetadata.getPrimitiveValue() * 45;
        if (cachedTag == null) {
            return;
        }
        NbtMapBuilder builder = cachedTag.toBuilder();
        builder.putFloat("ItemRotation", rotation);
        cachedTag = builder.build();
        changed = true;
    }

    @Override
    public boolean despawnEntity() {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(bedrockPosition);
        updateBlockPacket.setRuntimeId(session.getBlockMappings().getBedrockAirId()); //TODO maybe set this to the world block or another item frame?
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.sendUpstreamPacket(updateBlockPacket);

        session.getItemFrameCache().remove(bedrockPosition, this);

        valid = false;
        return true;
    }

    private NbtMap getDefaultTag() {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putInt("x", bedrockPosition.getX());
        builder.putInt("y", bedrockPosition.getY());
        builder.putInt("z", bedrockPosition.getZ());
        builder.putByte("isMovable", (byte) 1);
        builder.putString("id", this.definition.entityType() == EntityType.GLOW_ITEM_FRAME ? "GlowItemFrame" : "ItemFrame");
        return builder.build();
    }

    @Override
    public void updateBedrockMetadata() {
        updateBlock(false);
    }

    /**
     * Updates the item frame as a block
     */
    public void updateBlock(boolean force) {
        if (!changed && !force) {
            // Don't send a block update packet - nothing changed
            return;
        }
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

        changed = false;
    }

    /**
     * Finds the Java entity ID of an item frame from its Bedrock position.
     * @param position position of item frame in Bedrock.
     * @param session GeyserConnection.
     * @return Java entity ID or -1 if not found.
     */
    public static ItemFrameEntity getItemFrameEntity(GeyserSession session, Vector3i position) {
        return session.getItemFrameCache().get(position);
    }
}
