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

package org.geysermc.geyser.entity.type.living.animal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.HappyGhastVehicleComponent;
import org.geysermc.geyser.entity.vehicle.VehicleComponent;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.UUID;

public class HappyGhastEntity extends AnimalEntity implements ClientVehicle {

    private final HappyGhastVehicleComponent vehicleComponent = new HappyGhastVehicleComponent(this, 0.0f);
    private boolean staysStill;
    private float speed;

    public HappyGhastEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // BDS 1.21.90
        setFlag(EntityFlag.CAN_FLY, true);
        setFlag(EntityFlag.CAN_WALK, true);
        setFlag(EntityFlag.TAMED, true);
        setFlag(EntityFlag.BODY_ROTATION_ALWAYS_FOLLOWS_HEAD, true);
        setFlag(EntityFlag.COLLIDABLE, true);

        setFlag(EntityFlag.WASD_AIR_CONTROLLED, true);
        setFlag(EntityFlag.DOES_SERVER_AUTH_ONLY_DISMOUNT, true);

        propertyManager.add("minecraft:can_move", true);
    }

    @Override
    @Nullable
    protected Tag<Item> getFoodTag() {
        return ItemTag.HAPPY_GHAST_FOOD;
    }

    @Override
    protected float getBabySize() {
        return 0.2375f;
    }

    public void setStaysStill(BooleanEntityMetadata entityMetadata) {
        staysStill = entityMetadata.getPrimitiveValue();
        propertyManager.add("minecraft:can_move", !entityMetadata.getPrimitiveValue());
        updateBedrockEntityProperties();
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (this.isBaby()) {
            return super.testMobInteraction(hand, itemInHand);
        } else {
            if (!itemInHand.isEmpty()) {
                if (session.getTagCache().is(ItemTag.HARNESSES, itemInHand)) {
                    if (this.equipment.get(EquipmentSlot.BODY) == null) {
                        // Harnesses the ghast
                        return InteractiveTag.EQUIP_HARNESS;
                    }
                }
                // TODO: Handle shearing the harness off
            }

            if (this.equipment.get(EquipmentSlot.BODY) != null && !session.isSneaking()) {
                // Rides happy ghast
                return InteractiveTag.RIDE_HORSE;
            } else {
                return super.testMobInteraction(hand, itemInHand);
            }
        }
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (this.isBaby()) {
            return super.mobInteract(hand, itemInHand);
        } else {
            if (!itemInHand.isEmpty()) {
                if (session.getTagCache().is(ItemTag.HARNESSES, itemInHand)) {
                    if (this.equipment.get(EquipmentSlot.BODY) == null) {
                        // Harnesses the ghast
                        return InteractionResult.SUCCESS;
                    }
                }
                // TODO: Handle shearing the harness off
            }

            if (this.equipment.get(EquipmentSlot.BODY) == null && !session.isSneaking()) {
                // Rides happy ghast
                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(hand, itemInHand);
            }
        }
    }

    @Override
    public VehicleComponent<?> getVehicleComponent() {
        return vehicleComponent;
    }

    @Override
    public Vector2f getAdjustedInput(Vector2f input) {
        // not used; calculations look a bit different for the happy ghast
        return Vector2f.ZERO;
    }

    @Override
    public float getVehicleSpeed() {
        return speed; // TODO this doesnt seem right?
    }

    @Override
    public boolean isClientControlled() {
        // TODO proper check, just lazy
        if (body == null) {
            return false;
        }
        // TODO must have ai check
        if (staysStill) {
            return false;
        }

        return getFirstPassenger() instanceof SessionPlayerEntity;
    }

    private Entity getFirstPassenger() {
        return passengers.isEmpty() ? null : passengers.get(0);
    }
}
