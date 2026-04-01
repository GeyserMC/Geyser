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

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.Optional;

public class ArmorStandEntity extends LivingEntity {

    
    @Getter
    private boolean isMarker = false;
    private boolean isInvisible = false;
    @Getter
    private boolean isSmall = false;

    
    private ArmorStandEntity secondEntity = null;
    
    private boolean primaryEntity = true;
    
    private boolean positionRequiresOffset = false;
    
    private boolean positionUpdateRequired = false;

    public ArmorStandEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    public void spawnEntity() {
        
        setOffset(getYOffset());
        super.spawnEntity();
    }

    @Override
    public void despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        super.despawnEntity();
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        moveAbsoluteRaw(position.add(relX, relY, relZ), yaw, pitch, headYaw, isOnGround, false);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        if (secondEntity != null) {
            secondEntity.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);
        }
        
        setOffset(getYOffset());
        super.moveAbsoluteRaw(position, yaw, yaw, yaw, isOnGround, teleported);
    }

    @Override
    public void updateNametag(@Nullable Team team) {
        
        super.updateNametag(team, true);
    }

    @Override
    public void setCustomName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        super.setCustomName(entityMetadata);
        updateSecondEntityStatus(false);
    }

    public void setArmorStandFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        boolean offsetChanged = false;
        
        boolean newIsSmall = (xd & 0x01) == 0x01;
        if (newIsSmall != isSmall) {
            isSmall = newIsSmall;
            offsetChanged = true;
            
            updatePassengerOffsets();
        }

        
        boolean oldIsMarker = isMarker;
        isMarker = (xd & 0x10) == 0x10;
        if (oldIsMarker != isMarker) {
            if (isMarker) {
                setBoundingBoxWidth(0.0f);
                setBoundingBoxHeight(0.0f);
            } else {
                setBoundingBoxWidth(definition.width());
                setBoundingBoxHeight(definition.height());
            }

            updateMountOffset();
            offsetChanged = true;
        }

        if (offsetChanged) {
            if (positionRequiresOffset) {
                positionUpdateRequired = true;
            } else if (secondEntity != null) {
                secondEntity.positionUpdateRequired = true;
            }
            updateSecondEntityStatus(false);
        }

        
        
        setFlag(EntityFlag.ANGRY, (xd & 0x04) != 0x04); 
        setFlag(EntityFlag.ADMIRING, (xd & 0x08) == 0x08); 
        setFlag(EntityFlag.BABY, isSmall); 
    }

    public void setHeadRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate(EntityDataTypes.MARK_VARIANT, EntityFlag.INTERESTED, EntityFlag.CHARGED, EntityFlag.POWERED, entityMetadata.getValue());
    }

    public void setBodyRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate(EntityDataTypes.VARIANT, EntityFlag.IN_LOVE, EntityFlag.CELEBRATING, EntityFlag.CELEBRATING_SPECIAL, entityMetadata.getValue());
    }

    public void setLeftArmRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate(EntityDataTypes.TRADE_TIER, EntityFlag.CHARGING, EntityFlag.CRITICAL, EntityFlag.DANCING, entityMetadata.getValue());
    }

    public void setRightArmRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate(EntityDataTypes.MAX_TRADE_TIER, EntityFlag.ELDER, EntityFlag.EMOTING, EntityFlag.IDLING, entityMetadata.getValue());
    }

    public void setLeftLegRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate(EntityDataTypes.SKIN_ID, EntityFlag.IS_ILLAGER_CAPTAIN, EntityFlag.IS_IN_UI, EntityFlag.LINGERING, entityMetadata.getValue());
    }

    public void setRightLegRotation(EntityMetadata<Vector3f, ?> entityMetadata) {
        onRotationUpdate(EntityDataTypes.HURT_DIRECTION, EntityFlag.IS_PREGNANT, EntityFlag.SHEARED, EntityFlag.STALKING, entityMetadata.getValue());
    }

    
    private void onRotationUpdate(EntityDataType<Integer> dataLeech, EntityFlag negativeXToggle, EntityFlag negativeYToggle, EntityFlag negativeZToggle, Vector3f rotation) {
        
        setFlag(EntityFlag.BRIBED, true);

        int rotationX = MathUtils.wrapDegreesToInt(rotation.getX());
        int rotationY = MathUtils.wrapDegreesToInt(rotation.getY());
        int rotationZ = MathUtils.wrapDegreesToInt(rotation.getZ());
        
        
        int topBit = (Math.abs(rotationX) >= 100 ? 4 : 0) + (Math.abs(rotationY) >= 100 ? 2 : 0) + (Math.abs(rotationZ) >= 100 ? 1 : 0);
        int value = (topBit * 1000000) + ((Math.abs(rotationX) % 100) * 10000) + ((Math.abs(rotationY) % 100) * 100) + (Math.abs(rotationZ) % 100);
        dirtyMetadata.put(dataLeech, value);
        
        setFlag(negativeXToggle, rotationX < 0);
        setFlag(negativeYToggle, rotationY < 0);
        setFlag(negativeZToggle, rotationZ < 0);
    }

    @Override
    public void updateBedrockMetadata() {
        if (secondEntity != null) {
            secondEntity.updateBedrockMetadata();
        }
        super.updateBedrockMetadata();
        if (positionUpdateRequired) {
            positionUpdateRequired = false;
            moveAbsoluteRaw(position, yaw, pitch, headYaw, onGround, true);
        }
    }

    @Override
    protected void setInvisible(boolean value) {
        
        if (primaryEntity) {
            isInvisible = value;
            updateSecondEntityStatus(false);
        }
    }

    @Override
    public InteractionResult interactAt(Hand hand) {
        if (!isMarker && !session.getPlayerInventory().getItemInHand(hand).is(Items.NAME_TAG)) {
            
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void setHelmet(GeyserItemStack helmet) {
        super.setHelmet(helmet);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setChestplate(GeyserItemStack chestplate) {
        super.setChestplate(chestplate);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setLeggings(GeyserItemStack leggings) {
        super.setLeggings(leggings);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setBoots(GeyserItemStack boots) {
        super.setBoots(boots);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setHand(GeyserItemStack hand) {
        super.setHand(hand);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setOffhand(GeyserItemStack offHand) {
        super.setOffhand(offHand);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setCustomNameVisible(BooleanEntityMetadata entityMetadata) {
        super.setCustomNameVisible(entityMetadata);
        updateSecondEntityStatus(false);
    }

    
    private void updateSecondEntityStatus(boolean sendMetadata) {
        
        if (!primaryEntity) return;
        if (!isInvisible) {
            
            setFlag(EntityFlag.INVISIBLE, false);
            setScale(getScale());
            updateOffsetRequirement(false);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
            if (sendMetadata) {
                this.updateBedrockMetadata();
            }
            return;
        }
        boolean isNametagEmpty = nametag.isEmpty();
        if (!isNametagEmpty && hasAnyEquipment()) {
            
            setScale(getScale());
            
            setFlag(EntityFlag.INVISIBLE, true);
            
            updateOffsetRequirement(false);

            if (secondEntity == null) {
                
                
                secondEntity = new ArmorStandEntity(EntitySpawnContext.inherited(session, EntityDefinitions.ARMOR_STAND, this, position));
                secondEntity.primaryEntity = false;
            }
            
            secondEntity.isSmall = isSmall;
            secondEntity.isMarker = isMarker;
            secondEntity.positionRequiresOffset = true; 
            secondEntity.getDirtyMetadata().put(EntityDataTypes.NAME, nametag);
            secondEntity.getDirtyMetadata().put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, customNameVisible ? (byte) 1 : (byte) 0);
            secondEntity.flags.putAll(this.flags);
            
            secondEntity.setFlag(EntityFlag.INVISIBLE, false);
            
            secondEntity.setScale(0f);
            
            secondEntity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 0.0f);
            secondEntity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 0.0f);
            if (!secondEntity.valid) { 
                secondEntity.spawnEntity();
            }
        } else if (isNametagEmpty) {
            
            
            setScale(getScale());
            
            setFlag(EntityFlag.INVISIBLE, true);
            
            updateOffsetRequirement(false);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
        } else {
            
            
            setFlag(EntityFlag.INVISIBLE, false);
            setScale(0f);
            
            updateOffsetRequirement(!isMarker);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
        }
        if (sendMetadata) {
            this.updateBedrockMetadata();
        }
    }

    private boolean hasAnyEquipment() {
        return (!getItemInSlot(EquipmentSlot.HELMET).isEmpty() || !getItemInSlot(EquipmentSlot.CHESTPLATE).isEmpty()
            || !getItemInSlot(EquipmentSlot.LEGGINGS).isEmpty() || !getItemInSlot(EquipmentSlot.BOOTS).isEmpty()
            || !getMainHandItem().isEmpty() || !getOffHandItem().isEmpty());
    }

    @Override
    public float getBoundingBoxWidth() {
        
        return super.getBoundingBoxWidth() * getScale();
    }

    @Override
    public float getBoundingBoxHeight() {
        
        
        return super.getBoundingBoxHeight() * getScale();
    }

    
    public float getYOffset() {
        if (!positionRequiresOffset || isMarker || secondEntity != null) {
            return 0;
        }
        return definition.height() * getScale();
    }

    
    private float getScale() {
        return isSmall ? 0.5f : 1f;
    }

    
    private void updateOffsetRequirement(boolean newValue) {
        if (newValue != positionRequiresOffset) {
            this.positionRequiresOffset = newValue;
            this.positionUpdateRequired = true;
            
            updatePassengerOffsets();
        }
    }

    @Override
    public Vector3f bedrockRotation() {
        return Vector3f.from(getYaw(), getYaw(), getYaw());
    }
}
