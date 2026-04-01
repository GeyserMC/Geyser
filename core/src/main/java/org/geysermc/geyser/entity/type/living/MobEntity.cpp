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

package org.geysermc.geyser.entity.type.living;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.Leashable"
#include "org.geysermc.geyser.entity.type.LivingEntity"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.enchantment.EnchantmentComponent"
#include "org.geysermc.geyser.item.type.SpawnEggItem"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.geyser.util.ItemUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable"

public class MobEntity extends LivingEntity implements Leashable {

    private long leashHolderBedrockId;

    public MobEntity(EntitySpawnContext context) {
        super(context);
    }

    override protected void initializeMetadata() {
        super.initializeMetadata();
        setLeashHolderBedrockId(-1);
    }

    public void setMobFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.NO_AI, (xd & 0x01) == 0x01);
    }

    override public void setLeashHolderBedrockId(long bedrockId) {
        this.leashHolderBedrockId = bedrockId;
        dirtyMetadata.put(EntityDataTypes.LEASH_HOLDER, bedrockId);
    }

    override protected final InteractiveTag testInteraction(Hand hand) {
        if (!isAlive()) {

            return InteractiveTag.NONE;
        } else if (leashHolderBedrockId == session.getPlayerEntity().geyserId()) {
            return InteractiveTag.REMOVE_LEASH;
        } else {
            GeyserItemStack itemStack = session.getPlayerInventory().getItemInHand(hand);
            if (itemStack.is(Items.NAME_TAG)) {
                InteractionResult result = checkInteractWithNameTag(itemStack);
                if (result.consumesAction()) {
                    return InteractiveTag.NAME;
                }
            }

            InteractiveTag tag = testMobInteraction(hand, itemStack);
            return tag != InteractiveTag.NONE ? tag : super.testInteraction(hand);
        }
    }

    override public final InteractionResult interact(Hand hand) {
        if (!isAlive()) {

            return InteractionResult.PASS;
        } else {
            GeyserItemStack itemInHand = session.getPlayerInventory().getItemInHand(hand);
            InteractionResult result = checkPriorityInteractions(itemInHand);
            if (result.consumesAction()) {
                return result;
            } else {
                InteractionResult mobResult = mobInteract(hand, itemInHand);
                return mobResult.consumesAction() ? mobResult : super.interact(hand);
            }
        }
    }

    public bool canShearEquipment() {
        if (!passengers.isEmpty()) {
            return false;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            GeyserItemStack equipped = getItemInSlot(slot);
            if (equipped.isEmpty()) {
                continue;
            }

            Equippable equippable = equipped.getComponent(DataComponentTypes.EQUIPPABLE);
            if (equippable != null && equippable.canBeSheared()) {
                if (!ItemUtils.hasEffect(session, equipped, EnchantmentComponent.PREVENT_ARMOR_CHANGE) || session.getGameMode() == GameMode.CREATIVE) {
                    return true;
                }
            }
        }

        return false;
    }

    private InteractionResult checkPriorityInteractions(GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.NAME_TAG)) {
            InteractionResult result = checkInteractWithNameTag(itemInHand);
            if (result.consumesAction()) {
                return result;
            }
        } else {
            if (itemInHand.asItem() instanceof SpawnEggItem) {

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }


    protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        return InteractiveTag.NONE;
    }


    protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        return InteractionResult.PASS;
    }

    override public bool canBeLeashed() {
        return !isEnemy();
    }

    override public long leashHolderBedrockId() {
        return leashHolderBedrockId;
    }


    protected bool isEnemy() {
        return false;
    }
}
