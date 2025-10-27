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

package org.geysermc.geyser.entity.type;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;

import java.util.UUID;

/**
 * Used as a class for any object-like entity that moves as a projectile
 */
public class ThrowableEntity extends Entity implements Tickable {

    protected Vector3f lastJavaPosition;

    public ThrowableEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
        this.lastJavaPosition = position;
    }

    /**
     * Updates the position for the Bedrock client.
     * Java clients assume the next positions of moving items. Bedrock needs to be explicitly told positions
     */
    @Override
    public void tick() {
        if (removedInVoid()) {
            return;
        }
        moveAbsoluteImmediate(position.add(motion), getYaw(), getPitch(), getHeadYaw(), isOnGround(), false);
        float drag = getDrag();
        float gravity = getGravity();
        motion = motion.mul(drag).down(gravity);
    }

    protected void moveAbsoluteImmediate(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        MoveEntityDeltaPacket moveEntityDeltaPacket = new MoveEntityDeltaPacket();
        moveEntityDeltaPacket.setRuntimeEntityId(geyserId);

        if (isOnGround) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
        }
        setOnGround(isOnGround);

        if (teleported) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.TELEPORTING);
        }

        if (this.position.getX() != position.getX()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
            moveEntityDeltaPacket.setX(position.getX());
        }
        if (this.position.getY() != position.getY()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
            moveEntityDeltaPacket.setY(position.getY());
        }
        if (this.position.getZ() != position.getZ()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);
            moveEntityDeltaPacket.setZ(position.getZ());
        }
        setPosition(position);

        if (getYaw() != yaw) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
            moveEntityDeltaPacket.setYaw(yaw);
            setYaw(yaw);
        }
        if (getPitch() != pitch) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);
            moveEntityDeltaPacket.setPitch(pitch);
            setPitch(pitch);
        }
        if (getHeadYaw() != headYaw) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
            moveEntityDeltaPacket.setHeadYaw(headYaw);
            setHeadYaw(headYaw);
        }

        if (!moveEntityDeltaPacket.getFlags().isEmpty()) {
            session.sendUpstreamPacket(moveEntityDeltaPacket);
        }
    }

    /**
     * Get the gravity of this entity type. Used for applying gravity while the entity is in motion.
     *
     * @return the amount of gravity to apply to this entity while in motion.
     */
    protected float getGravity() {
        if (getFlag(EntityFlag.HAS_GRAVITY)) {
            if (definition.is(BuiltinEntityType.LINGERING_POTION) || definition.is(BuiltinEntityType.SPLASH_POTION)) {
                return 0.05f;
            } else if (definition.is(BuiltinEntityType.EXPERIENCE_BOTTLE)) {
                return 0.07f;
            } else if (definition.is(BuiltinEntityType.FIREBALL) || definition.is(BuiltinEntityType.SHULKER_BULLET)) {
                return 0;
            } else if (definition.is(BuiltinEntityType.SNOWBALL) || definition.is(BuiltinEntityType.EGG) || definition.is(BuiltinEntityType.ENDER_PEARL)) {
                return 0.03f;
            } else if (definition.is(BuiltinEntityType.LLAMA_SPIT)) {
                return 0.06f;
            }
        }
        return 0;
    }

    /**
     * @return the drag that should be multiplied to the entity's motion
     */
    protected float getDrag() {
        if (isInWater()) {
            return 0.8f;
        } else {
            if (definition.is(BuiltinEntityType.LINGERING_POTION) || definition.is(BuiltinEntityType.SPLASH_POTION) || definition.is(BuiltinEntityType.EXPERIENCE_BOTTLE)
                || definition.is(BuiltinEntityType.SNOWBALL) || definition.is(BuiltinEntityType.EGG) || definition.is(BuiltinEntityType.ENDER_PEARL) || definition.is(BuiltinEntityType.LLAMA_SPIT)) {
                return 0.99f;
            } else if (definition.is(BuiltinEntityType.FIREBALL) || definition.is(BuiltinEntityType.SMALL_FIREBALL) || definition.is(BuiltinEntityType.DRAGON_FIREBALL)) {
                return 0.95f;
            } else if (definition.is(BuiltinEntityType.SHULKER_BULLET)) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * @return true if this entity is currently in water.
     */
    protected boolean isInWater() {
        int block = session.getGeyser().getWorldManager().getBlockAt(session, position.toInt());
        return BlockStateValues.getWaterLevel(block) != -1;
    }

    @Override
    public void despawnEntity() {
        if (definition.is(BuiltinEntityType.ENDER_PEARL)) {
            LevelEventPacket particlePacket = new LevelEventPacket();
            particlePacket.setType(LevelEvent.PARTICLE_TELEPORT);
            particlePacket.setPosition(position);
            session.sendUpstreamPacket(particlePacket);
        }
        super.despawnEntity();
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        moveAbsoluteImmediate(lastJavaPosition.add(relX, relY, relZ), yaw, pitch, headYaw, isOnGround, false);
        lastJavaPosition = position;
    }

    @Override
    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        moveAbsoluteImmediate(position, yaw, pitch, headYaw, isOnGround, teleported);
        lastJavaPosition = position;
    }

    /**
     * Removes the entity if it is 64 blocks below the world.
     *
     * @return true if the entity was removed
     */
    public boolean removedInVoid() {
        if (position.getY() < session.getDimensionType().minY() - 64) {
            session.getEntityCache().removeEntity(this);
            return true;
        }
        return false;
    }
}
