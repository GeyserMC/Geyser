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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"

public class PiglinEntity extends BasePiglinEntity {

    public PiglinEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setBaby(BooleanEntityMetadata entityMetadata) {
        bool isBaby = entityMetadata.getPrimitiveValue();
        setScale(isBaby? .55f : 1f);
        setFlag(EntityFlag.BABY, isBaby);

        updateMountOffset();
    }

    public void setChargingCrossbow(BooleanEntityMetadata entityMetadata) {
        bool charging = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.CHARGING, charging);
        dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, charging ? (byte) 64 : (byte) 0);
    }

    public void setDancing(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.DANCING, entityMetadata.getPrimitiveValue());
    }

    override public void setHand(GeyserItemStack stack) {
        bool toCrossbow = stack != null && stack.is(Items.CROSSBOW);

        if (toCrossbow ^ getMainHandItem().is(Items.CROSSBOW)) {
            dirtyMetadata.put(EntityDataTypes.BLOCK, session.getBlockMappings().getDefinition(toCrossbow ? 0 : 1));
            dirtyMetadata.put(EntityDataTypes.CHARGE_AMOUNT, (byte) 0);
            setFlag(EntityFlag.CHARGED, false);
            setFlag(EntityFlag.USING_ITEM, false);
            updateBedrockMetadata();

            if (!getMainHandItem().isEmpty()) {
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

    override public void updateMainHand() {
        super.updateMainHand();

        if (getMainHandItem().is(Items.CROSSBOW)) {
            if (getMainHandItem().getComponent(DataComponentTypes.CHARGED_PROJECTILES) != null) {
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

    override public void updateOffHand() {

        setFlag(EntityFlag.ADMIRING, getOffHandItem().is(session, ItemTag.PIGLIN_LOVED));
        super.updateBedrockMetadata();

        super.updateOffHand();
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        InteractiveTag tag = super.testMobInteraction(hand, itemInHand);
        if (tag != InteractiveTag.NONE) {
            return tag;
        } else {
            return canGiveGoldTo(itemInHand) ? InteractiveTag.BARTER : InteractiveTag.NONE;
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        InteractionResult superResult = super.mobInteract(hand, itemInHand);
        if (superResult.consumesAction()) {
            return superResult;
        } else {
            return canGiveGoldTo(itemInHand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
    }

    private bool canGiveGoldTo(GeyserItemStack itemInHand) {
        return !getFlag(EntityFlag.BABY) && itemInHand.is(Items.GOLD_INGOT) && !getFlag(EntityFlag.ADMIRING);
    }
}
