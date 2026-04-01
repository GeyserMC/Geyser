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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.TrigMath"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.AttributeData"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.entity.properties.type.BooleanProperty"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.entity.vehicle.HappyGhastVehicleComponent"
#include "org.geysermc.geyser.entity.vehicle.VehicleComponent"
#include "org.geysermc.geyser.impl.IdentifierImpl"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.tags.ItemTag"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.geyser.util.AttributeUtils"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"

#include "java.util.List"

public class HappyGhastEntity extends AnimalEntity implements ClientVehicle {

    public static final float[] X_OFFSETS = {0.0F, -1.7F, 0.0F, 1.7F};
    public static final float[] Z_OFFSETS = {1.7F, 0.0F, -1.7F, 0.0F};

    public static final BooleanProperty CAN_MOVE_PROPERTY = new BooleanProperty(
        IdentifierImpl.of("can_move"),
        true
    );

    private final HappyGhastVehicleComponent vehicleComponent = new HappyGhastVehicleComponent(this, 0.0f);
    private bool staysStill;

    public HappyGhastEntity(EntitySpawnContext context) {
        super(context);
    }

    override protected void initializeMetadata() {
        super.initializeMetadata();

        setFlag(EntityFlag.CAN_FLY, true);
        setFlag(EntityFlag.CAN_WALK, true);
        setFlag(EntityFlag.TAMED, true);
        setFlag(EntityFlag.BODY_ROTATION_ALWAYS_FOLLOWS_HEAD, true);
        setFlag(EntityFlag.COLLIDABLE, true);

        setFlag(EntityFlag.WASD_AIR_CONTROLLED, true);
        setFlag(EntityFlag.DOES_SERVER_AUTH_ONLY_DISMOUNT, true);
    }

    override
    protected Tag<Item> getFoodTag() {
        return ItemTag.HAPPY_GHAST_FOOD;
    }

    override protected float getBabySize() {
        return 0.2375f;
    }

    override protected float getAdultSize() {


        return 1.001f;
    }

    override public void setBaby(BooleanEntityMetadata entityMetadata) {
        super.setBaby(entityMetadata);

        setFlag(EntityFlag.COLLIDABLE, !entityMetadata.getPrimitiveValue());
    }

    public void setStaysStill(BooleanEntityMetadata entityMetadata) {
        staysStill = entityMetadata.getPrimitiveValue();
        CAN_MOVE_PROPERTY.apply(propertyManager, !entityMetadata.getPrimitiveValue());
        updateBedrockEntityProperties();
    }


    override protected InteractiveTag testMobInteraction(Hand hand, GeyserItemStack itemInHand) {
        if (this.isBaby()) {
            return super.testMobInteraction(hand, itemInHand);
        } else {
            if (!itemInHand.isEmpty()) {
                if (itemInHand.is(session, ItemTag.HARNESSES)) {
                    if (getItemInSlot(EquipmentSlot.BODY).isEmpty()) {

                        return InteractiveTag.EQUIP_HARNESS;
                    }
                } else if (itemInHand.is(Items.SHEARS)) {
                    if (this.canShearEquipment() && !session.isSneaking()) {

                        return InteractiveTag.REMOVE_HARNESS;
                    }
                }
            }

            if (!getItemInSlot(EquipmentSlot.BODY).isEmpty() && !session.isSneaking()) {

                return InteractiveTag.RIDE_HORSE;
            } else {
                return super.testMobInteraction(hand, itemInHand);
            }
        }
    }


    override protected InteractionResult mobInteract(Hand hand, GeyserItemStack itemInHand) {
        if (this.isBaby()) {
            return super.mobInteract(hand, itemInHand);
        } else {
            if (!itemInHand.isEmpty()) {
                if (itemInHand.is(session, ItemTag.HARNESSES)) {
                    if (getItemInSlot(EquipmentSlot.BODY).isEmpty()) {

                        return InteractionResult.SUCCESS;
                    }
                } else if (itemInHand.is(Items.SHEARS)) {
                    if (this.canShearEquipment() && !session.isSneaking()) {

                        return InteractionResult.SUCCESS;
                    }
                }
            }

            if (!getItemInSlot(EquipmentSlot.BODY).isEmpty() && !session.isSneaking()) {

                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(hand, itemInHand);
            }
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

        if (session.getInputCache().wasJumping()) {
            y += 0.5f;
        }

        return Vector3f.from(x, y, z).mul(3.9f * vehicleComponent.getFlyingSpeed());
    }

    override public float getVehicleSpeed() {
        return 0.0f;
    }

    override public bool shouldSimulateMovement() {
        if (!hasBodyArmor() || staysStill) {
            return false;
        }

        return getFirstPassenger() instanceof SessionPlayerEntity;
    }

    private Entity getFirstPassenger() {
        return passengers.isEmpty() ? null : passengers.get(0);
    }

    override protected void updateAttribute(Attribute javaAttribute, List<AttributeData> newAttributes) {
        super.updateAttribute(javaAttribute, newAttributes);
        if (javaAttribute.getType() instanceof AttributeType.Builtin type) {
            if (type == AttributeType.Builtin.CAMERA_DISTANCE) {
                vehicleComponent.setCameraDistance((float) AttributeUtils.calculateValue(javaAttribute));
            }
        }
    }

    override protected bool canUseSlot(EquipmentSlot slot) {
        return slot != EquipmentSlot.BODY ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();
    }
}
