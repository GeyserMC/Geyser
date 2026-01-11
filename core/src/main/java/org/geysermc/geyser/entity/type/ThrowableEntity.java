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
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;

/**
 * Used as a class for any object-like entity that moves as a projectile
 */
public class ThrowableEntity extends Entity implements Tickable {

    protected Vector3f lastJavaPosition;

    public ThrowableEntity(EntitySpawnContext context) {
        super(context);
        this.lastJavaPosition = position();
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
        moveAbsoluteImmediate(position().add(motion), getYaw(), getPitch(), getHeadYaw(), isOnGround(), false);
        float drag = getDrag();
        float gravity = getGravity();
        motion = motion.mul(drag).down(gravity);
    }

    // TODO offsets!!!
    protected void moveAbsoluteImmediate(Vector3f javaPosition, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        MoveEntityDeltaPacket moveEntityDeltaPacket = new MoveEntityDeltaPacket();
        moveEntityDeltaPacket.setRuntimeEntityId(geyserId);

        if (isOnGround) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
        }
        setOnGround(isOnGround);

        if (teleported) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.TELEPORTING);
        }

        if (this.position().getX() != javaPosition.getX()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
            moveEntityDeltaPacket.setX(javaPosition.getX());
        }
        if (this.position().getY() != javaPosition.getY()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
            moveEntityDeltaPacket.setY(javaPosition.getY() + offset);
        }
        if (this.position().getZ() != javaPosition.getZ()) {
            moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);
            moveEntityDeltaPacket.setZ(javaPosition.getZ());
        }
        position(javaPosition);

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
            session.getQueuedImmediatelyPackets().add(moveEntityDeltaPacket);
        }
    }

    /**
     * Get the gravity of this entity type. Used for applying gravity while the entity is in motion.
     *
     * @return the amount of gravity to apply to this entity while in motion.
     */
    protected float getGravity() {
        if (getFlag(EntityFlag.HAS_GRAVITY)) {
            if (javaTypeDefinition.is(BuiltinEntityType.LINGERING_POTION) || javaTypeDefinition.is(BuiltinEntityType.SPLASH_POTION)) {
                return 0.05f;
            } else if (javaTypeDefinition.is(BuiltinEntityType.EXPERIENCE_BOTTLE)) {
                return 0.07f;
            } else if (javaTypeDefinition.is(BuiltinEntityType.FIREBALL) || javaTypeDefinition.is(BuiltinEntityType.SHULKER_BULLET)) {
                return 0;
            } else if (javaTypeDefinition.is(BuiltinEntityType.SNOWBALL) || javaTypeDefinition.is(BuiltinEntityType.EGG) || javaTypeDefinition.is(BuiltinEntityType.ENDER_PEARL)) {
                return 0.03f;
            } else if (javaTypeDefinition.is(BuiltinEntityType.LLAMA_SPIT)) {
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
            if (javaTypeDefinition.is(BuiltinEntityType.LINGERING_POTION) || javaTypeDefinition.is(BuiltinEntityType.SPLASH_POTION) || javaTypeDefinition.is(BuiltinEntityType.EXPERIENCE_BOTTLE)
                || javaTypeDefinition.is(BuiltinEntityType.SNOWBALL) || javaTypeDefinition.is(BuiltinEntityType.EGG) || javaTypeDefinition.is(BuiltinEntityType.ENDER_PEARL) || javaTypeDefinition.is(BuiltinEntityType.LLAMA_SPIT)) {
                return 0.99f;
            } else if (javaTypeDefinition.is(BuiltinEntityType.FIREBALL) || javaTypeDefinition.is(BuiltinEntityType.SMALL_FIREBALL) || javaTypeDefinition.is(BuiltinEntityType.DRAGON_FIREBALL)) {
                return 0.95f;
            } else if (javaTypeDefinition.is(BuiltinEntityType.SHULKER_BULLET)) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * @return true if this entity is currently in water.
     */
    protected boolean isInWater() {
        int block = session.getGeyser().getWorldManager().getBlockAt(session, position().toInt());
        return BlockStateValues.getWaterLevel(block) != -1;
    }

    @Override
    public void despawnEntity() {
        if (javaTypeDefinition.is(BuiltinEntityType.ENDER_PEARL)) {
            LevelEventPacket particlePacket = new LevelEventPacket();
            particlePacket.setType(LevelEvent.PARTICLE_TELEPORT);
            particlePacket.setPosition(bedrockPosition());
            session.sendUpstreamPacket(particlePacket);
        }
        super.despawnEntity();
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        moveAbsoluteImmediate(lastJavaPosition.add(relX, relY, relZ), yaw, pitch, headYaw, isOnGround, false);
        lastJavaPosition = position();
    }

    @Override
    public void moveAbsoluteRaw(Vector3f javaPosition, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        moveAbsoluteImmediate(javaPosition, yaw, pitch, headYaw, isOnGround, teleported);
        lastJavaPosition = javaPosition;
    }

    /**
     * Removes the entity if it is 64 blocks below the world.
     *
     * @return true if the entity was removed
     */
    public boolean removedInVoid() {
        if (position().getY() < session.getDimensionType().minY() - 64) {
            session.getEntityCache().removeEntity(this);
            return true;
        }
        return false;
    }
}
