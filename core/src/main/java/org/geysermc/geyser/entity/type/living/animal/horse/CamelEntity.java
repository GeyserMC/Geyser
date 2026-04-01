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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.vehicle.CamelVehicleComponent;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.VehicleComponent;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.LongEntityMetadata;

public class CamelEntity extends AbstractHorseEntity implements ClientVehicle {
    public static final float SITTING_HEIGHT_DIFFERENCE = 1.43F;

    private final CamelVehicleComponent vehicleComponent = new CamelVehicleComponent(this);

    public CamelEntity(EntitySpawnContext context) {
        super(context);

        dirtyMetadata.put(EntityDataTypes.CONTAINER_TYPE, (byte) ContainerType.HORSE.getId());

        
        setFlag(EntityFlag.TAMED, true);
    }

    public void setHorseFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.EATING, (xd & 0x10) == 0x10);
        setFlag(EntityFlag.STANDING, (xd & 0x20) == 0x20);

        
        
        
        
        int horseFlags = 0x0;
        horseFlags = (xd & 0x40) == 0x40 ? horseFlags | 0x80 : horseFlags;

        
        horseFlags = (xd & 0x10) == 0x10 && (xd & 0x40) != 0x40 ? horseFlags | 0x20 : horseFlags;

        
        dirtyMetadata.put(EntityDataTypes.HORSE_FLAGS, horseFlags);

        
        
        
        if ((xd & 0x40) == 0x40) {
            EntityEventPacket entityEventPacket = new EntityEventPacket();
            entityEventPacket.setRuntimeEntityId(geyserId);
            entityEventPacket.setType(EntityEventType.EATING_ITEM);
            entityEventPacket.setData(session.getItemMappings().getStoredItems().wheat().getBedrockDefinition().getRuntimeId() << 16);
            session.sendUpstreamPacket(entityEventPacket);
        }

        
        
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
    protected void setDimensionsFromPose(Pose pose) {
        if (pose == Pose.SITTING) {
            setBoundingBoxHeight(definition.height() - SITTING_HEIGHT_DIFFERENCE);
            setBoundingBoxWidth(definition.width());
        } else {
            super.setDimensionsFromPose(pose);
        }
    }

    public void setDashing(BooleanEntityMetadata entityMetadata) {
        
        
        
        if (entityMetadata.getPrimitiveValue()) {
            setFlag(EntityFlag.HAS_DASH_COOLDOWN, true);
            vehicleComponent.startDashCooldown();
        } else if (!this.shouldSimulateMovement()) { 
            setFlag(EntityFlag.HAS_DASH_COOLDOWN, false);
        }
    }

    public void setLastPoseTick(LongEntityMetadata entityMetadata) {
        
        
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
    public boolean shouldSimulateMovement() {
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
