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
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.HappyGhastVehicleComponent;
import org.geysermc.geyser.entity.vehicle.VehicleComponent;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.util.AttributeUtils;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.List;

public class HappyGhastEntity extends AnimalEntity implements ClientVehicle {

    public static final float[] X_OFFSETS = {0.0F, -1.7F, 0.0F, 1.7F};
    public static final float[] Z_OFFSETS = {1.7F, 0.0F, -1.7F, 0.0F};

    public static final BooleanProperty CAN_MOVE_PROPERTY = new BooleanProperty(
        IdentifierImpl.of("can_move"),
        true
    );

    private final HappyGhastVehicleComponent vehicleComponent = new HappyGhastVehicleComponent(this, 0.0f);
    private boolean staysStill;

    public HappyGhastEntity(EntitySpawnContext context) {
        super(context);
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

    @Override
    protected float getAdultSize() {
        // Make collision slightly larger to stop bedrock client from clipping into it
        // This value will not work at very large coordinates
        return 1.001f;
    }

    @Override
    public void setBaby(BooleanEntityMetadata entityMetadata) {
        super.setBaby(entityMetadata);
        // Players can only stand on grown up happy ghasts
        setFlag(EntityFlag.COLLIDABLE, !entityMetadata.getPrimitiveValue());
    }

    public void setStaysStill(BooleanEntityMetadata entityMetadata) {
        staysStill = entityMetadata.getPrimitiveValue();
        CAN_MOVE_PROPERTY.apply(propertyManager, !entityMetadata.getPrimitiveValue());
        updateBedrockEntityProperties();
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull Hand hand, @NonNull GeyserItemStack itemInHand) {
        if (this.isBaby()) {
            return super.testMobInteraction(hand, itemInHand);
        } else {
            if (!itemInHand.isEmpty()) {
                if (itemInHand.is(session, ItemTag.HARNESSES)) {
                    if (getItemInSlot(EquipmentSlot.BODY).isEmpty()) {
                        // Harnesses the ghast
                        return InteractiveTag.EQUIP_HARNESS;
                    }
                } else if (itemInHand.is(Items.SHEARS)) {
                    if (this.canShearEquipment() && !session.isSneaking()) {
                        // Shears the harness off of the ghast
                        return InteractiveTag.REMOVE_HARNESS;
                    }
                }
            }

            if (!getItemInSlot(EquipmentSlot.BODY).isEmpty() && !session.isSneaking()) {
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
                if (itemInHand.is(session, ItemTag.HARNESSES)) {
                    if (getItemInSlot(EquipmentSlot.BODY).isEmpty()) {
                        // Harnesses the ghast
                        return InteractionResult.SUCCESS;
                    }
                } else if (itemInHand.is(Items.SHEARS)) {
                    if (this.canShearEquipment() && !session.isSneaking()) {
                        // Shears the harness off of the ghast
                        return InteractionResult.SUCCESS;
                    }
                }
            }

            if (!getItemInSlot(EquipmentSlot.BODY).isEmpty() && !session.isSneaking()) {
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
    public Vector3f getRiddenInput(Vector2f input) {
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

        if (session.getInputCache().wasJumping()) {
            y += 0.5f;
        }

        return Vector3f.from(x, y, z).mul(3.9f * vehicleComponent.getFlyingSpeed());
    }

    @Override
    public float getVehicleSpeed() {
        return 0.0f; // Not used
    }

    @Override
    public boolean isClientControlled() {
        if (!hasBodyArmor() || staysStill) {
            return false;
        }

        return getFirstPassenger() instanceof SessionPlayerEntity;
    }

    private Entity getFirstPassenger() {
        return passengers.isEmpty() ? null : passengers.get(0);
    }

    @Override
    protected void updateAttribute(Attribute javaAttribute, List<AttributeData> newAttributes) {
        super.updateAttribute(javaAttribute, newAttributes);
        if (javaAttribute.getType() instanceof AttributeType.Builtin type) {
            if (type == AttributeType.Builtin.CAMERA_DISTANCE) {
                vehicleComponent.setCameraDistance((float) AttributeUtils.calculateValue(javaAttribute));
            }
        }
    }

    @Override
    protected boolean canUseSlot(EquipmentSlot slot) {
        return slot != EquipmentSlot.BODY ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
    }
}
