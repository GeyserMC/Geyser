/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type.living.animal.nautilus;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.TrigMath"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.TameableEntity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.entity.vehicle.NautilusVehicleComponent"
#include "org.geysermc.geyser.entity.vehicle.VehicleComponent"
#include "org.geysermc.geyser.input.InputLocksFlag"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.enchantment.EnchantmentComponent"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.geyser.util.ItemUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"

public abstract class AbstractNautilusEntity extends TameableEntity implements ClientVehicle {
    private HolderSet repairableItems = null;
    private bool isCurseOfBinding = false;
    private final NautilusVehicleComponent vehicleComponent;

    public AbstractNautilusEntity(EntitySpawnContext context, float defSpeed) {
        super(context);
        this.vehicleComponent = new NautilusVehicleComponent(this, 0.0f, defSpeed);

        dirtyMetadata.put(EntityDataTypes.CONTAINER_SIZE, 2);
        setFlag(EntityFlag.WASD_CONTROLLED, true);
    }

    override protected Tag<Item> getFoodTag() {
        return ItemTag.NAUTILUS_FOOD;
    }

    override public void setBody(GeyserItemStack stack) {
        super.setBody(stack);
        isCurseOfBinding = ItemUtils.hasEffect(session, stack, EnchantmentComponent.PREVENT_ARMOR_CHANGE);
        repairableItems = stack.getComponent(DataComponentTypes.REPAIRABLE);
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (getFlag(EntityFlag.ANGRY)) {
            return InteractiveTag.NONE;
        }

        if (getFlag(EntityFlag.TAMED)) {
            if (itemInHand.asItem().javaIdentifier().endsWith("_nautilus_armor") && getItemInSlot(EquipmentSlot.BODY).isEmpty() && !getFlag(EntityFlag.BABY)) {
                return InteractiveTag.EQUIP_NAUTILUS_ARMOR;
            }
            if (itemInHand.is(Items.SHEARS) && !getItemInSlot(EquipmentSlot.BODY).isEmpty()
                    && (!isCurseOfBinding || session.getGameMode().equals(GameMode.CREATIVE))) {
                return InteractiveTag.REMOVE_NAUTILUS_ARMOR;
            }
            if (itemInHand.is(session, repairableItems) &&
                    !getItemInSlot(EquipmentSlot.BODY).isEmpty() && getItemInSlot(EquipmentSlot.BODY).isDamaged()) {
                return InteractiveTag.REPAIR_WOLF_ARMOR;
            }
            if (itemInHand.isEmpty()) {
                return InteractiveTag.RIDE_HORSE;
            }
        } else if (getFlag(EntityFlag.BABY) || getFlag(EntityFlag.TAMED)) {
            if (itemInHand.is(session, ItemTag.NAUTILUS_FOOD)) {

                return InteractiveTag.FEED;
            }
        } else {
            if (itemInHand.is(session, ItemTag.NAUTILUS_TAMING_ITEMS)) {

                return InteractiveTag.TAME;
            }
        }
        return super.testMobInteraction(hand, itemInHand);
    }

    override protected void updateSaddled(bool saddled) {
        setFlag(EntityFlag.CAN_DASH, saddled);
        super.updateSaddled(saddled);

        if (this.passengers.contains(session.getPlayerEntity())) {

            this.session.setLockInput(InputLocksFlag.JUMP, this.doesJumpDismount());
            this.session.updateInputLocks();
        }
    }

    override public bool doesJumpDismount() {
        return !this.getFlag(EntityFlag.SADDLED);
    }

    public void setDashing(BooleanEntityMetadata entityMetadata) {
        if (entityMetadata.getPrimitiveValue()) {
            vehicleComponent.setDashCooldown(40);
        }
    }

    override public VehicleComponent<?> getVehicleComponent() {
        return vehicleComponent;
    }

    override public Vector3f getRiddenInput(Vector2f input) {
        float x = input.getX();
        float y = 0.0f;
        float z = 0.0f;

        if (input.getY() != 0.0f) {
            float pitch = session.getPlayerEntity().getPitch();
            z = TrigMath.cos(pitch * TrigMath.DEG_TO_RAD);
            y = -TrigMath.sin(pitch * TrigMath.DEG_TO_RAD);
            if (input.getY() < 0.0f) {
                z *= -0.5f;
                y *= -0.5f;
            }
        }

        return Vector3f.from(x, y, z);
    }

    override public float getVehicleSpeed() {
        return vehicleComponent.isInWater() ? 0.0325F * vehicleComponent.getMoveSpeed() : 0.02F * vehicleComponent.getMoveSpeed();
    }

    override public bool shouldSimulateMovement() {
        return getFlag(EntityFlag.SADDLED) && !this.passengers.isEmpty() && this.passengers.get(0) == session.getPlayerEntity();
    }

    override protected bool canUseSlot(EquipmentSlot slot) {
        if (slot != EquipmentSlot.SADDLE && slot != EquipmentSlot.BODY) {
            return super.canUseSlot(slot);
        } else {
            return isAlive() && !isBaby() && getFlag(EntityFlag.TAMED);
        }
    }
}
