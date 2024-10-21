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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.vehicle.BoostableVehicleComponent;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.VehicleComponent;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.UUID;

public class PigEntity extends AnimalEntity implements Tickable, ClientVehicle {
    private final BoostableVehicleComponent<PigEntity> vehicleComponent = new BoostableVehicleComponent<>(this, 1.0f);

    public PigEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    @Nullable
    protected Tag<Item> getFoodTag() {
        return ItemTag.PIG_FOOD;
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (!canEat(itemInHand) && getFlag(EntityFlag.SADDLED) && passengers.isEmpty() && !session.isSneaking()) {
            // Mount
            return InteractiveTag.MOUNT;
        } else {
            InteractiveTag superTag = super.testMobInteraction(hand, itemInHand);
            if (superTag != InteractiveTag.NONE) {
                return superTag;
            } else {
                return EntityUtils.attemptToSaddle(this, itemInHand).consumesAction()
                        ? InteractiveTag.SADDLE : InteractiveTag.NONE;
            }
        }
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (!canEat(itemInHand) && getFlag(EntityFlag.SADDLED) && passengers.isEmpty() && !session.isSneaking()) {
            // Mount
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

    @Override
    public void tick() {
        PlayerEntity player = getPlayerPassenger();
        if (player == null) {
            return;
        }

        if (player == session.getPlayerEntity()) {
            if (session.getPlayerInventory().isHolding(Items.CARROT_ON_A_STICK)) {
                vehicleComponent.tickBoost();
            }
        } else { // getHand() for session player seems to always return air
            ItemDefinition itemDefinition = session.getItemMappings().getStoredItems().carrotOnAStick().getBedrockDefinition();
            if (player.getHand().getDefinition() == itemDefinition || player.getOffhand().getDefinition() == itemDefinition) {
                vehicleComponent.tickBoost();
            }
        }
    }

    @Override
    public VehicleComponent<?> getVehicleComponent() {
        return vehicleComponent;
    }

    @Override
    public Vector2f getAdjustedInput(Vector2f input) {
        return Vector2f.UNIT_Y;
    }

    @Override
    public float getVehicleSpeed() {
        return vehicleComponent.getMoveSpeed() * 0.225f * vehicleComponent.getBoostMultiplier();
    }

    private @Nullable PlayerEntity getPlayerPassenger() {
        if (getFlag(EntityFlag.SADDLED) && !passengers.isEmpty() && passengers.get(0) instanceof PlayerEntity playerEntity) {
            return playerEntity;
        }

        return null;
    }

    @Override
    public boolean isClientControlled() {
        return getPlayerPassenger() == session.getPlayerEntity() && session.getPlayerInventory().isHolding(Items.CARROT_ON_A_STICK);
    }
}
