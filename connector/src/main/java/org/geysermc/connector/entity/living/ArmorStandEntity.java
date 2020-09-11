/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity.living;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class ArmorStandEntity extends LivingEntity {

    // These are used to store the state of the armour stand for use when handling invisibility
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
     * Whether the last position update included the offset.
     */
    private boolean lastPositionIncludedOffset = false;
    private GeyserSession session;

    public ArmorStandEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        this.session = session;
        super.spawnEntity(session);
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        if (secondEntity != null) {
            secondEntity.despawnEntity(session);
        }
        return super.despawnEntity(session);
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        // Fake the height to be above where it is so the nametag appears in the right location for invisible non-marker armour stands
        lastPositionIncludedOffset = false;
        if (secondEntity != null) {
            secondEntity.moveAbsolute(session, position.add(0d, entityType.getHeight() * (isSmall ? 0.55d : 1d), 0d), rotation, isOnGround, teleported);
        } else if (!isMarker && isInvisible && !metadata.getFlags().getFlag(EntityFlag.INVISIBLE)) { // Means it's not visible
            position = position.add(0d, entityType.getHeight() * (isSmall ? 0.55d : 1d), 0d);
            lastPositionIncludedOffset = true;
        }

        super.moveAbsolute(session, position, rotation, isOnGround, teleported);
    }

    @Override
    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        if (relX == 0 && relY == 0 && relZ == 0 && rotation.equals(this.rotation)) return; // Prevents a weird glitch where the armor stand fidgets
        super.moveRelative(session, relX, relY, relZ, rotation, isOnGround);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        super.updateBedrockMetadata(entityMetadata, session);
        if (entityMetadata.getId() == 0 && entityMetadata.getType() == MetadataType.BYTE) {
            byte xd = (byte) entityMetadata.getValue();

            // Check if the armour stand is invisible and store accordingly
            if (primaryEntity) {
                isInvisible = (xd & 0x20) == 0x20;
                updateSecondEntityStatus();
            }
        } else if (entityMetadata.getId() == 2) {
            updateSecondEntityStatus();
        } else if (entityMetadata.getId() == 14 && entityMetadata.getType() == MetadataType.BYTE) {
            byte xd = (byte) entityMetadata.getValue();

            // isSmall
            if ((xd & 0x01) == 0x01) {
                isSmall = true;

                if (metadata.getFloat(EntityData.SCALE) != 0.55f && metadata.getFloat(EntityData.SCALE) != 0.0f) {
                    metadata.put(EntityData.SCALE, 0.55f);
                }

                if (metadata.get(EntityData.BOUNDING_BOX_WIDTH) != null && metadata.get(EntityData.BOUNDING_BOX_WIDTH).equals(0.5f)) {
                    metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.25f);
                    metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.9875f);
                }
            } else if (metadata.get(EntityData.BOUNDING_BOX_WIDTH) != null && metadata.get(EntityData.BOUNDING_BOX_WIDTH).equals(0.25f)) {
                metadata.put(EntityData.BOUNDING_BOX_WIDTH, entityType.getWidth());
                metadata.put(EntityData.BOUNDING_BOX_HEIGHT, entityType.getHeight());
            }

            // setMarker
            if ((xd & 0x10) == 0x10 && (metadata.get(EntityData.BOUNDING_BOX_WIDTH) == null || !metadata.get(EntityData.BOUNDING_BOX_WIDTH).equals(0.0f))) {
                metadata.put(EntityData.BOUNDING_BOX_WIDTH, 0.0f);
                metadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0.0f);
                isMarker = true;
            }
        }
        if (secondEntity != null) {
            secondEntity.updateBedrockMetadata(entityMetadata, session);
        }
    }

    @Override
    public void updateBedrockMetadata(GeyserSession session) {
        if (secondEntity != null) {
            secondEntity.updateBedrockMetadata(session);
        }
        super.updateBedrockMetadata(session);
    }

    @Override
    public void setHelmet(ItemData helmet) {
        super.setHelmet(helmet);
        updateSecondEntityStatus();
    }

    @Override
    public void setChestplate(ItemData chestplate) {
        super.setChestplate(chestplate);
        updateSecondEntityStatus();
    }

    @Override
    public void setLeggings(ItemData leggings) {
        super.setLeggings(leggings);
        updateSecondEntityStatus();
    }

    @Override
    public void setBoots(ItemData boots) {
        super.setBoots(boots);
        updateSecondEntityStatus();
    }

    @Override
    public void setHand(ItemData hand) {
        super.setHand(hand);
        updateSecondEntityStatus();
    }

    @Override
    public void setOffHand(ItemData offHand) {
        super.setOffHand(offHand);
        updateSecondEntityStatus();
    }

    /**
     * Determine if we need to load or unload the second entity.
     */
    private void updateSecondEntityStatus() {
        if (!primaryEntity) return;
        if (!isInvisible || isMarker) {
            if (secondEntity != null) {
                secondEntity.despawnEntity(session);
                secondEntity = null;
                // Update the position of this armor stand
                updatePosition();
            }
            return;
        }
        if ((helmet != ItemData.AIR || chestplate != ItemData.AIR || leggings != ItemData.AIR || boots != ItemData.AIR
                || hand != ItemData.AIR || offHand != ItemData.AIR) && !metadata.getString(EntityData.NAMETAG).equals("")) {
            if (secondEntity != null) return; // No need to recreate
            // Create the second entity. It doesn't need to worry about the items, but it does need to worry about
            // the metadata as it will hold the name tag.
            secondEntity = new ArmorStandEntity(0, session.getEntityCache().getNextEntityId().incrementAndGet(),
                    EntityType.ARMOR_STAND, position, motion, rotation);
            secondEntity.primaryEntity = false;
            // Copy metadata
            secondEntity.isSmall = isSmall;
            secondEntity.getMetadata().putAll(metadata);
            // Copy the flags so they aren't the same object in memory
            secondEntity.getMetadata().putFlags(metadata.getFlags().copy());
            // Guarantee this copy is NOT invisible
            secondEntity.getMetadata().getFlags().setFlag(EntityFlag.INVISIBLE, false);
            // Scale to 0 to show nametag
            secondEntity.getMetadata().put(EntityData.SCALE, 0.0f);
            // No bounding box as we don't want to interact with this entity
            secondEntity.getMetadata().put(EntityData.BOUNDING_BOX_WIDTH, 0.0f);
            secondEntity.getMetadata().put(EntityData.BOUNDING_BOX_HEIGHT, 0.0f);
            secondEntity.spawnEntity(session);
            // Reset scale of the proper armor stand
            metadata.put(EntityData.SCALE, isSmall ? 0.55f : 1f);
            // Set the proper armor stand to invisible to show armor
            metadata.getFlags().setFlag(EntityFlag.INVISIBLE, true);

            // Update the position of the armor stands
            updatePosition();

            if (lastPositionIncludedOffset) {
                secondEntity.updatePosition();
            } else {
                secondEntity.updatePositionWithOffset();
            }
        } else if (metadata.getString(EntityData.NAMETAG).equals("")) {
            // We can just make an invisible entity
            // Reset scale of the proper armor stand
            metadata.put(EntityData.SCALE, isSmall ? 0.55f : 1f);
            // Set the proper armor stand to invisible to show armor
            metadata.getFlags().setFlag(EntityFlag.INVISIBLE, true);
            if (secondEntity != null) {
                secondEntity.despawnEntity(session);
                secondEntity = null;
                // Update the position of this armor stand
                updatePosition();
            }
        } else {
            // We don't need to make a new entity
            updatePositionWithOffset();
            metadata.getFlags().setFlag(EntityFlag.INVISIBLE, false);
            metadata.put(EntityData.SCALE, 0.0f);
            if (secondEntity != null) {
                secondEntity.despawnEntity(session);
                secondEntity = null;
            }
        }
        this.updateBedrockMetadata(session);
    }

    /**
     * Updates position without calling movement code.
     */
    private void updatePosition() {
        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(lastPositionIncludedOffset ? position.sub(0d, entityType.getHeight() * (isSmall ? 0.55d : 1d), 0d) : position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(onGround);
        moveEntityPacket.setTeleported(false);
        session.sendUpstreamPacket(moveEntityPacket);
    }

    /**
     * Updates position without calling movement code and includes the offset.
     */
    private void updatePositionWithOffset() {
        System.out.println(secondEntity);
        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(primaryEntity && lastPositionIncludedOffset ?
                position : position.add(0d, entityType.getHeight() * (isSmall ? 0.55d : 1d), 0d));
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(onGround);
        moveEntityPacket.setTeleported(false);
        session.sendUpstreamPacket(moveEntityPacket);
    }

    @Override
    public Vector3f getBedrockRotation() {
        return Vector3f.from(rotation.getY(), rotation.getX(), rotation.getZ());
    }
}
