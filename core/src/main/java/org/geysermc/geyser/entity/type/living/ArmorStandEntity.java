/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Rotation;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Optional;
import java.util.UUID;

public class ArmorStandEntity extends LivingEntity {

    // These are used to store the state of the armour stand for use when handling invisibility
    @Getter
    private boolean isMarker = false;
    private boolean isInvisible = false;
    private boolean isSmall = false;

    /**
     * On Java Edition, armor stands always show their name. Invisibility hides the name on Bedrock.
     * By having a second entity, we can allow an invisible entity with the name tag.
     * (This lets armor on armor stands still show)
     */
    private ArmorStandEntity secondEntity = null;
    /**
     * Whether this is the primary armor stand that holds the armor and not the name tag.
     */
    private boolean primaryEntity = true;
    /**
     * Whether the entity's position must be updated to included the offset.
     *
     * This should be true when the Java server marks the armor stand as invisible, but we shrink the entity
     * to allow the nametag to appear. Basically:
     * - Is visible: this is irrelevant (false)
     * - Has armor, no name: false
     * - Has armor, has name: false, with a second entity
     * - No armor, no name: false
     * - No armor, yes name: true
     */
    private boolean positionRequiresOffset = false;
    /**
     * Whether we should update the position of this armor stand after metadata updates.
     */
    private boolean positionUpdateRequired = false;

    public ArmorStandEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void spawnEntity() {
        this.pitch = yaw;
        this.headYaw = yaw;
        super.spawnEntity();
    }

    @Override
    public boolean despawnEntity() {
        if (secondEntity != null) {
            secondEntity.despawnEntity();
        }
        return super.despawnEntity();
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        if (secondEntity != null) {
            secondEntity.moveRelative(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        }
        super.moveRelative(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
    }

    @Override
    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        if (secondEntity != null) {
            secondEntity.moveAbsolute(applyOffsetToPosition(position), yaw, pitch, headYaw, isOnGround, teleported);
        } else if (positionRequiresOffset) {
            // Fake the height to be above where it is so the nametag appears in the right location for invisible non-marker armour stands
            position = applyOffsetToPosition(position);
        }

        super.moveAbsolute(position, yaw, yaw, yaw, isOnGround, teleported);
    }

    @Override
    public void setDisplayName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        super.setDisplayName(entityMetadata);
        updateSecondEntityStatus(false);
    }

    public void setArmorStandFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();

        // isSmall
        boolean newIsSmall = (xd & 0x01) == 0x01;
        if (newIsSmall != isSmall) {
            if (positionRequiresOffset) {
                // Fix new inconsistency with offset
                this.position = fixOffsetForSize(position, newIsSmall);
                positionUpdateRequired = true;
            }

            isSmall = newIsSmall;
            if (!isMarker) {
                toggleSmallStatus();
            }
        }

        // setMarker
        boolean oldIsMarker = isMarker;
        isMarker = (xd & 0x10) == 0x10;
        if (oldIsMarker != isMarker) {
            if (isMarker) {
                dirtyMetadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.0f);
                dirtyMetadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.0f);
                dirtyMetadata.put(EntityData.SCALE, 0f);
            } else {
                toggleSmallStatus();
            }

            updateSecondEntityStatus(false);
        }

        // The following values don't do anything on normal Bedrock.
        // But if given a resource pack, then we can use these values to control armor stand visual properties
        setFlag(EntityFlag.ANGRY, (xd & 0x04) != 0x04); // Has arms
        setFlag(EntityFlag.ADMIRING, (xd & 0x08) == 0x08); // Has no baseplate
    }

    public void setHeadRotation(EntityMetadata<Rotation, ?> entityMetadata) {
        onRotationUpdate(EntityData.MARK_VARIANT, EntityFlag.INTERESTED, EntityFlag.CHARGED, EntityFlag.POWERED, entityMetadata.getValue());
    }

    public void setBodyRotation(EntityMetadata<Rotation, ?> entityMetadata) {
        onRotationUpdate(EntityData.VARIANT, EntityFlag.IN_LOVE, EntityFlag.CELEBRATING, EntityFlag.CELEBRATING_SPECIAL, entityMetadata.getValue());
    }

    public void setLeftArmRotation(EntityMetadata<Rotation, ?> entityMetadata) {
        onRotationUpdate(EntityData.TRADE_TIER, EntityFlag.CHARGING, EntityFlag.CRITICAL, EntityFlag.DANCING, entityMetadata.getValue());
    }

    public void setRightArmRotation(EntityMetadata<Rotation, ?> entityMetadata) {
        onRotationUpdate(EntityData.MAX_TRADE_TIER, EntityFlag.ELDER, EntityFlag.EMOTING, EntityFlag.IDLING, entityMetadata.getValue());
    }

    public void setLeftLegRotation(EntityMetadata<Rotation, ?> entityMetadata) {
        onRotationUpdate(EntityData.SKIN_ID, EntityFlag.IS_ILLAGER_CAPTAIN, EntityFlag.IS_IN_UI, EntityFlag.LINGERING, entityMetadata.getValue());
    }

    public void setRightLegRotation(EntityMetadata<Rotation, ?> entityMetadata) {
        onRotationUpdate(EntityData.HURT_DIRECTION, EntityFlag.IS_PREGNANT, EntityFlag.SHEARED, EntityFlag.STALKING, entityMetadata.getValue());
    }

    /**
     * Updates rotation on the armor stand by hijacking other unused Bedrock entity data/flags.
     * Do note: as of recent Bedrock versions there is a custom entity data system that can be replaced with this,
     * but at this time there is no need to implement this.
     *
     * @param dataLeech the entity data to "leech" off of that stores a compressed version of the rotation
     * @param negativeXToggle the flag to set true if the X value of rotation is negative
     * @param negativeYToggle the flag to set true if the Y value of rotation is negative
     * @param negativeZToggle the flag to set true if the Z value of rotation is negative
     * @param rotation the Java rotation value
     */
    private void onRotationUpdate(EntityData dataLeech, EntityFlag negativeXToggle, EntityFlag negativeYToggle, EntityFlag negativeZToggle, Rotation rotation) {
        // Indicate that rotation should be checked
        setFlag(EntityFlag.BRIBED, true);

        int rotationX = getRotation(rotation.getPitch());
        int rotationY = getRotation(rotation.getYaw());
        int rotationZ = getRotation(rotation.getRoll());
        // The top bit acts like binary and determines if each rotation goes above 100
        // We don't do this for the negative values out of concerns of the number being too big
        int topBit = (Math.abs(rotationX) >= 100 ? 4 : 0) + (Math.abs(rotationY) >= 100 ? 2 : 0) + (Math.abs(rotationZ) >= 100 ? 1 : 0);
        int value = (topBit * 1000000) + ((Math.abs(rotationX) % 100) * 10000) + ((Math.abs(rotationY) % 100) * 100) + (Math.abs(rotationZ) % 100);
        dirtyMetadata.put(dataLeech, value);
        // Set the entity flags if a value is negative
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
            updatePosition();
        }
    }

    @Override
    protected void setInvisible(boolean value) {
        // Check if the armour stand is invisible and store accordingly
        if (primaryEntity) {
            isInvisible = value;
            updateSecondEntityStatus(false);
        }
    }

    @Override
    public void setHelmet(ItemData helmet) {
        super.setHelmet(helmet);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setChestplate(ItemData chestplate) {
        super.setChestplate(chestplate);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setLeggings(ItemData leggings) {
        super.setLeggings(leggings);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setBoots(ItemData boots) {
        super.setBoots(boots);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setHand(ItemData hand) {
        super.setHand(hand);
        updateSecondEntityStatus(true);
    }

    @Override
    public void setOffHand(ItemData offHand) {
        super.setOffHand(offHand);
        updateSecondEntityStatus(true);
    }

    /**
     * Determine if we need to load or unload the second entity.
     *
     * @param sendMetadata whether to send a metadata update after a change.
     */
    private void updateSecondEntityStatus(boolean sendMetadata) {
        // A secondary entity always has to have the offset applied, so it remains invisible and the nametag shows.
        if (!primaryEntity) return;
        if (!isInvisible || isMarker) {
            // It is either impossible to show armor, or the armor stand isn't invisible. We good.
            setFlag(EntityFlag.INVISIBLE, false);
            updateOffsetRequirement(false);
            if (positionUpdateRequired) {
                positionUpdateRequired = false;
                updatePosition();
            }

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
            return;
        }
        boolean isNametagEmpty = nametag.isEmpty();
        if (!isNametagEmpty && (!helmet.equals(ItemData.AIR) || !chestplate.equals(ItemData.AIR) || !leggings.equals(ItemData.AIR)
                || !boots.equals(ItemData.AIR) || !hand.equals(ItemData.AIR) || !offHand.equals(ItemData.AIR))) {
            // If the second entity exists, no need to recreate it.
            // We can't stuff this check above or else it'll fall into another else case and delete the second entity
            if (secondEntity != null) return;

            // Create the second entity. It doesn't need to worry about the items, but it does need to worry about
            // the metadata as it will hold the name tag.
            secondEntity = new ArmorStandEntity(session, 0, session.getEntityCache().getNextEntityId().incrementAndGet(), null,
                    EntityDefinitions.ARMOR_STAND, position, motion, yaw, pitch, headYaw);
            secondEntity.primaryEntity = false;
            if (!this.positionRequiresOffset) {
                // Ensure the offset is applied for the 0 scale
                secondEntity.position = secondEntity.applyOffsetToPosition(secondEntity.position);
            }
            // Copy metadata
            secondEntity.isSmall = isSmall;
            //secondEntity.getDirtyMetadata().putAll(dirtyMetadata); //TODO check
            secondEntity.flags.merge(this.flags);
            // Guarantee this copy is NOT invisible
            secondEntity.setFlag(EntityFlag.INVISIBLE, false);
            // Scale to 0 to show nametag
            secondEntity.getDirtyMetadata().put(EntityData.SCALE, 0.0f);
            // No bounding box as we don't want to interact with this entity
            secondEntity.getDirtyMetadata().put(EntityData.BOUNDING_BOX_WIDTH, 0.0f);
            secondEntity.getDirtyMetadata().put(EntityData.BOUNDING_BOX_HEIGHT, 0.0f);
            secondEntity.spawnEntity();

            // Reset scale of the proper armor stand
            this.dirtyMetadata.put(EntityData.SCALE, isSmall ? 0.55f : 1f);
            // Set the proper armor stand to invisible to show armor
            setFlag(EntityFlag.INVISIBLE, true);
            // Update the position of the armor stand
            updateOffsetRequirement(false);
        } else if (isNametagEmpty) {
            // We can just make an invisible entity
            // Reset scale of the proper armor stand
            dirtyMetadata.put(EntityData.SCALE, isSmall ? 0.55f : 1f);
            // Set the proper armor stand to invisible to show armor
            setFlag(EntityFlag.INVISIBLE, true);
            // Update offset
            updateOffsetRequirement(false);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
        } else {
            // Nametag is not empty and there is no armor
            // We don't need to make a new entity
            setFlag(EntityFlag.INVISIBLE, false);
            dirtyMetadata.put(EntityData.SCALE, 0.0f);
            // As the above is applied, we need an offset
            updateOffsetRequirement(true);

            if (secondEntity != null) {
                secondEntity.despawnEntity();
                secondEntity = null;
            }
        }
        if (sendMetadata) {
            this.updateBedrockMetadata();
        }
    }

    private int getRotation(float rotation) {
        rotation = rotation % 360f;
        if (rotation < -180f) {
            rotation += 360f;
        } else if (rotation >= 180f) {
            // 181 -> -179
            rotation = -(180 - (rotation - 180));
        }
        return (int) rotation;
    }

    /**
     * If this armor stand is not a marker, set its bounding box size and scale.
     */
    private void toggleSmallStatus() {
        dirtyMetadata.put(EntityData.BOUNDING_BOX_WIDTH, isSmall ? 0.25f : definition.width());
        dirtyMetadata.put(EntityData.BOUNDING_BOX_HEIGHT, isSmall ? 0.9875f : definition.height());
        dirtyMetadata.put(EntityData.SCALE, isSmall ? 0.55f : 1f);
    }

    /**
     * @return the selected position with the position offset applied.
     */
    private Vector3f applyOffsetToPosition(Vector3f position) {
        return position.add(0d, definition.height() * (isSmall ? 0.55d : 1d), 0d);
    }

    /**
     * @return an adjusted offset for the new small status.
     */
    private Vector3f fixOffsetForSize(Vector3f position, boolean isNowSmall) {
        position = removeOffsetFromPosition(position);
        return position.add(0d, definition.height() * (isNowSmall ? 0.55d : 1d), 0d);
    }

    /**
     * @return the selected position with the position offset removed.
     */
    private Vector3f removeOffsetFromPosition(Vector3f position) {
        return position.sub(0d, definition.height() * (isSmall ? 0.55d : 1d), 0d);
    }

    /**
     * Set the offset to a new value; if it changed, update the position, too.
     */
    private void updateOffsetRequirement(boolean newValue) {
        if (newValue != positionRequiresOffset) {
            this.positionRequiresOffset = newValue;
            if (positionRequiresOffset) {
                this.position = applyOffsetToPosition(position);
            } else {
                this.position = removeOffsetFromPosition(position);
            }
            positionUpdateRequired = true;
        }
    }

    /**
     * Updates position without calling movement code.
     */
    private void updatePosition() {
        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(Vector3f.from(yaw, yaw, yaw));
        moveEntityPacket.setOnGround(onGround);
        moveEntityPacket.setTeleported(false);
        session.sendUpstreamPacket(moveEntityPacket);
    }
}
