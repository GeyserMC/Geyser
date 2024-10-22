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

package org.geysermc.geyser.entity.type.living.animal.horse;

import org.cloudburstmc.math.vector.Vector2f;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.vehicle.CamelVehicleComponent;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.VehicleComponent;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.LongEntityMetadata;

import java.util.UUID;

public class CamelEntity extends AbstractHorseEntity implements ClientVehicle {
    public static final float SITTING_HEIGHT_DIFFERENCE = 1.43F;

    private final CamelVehicleComponent vehicleComponent = new CamelVehicleComponent(this);

    public CamelEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);

        dirtyMetadata.put(EntityDataTypes.CONTAINER_TYPE, (byte) ContainerType.HORSE.getId());

        // Always tamed, but not indicated in horse flags
        setFlag(EntityFlag.TAMED, true);
    }

    public void setHorseFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        boolean saddled = (xd & 0x04) == 0x04;
        setFlag(EntityFlag.SADDLED, saddled);
        setFlag(EntityFlag.EATING, (xd & 0x10) == 0x10);
        setFlag(EntityFlag.STANDING, (xd & 0x20) == 0x20);

        // HorseFlags
        // Bred 0x10
        // Eating 0x20
        // Open mouth 0x80
        int horseFlags = 0x0;
        horseFlags = (xd & 0x40) == 0x40 ? horseFlags | 0x80 : horseFlags;

        // Only set eating when we don't have mouth open so a player interaction doesn't trigger the eating animation
        horseFlags = (xd & 0x10) == 0x10 && (xd & 0x40) != 0x40 ? horseFlags | 0x20 : horseFlags;

        // Set the flags into the horse flags
        dirtyMetadata.put(EntityDataTypes.HORSE_FLAGS, horseFlags);

        // Send the eating particles
        // We use the wheat metadata as static particles since Java
        // doesn't send over what item was used to feed the horse
        if ((xd & 0x40) == 0x40) {
            EntityEventPacket entityEventPacket = new EntityEventPacket();
            entityEventPacket.setRuntimeEntityId(geyserId);
            entityEventPacket.setType(EntityEventType.EATING_ITEM);
            entityEventPacket.setData(session.getItemMappings().getStoredItems().wheat().getBedrockDefinition().getRuntimeId() << 16);
            session.sendUpstreamPacket(entityEventPacket);
        }

        // Shows the dash meter
        setFlag(EntityFlag.CAN_DASH, saddled);
    }

    @Override
    protected @Nullable Tag<Item> getFoodTag() {
        return ItemTag.CAMEL_FOOD;
    }

    @Override
    public void setPose(Pose pose) {
        setFlag(EntityFlag.SITTING, pose == Pose.SITTING);
        super.setPose(pose);
    }

    @Override
    protected void setDimensions(Pose pose) {
        if (pose == Pose.SITTING) {
            setBoundingBoxHeight(definition.height() - SITTING_HEIGHT_DIFFERENCE);
            setBoundingBoxWidth(definition.width());
        } else {
            super.setDimensions(pose);
        }
    }

    public void setDashing(BooleanEntityMetadata entityMetadata) {
        // Java sends true to show dash animation and start the dash cooldown,
        // false ends the dash animation, not the cooldown.
        // Bedrock shows dash animation if HAS_DASH_COOLDOWN is set and the camel is above ground
        if (entityMetadata.getPrimitiveValue()) {
            setFlag(EntityFlag.HAS_DASH_COOLDOWN, true);
            vehicleComponent.startDashCooldown();
        } else if (!isClientControlled()) { // Don't remove dash cooldown prematurely if client is controlling
            setFlag(EntityFlag.HAS_DASH_COOLDOWN, false);
        }
    }

    public void setLastPoseTick(LongEntityMetadata entityMetadata) {
        // Tick is based on world time. If negative, the camel is sitting.
        // Must be compared to world time to know if the camel is fully standing/sitting or transitioning.
        vehicleComponent.setLastPoseTick(entityMetadata.getPrimitiveValue());
    }

    @Override
    protected AttributeData calculateAttribute(Attribute javaAttribute, GeyserAttributeType type) {
        AttributeData attributeData = super.calculateAttribute(javaAttribute, type);
        if (javaAttribute.getType() == AttributeType.Builtin.JUMP_STRENGTH) {
            vehicleComponent.setHorseJumpStrength(attributeData.getValue());
        }
        return attributeData;
    }

    @Override
    public VehicleComponent<?> getVehicleComponent() {
        return vehicleComponent;
    }

    @Override
    public Vector2f getAdjustedInput(Vector2f input) {
        return input.mul(0.5f, input.getY() < 0 ? 0.25f : 1.0f);
    }

    @Override
    public boolean isClientControlled() {
        return getFlag(EntityFlag.SADDLED) && !passengers.isEmpty() && passengers.get(0) == session.getPlayerEntity();
    }

    @Override
    public float getVehicleSpeed() {
        float moveSpeed = vehicleComponent.getMoveSpeed();
        if (!getFlag(EntityFlag.HAS_DASH_COOLDOWN) && session.getPlayerEntity().getFlag(EntityFlag.SPRINTING)) {
            return moveSpeed + 0.1f;
        }
        return moveSpeed;
    }

    @Override
    public boolean canClimb() {
        return false;
    }
}
