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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Leashable;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.enchantment.EnchantmentComponent;
import org.geysermc.geyser.item.type.SpawnEggItem;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.geyser.util.ItemUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;

public class MobEntity extends LivingEntity implements Leashable {
    /**
     * If another mob is holding this mob by a leash, this variable tracks their Bedrock entity ID.
     */
    private long leashHolderBedrockId;

    public MobEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        setLeashHolderBedrockId(-1);
    }

    public void setMobFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.NO_AI, (xd & 0x01) == 0x01);
    }

    @Override
    public void setLeashHolderBedrockId(long bedrockId) {
        this.leashHolderBedrockId = bedrockId;
        dirtyMetadata.put(EntityDataTypes.LEASH_HOLDER, bedrockId);
    }

    @Override
    protected final InteractiveTag testInteraction(Hand hand) {
        if (!isAlive()) {
            // dead lol
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

    @Override
    public final InteractionResult interact(Hand hand) {
        if (!isAlive()) {
            // dead lol
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

    public boolean canShearEquipment() {
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
                // Using the spawn egg on this entity to create a child
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @NonNull
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        return InteractiveTag.NONE;
    }

    @NonNull
    protected InteractionResult mobInteract(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean canBeLeashed() {
        return !isEnemy();
    }

    @Override
    public long leashHolderBedrockId() {
        return leashHolderBedrockId;
    }

    /**
     * Returns if the entity is hostile. Used to determine if it can be leashed.
     */
    protected boolean isEnemy() {
        return false;
    }
}
