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

package org.geysermc.geyser.entity.type.living.monster;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.UUID;

public class PiglinEntity extends BasePiglinEntity {

    public PiglinEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setBaby(BooleanEntityMetadata entityMetadata) {
        boolean isBaby = entityMetadata.getPrimitiveValue();
        setScale(isBaby? .55f : 1f);
        setFlag(EntityFlag.BABY, isBaby);

        updateMountOffset();
    }

    public void setChargingCrossbow(BooleanEntityMetadata entityMetadata) {
        boolean charging = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.CHARGING, charging);
        dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, charging ? (byte) 64 : (byte) 0); // TODO: gradually increase
    }

    public void setDancing(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.DANCING, entityMetadata.getPrimitiveValue());
    }

    @Override
    public void setHand(ItemStack stack) {
        ItemMapping crossbow = session.getItemMappings().getStoredItems().crossbow();
        boolean toCrossbow = stack != null && stack.getId() == crossbow.getJavaItem().javaId();

        if (toCrossbow ^ this.hand.getDefinition() == crossbow.getBedrockDefinition()) { // If switching to/from crossbow
            dirtyMetadata.put(EntityDataTypes.BLOCK, session.getBlockMappings().getDefinition(toCrossbow ? 0 : 1));
            dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, (byte) 0);
            setFlag(EntityFlag.CHARGED, false);
            setFlag(EntityFlag.USING_ITEM, false);
            updateBedrockMetadata();

            if (this.hand.isValid()) {
                MobEquipmentPacket mobEquipmentPacket = new MobEquipmentPacket();
                mobEquipmentPacket.setRuntimeEntityId(geyserId);
                mobEquipmentPacket.setContainerId(ContainerId.INVENTORY);
                mobEquipmentPacket.setInventorySlot(0);
                mobEquipmentPacket.setHotbarSlot(-1);
                mobEquipmentPacket.setItem(ItemData.AIR);
                session.sendUpstreamPacket(mobEquipmentPacket);
            }
        }

        super.setHand(stack);
    }

    @Override
    public void updateMainHand(GeyserSession session) {
        super.updateMainHand(session);

        if (this.hand.getDefinition() == session.getItemMappings().getStoredItems().crossbow().getBedrockDefinition()) {
            if (this.hand.getTag() != null && this.hand.getTag().containsKey("chargedItem")) {
                dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, Byte.MAX_VALUE);
                setFlag(EntityFlag.CHARGING, false);
                setFlag(EntityFlag.CHARGED, true);
                setFlag(EntityFlag.USING_ITEM, true);
            } else if (getFlag(EntityFlag.CHARGED)) {
                dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, (byte) 0);
                setFlag(EntityFlag.CHARGED, false);
                setFlag(EntityFlag.USING_ITEM, false);
            }
        }

        updateBedrockMetadata();
    }

    @Override
    public void updateOffHand(GeyserSession session) {
        // Check if the Piglin is holding Gold and set the ADMIRING flag accordingly so its pose updates
        setFlag(EntityFlag.ADMIRING, session.getTagCache().is(ItemTag.PIGLIN_LOVED, session.getItemMappings().getMapping(this.offhand).getJavaItem()));
        super.updateBedrockMetadata();

        super.updateOffHand(session);
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        InteractiveTag tag = super.testMobInteraction(hand, itemInHand);
        if (tag != InteractiveTag.NONE) {
            return tag;
        } else {
            return canGiveGoldTo(itemInHand) ? InteractiveTag.BARTER : InteractiveTag.NONE;
        }
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        InteractionResult superResult = super.mobInteract(hand, itemInHand);
        if (superResult.consumesAction()) {
            return superResult;
        } else {
            return canGiveGoldTo(itemInHand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
    }

    private boolean canGiveGoldTo(@NonNull GeyserItemStack itemInHand) {
        return !getFlag(EntityFlag.BABY) && itemInHand.asItem() == Items.GOLD_INGOT && !getFlag(EntityFlag.ADMIRING);
    }
}
