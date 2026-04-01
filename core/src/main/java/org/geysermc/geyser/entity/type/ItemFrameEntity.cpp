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

package org.geysermc.geyser.entity.type;

#include "lombok.Getter"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"


public class ItemFrameEntity extends HangingEntity {

    private final Vector3i bedrockPosition;

    private BlockDefinition blockDefinition;

    private float rotation = 0.0f;

    private NbtMap cachedTag;

    @Getter
    private ItemStack heldItem = null;

    private bool changed = true;

    public ItemFrameEntity(EntitySpawnContext context) {
        super(context);

        blockDefinition = buildBlockDefinition(Direction.SOUTH);
        bedrockPosition = position().toInt();

        session.getItemFrameCache().put(bedrockPosition, this);
    }

    override protected void initializeMetadata() {


    }

    override public void spawnEntity() {
        updateBlock(true);
        session.getGeyser().getLogger().debug("Spawned item frame at location " + bedrockPosition + " with java id " + entityId);
        valid = true;
    }

    override public void setDirection(Direction direction) {
        blockDefinition = buildBlockDefinition(direction);
        changed = true;
    }

    public void setItemInFrame(EntityMetadata<ItemStack, ?> entityMetadata) {
        if (entityMetadata.getValue() != null) {
            this.heldItem = entityMetadata.getValue();
            ItemData itemData = ItemTranslator.translateToBedrock(session, heldItem);
            std::string customIdentifier = session.getItemMappings().getCustomIdMappings().get(itemData.getDefinition().getRuntimeId());

            NbtMapBuilder builder = NbtMap.builder();
            builder.putByte("Count", (byte) itemData.getCount());
            NbtMap itemDataTag = itemData.getTag();
            if (itemDataTag != null) {

                std::string customName = ItemTranslator.getCustomName(session, heldItem.getDataComponentsPatch(),
                    session.getItemMappings().getMapping(heldItem), 'f', true, false);
                if (customName == null) {

                    NbtMapBuilder copy = itemDataTag.toBuilder();
                    copy.remove("display");
                    itemDataTag = copy.build();
                }

                builder.put("tag", itemDataTag);
            }
            builder.putShort("Damage", (short) itemData.getDamage());
            builder.putString("Name", customIdentifier != null ? customIdentifier : session.getItemMappings().getMapping(entityMetadata.getValue()).getBedrockIdentifier());
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

    override public void despawnEntity() {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(bedrockPosition);
        updateBlockPacket.setDefinition(session.getBlockMappings().getBedrockAir()); //TODO maybe set this to the world block or another item frame?
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.sendUpstreamPacket(updateBlockPacket);

        session.getItemFrameCache().remove(bedrockPosition, this);

        valid = false;
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

    override public void updateBedrockMetadata() {
        updateBlock(false);
    }


    public void updateBlock(bool force) {
        if (!changed && !force) {

            return;
        }
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(bedrockPosition);
        updateBlockPacket.setDefinition(blockDefinition);
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

    override public InteractionResult interact(Hand hand) {
        return InventoryUtils.isEmpty(heldItem) && session.getPlayerInventory().getItemInHand(hand).isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    private BlockDefinition buildBlockDefinition(Direction direction) {
        NbtMapBuilder blockBuilder = NbtMap.builder()
            .putString("name", this.definition.entityType() == EntityType.GLOW_ITEM_FRAME ? "minecraft:glow_frame" : "minecraft:frame");
        NbtMapBuilder statesBuilder = NbtMap.builder()
            .putInt("facing_direction", direction.ordinal())
            .putByte("item_frame_map_bit", (byte) 0)
            .putByte("item_frame_photo_bit", (byte) 0);
        blockBuilder.put("states", statesBuilder.build());

        return session.getBlockMappings().getItemFrame(blockBuilder.build());
    }


    public static ItemFrameEntity getItemFrameEntity(GeyserSession session, Vector3i position) {
        return session.getItemFrameCache().get(position);
    }
}
