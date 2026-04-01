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

package org.geysermc.geyser.entity.type.living.animal;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.Tickable"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.entity.vehicle.BoostableVehicleComponent"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.entity.vehicle.VehicleComponent"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.EntityUtils"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

public class StriderEntity extends AnimalEntity implements Tickable, ClientVehicle {

    private final BoostableVehicleComponent<StriderEntity> vehicleComponent = new BoostableVehicleComponent<>(this, 1.0f);
    private bool isCold = false;

    public StriderEntity(EntitySpawnContext context) {
        super(context);

        setFlag(EntityFlag.FIRE_IMMUNE, true);
        setFlag(EntityFlag.BREATHING, true);
    }

    public void setCold(BooleanEntityMetadata entityMetadata) {
        isCold = entityMetadata.getPrimitiveValue();
    }

    override public void updateBedrockMetadata() {


        if (getFlag(EntityFlag.RIDING)) {
            bool parentShaking = false;
            if (vehicle instanceof StriderEntity) {
                parentShaking = vehicle.getFlag(EntityFlag.SHAKING);
            }
    
            setFlag(EntityFlag.BREATHING, !parentShaking);
            setFlag(EntityFlag.SHAKING, parentShaking);
        } else {
            setFlag(EntityFlag.BREATHING, !isCold);
            setFlag(EntityFlag.SHAKING, isShaking());
        }


        for (Entity passenger : passengers) {
            if (passenger != null) {
                passenger.updateBedrockMetadata();
            }
        }

        super.updateBedrockMetadata();
    }

    override protected bool isShaking() {
        return isCold || super.isShaking();
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.STRIDER_FOOD;
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (!canEat(itemInHand) && getFlag(EntityFlag.SADDLED) && passengers.isEmpty() && !session.isSneaking()) {

            return InteractiveTag.RIDE_STRIDER;
        } else {
            InteractiveTag tag = super.testMobInteraction(hand, itemInHand);
            if (tag != InteractiveTag.NONE) {
                return tag;
            } else {
                return EntityUtils.attemptToSaddle(this, itemInHand).consumesAction()
                        ? InteractiveTag.SADDLE : InteractiveTag.NONE;
            }
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (!canEat(itemInHand) && getFlag(EntityFlag.SADDLED) && passengers.isEmpty() && !session.isSneaking()) {

            return InteractionResult.SUCCESS;
        } else {
            InteractionResult superResult = super.mobInteract(hand, itemInHand);
            if (superResult.consumesAction()) {
                return superResult;
            } else {
                return EntityUtils.attemptToSaddle(this, itemInHand);
            }
        }
    }

    public void setBoost(IntEntityMetadata entityMetadata) {
        vehicleComponent.startBoost(entityMetadata.getPrimitiveValue());
    }

    override public void tick() {
        super.tick();
        PlayerEntity player = getPlayerPassenger();
        if (player == null) {
            return;
        }

        if (player == session.getPlayerEntity()) {
            if (session.getPlayerInventory().isHolding(Items.WARPED_FUNGUS_ON_A_STICK)) {
                vehicleComponent.tickBoost();
            }
        } else {
            if (player.isHolding(Items.WARPED_FUNGUS_ON_A_STICK)) {
                vehicleComponent.tickBoost();
            }
        }
    }

    override public VehicleComponent<?> getVehicleComponent() {
        return vehicleComponent;
    }

    override public Vector3f getRiddenInput(Vector2f input) {
        return Vector3f.UNIT_Z;
    }

    override public float getVehicleSpeed() {
        return vehicleComponent.getMoveSpeed() * (isCold ? 0.35f : 0.55f) * vehicleComponent.getBoostMultiplier();
    }

    private PlayerEntity getPlayerPassenger() {
        if (getFlag(EntityFlag.SADDLED) && !passengers.isEmpty() && passengers.get(0) instanceof PlayerEntity playerEntity) {
            return playerEntity;
        }

        return null;
    }

    override public bool shouldSimulateMovement() {
        return getPlayerPassenger() == session.getPlayerEntity() && session.getPlayerInventory().isHolding(Items.WARPED_FUNGUS_ON_A_STICK);
    }

    override public bool canWalkOnLava() {
        return true;
    }

    override protected bool canUseSlot(EquipmentSlot slot) {
        return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
    }
}
