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

package org.geysermc.connector.entity;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Used as a class for any object-like entity that moves as a projectile
 */
public class ThrowableEntity extends Entity {

    private Vector3f lastPosition;
    /**
     * Updates the position for the Bedrock client.
     *
     * Java clients assume the next positions of moving items. Bedrock needs to be explicitly told positions
     */
    protected ScheduledFuture<?> positionUpdater;

    public ThrowableEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        this.lastPosition = position;
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        super.spawnEntity(session);
        positionUpdater = session.getConnector().getGeneralThreadPool().scheduleAtFixedRate(() -> {
            if (session.isClosed()) {
                positionUpdater.cancel(true);
                return;
            }
            updatePosition(session);
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    protected void moveAbsoluteImmediate(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        super.moveAbsolute(session, position, rotation, isOnGround, teleported);
    }

    protected void updatePosition(GeyserSession session) {
        super.moveRelative(session, motion.getX(), motion.getY(), motion.getZ(), rotation, onGround);
        float drag = getDrag(session);
        float gravity = getGravity();
        motion = motion.mul(drag).down(gravity);
    }

    /**
     * Get the gravity of this entity type. Used for applying gravity while the entity is in motion.
     *
     * @return the amount of gravity to apply to this entity while in motion.
     */
    protected float getGravity() {
        if (metadata.getFlags().getFlag(EntityFlag.HAS_GRAVITY)) {
            switch (entityType) {
                case THROWN_POTION:
                case LINGERING_POTION:
                    return 0.05f;
                case THROWN_EXP_BOTTLE:
                    return 0.07f;
                case FIREBALL:
                    return 0;
                case SNOWBALL:
                case THROWN_EGG:
                case THROWN_ENDERPEARL:
                    return 0.03f;
            }
        }
        return 0;
    }

    /**
     * @param session the session of the Bedrock client.
     * @return the drag that should be multiplied to the entity's motion
     */
    protected float getDrag(GeyserSession session) {
        if (isInWater(session)) {
            return 0.8f;
        } else {
            switch (entityType) {
                case THROWN_POTION:
                case LINGERING_POTION:
                case THROWN_EXP_BOTTLE:
                case SNOWBALL:
                case THROWN_EGG:
                case THROWN_ENDERPEARL:
                    return 0.99f;
                case FIREBALL:
                case SMALL_FIREBALL:
                case DRAGON_FIREBALL:
                    return 0.95f;
            }
        }
        return 1;
    }

    /**
     * @param session the session of the Bedrock client.
     * @return true if this entity is currently in water.
     */
    protected boolean isInWater(GeyserSession session) {
        if (session.getConnector().getConfig().isCacheChunks()) {
            int block = session.getConnector().getWorldManager().getBlockAt(session, position.toInt());
            return block == BlockTranslator.BEDROCK_WATER_ID;
        }
        return false;
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        positionUpdater.cancel(true);
        if (entityType == EntityType.THROWN_ENDERPEARL) {
            LevelEventPacket particlePacket = new LevelEventPacket();
            particlePacket.setType(LevelEventType.PARTICLE_TELEPORT);
            particlePacket.setPosition(position);
            session.sendUpstreamPacket(particlePacket);
        }
        return super.despawnEntity(session);
    }

    @Override
    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        position = lastPosition;
        super.moveRelative(session, relX, relY, relZ, rotation, isOnGround);
        lastPosition = position;
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        super.moveAbsolute(session, position, rotation, isOnGround, teleported);
        lastPosition = position;
    }
}
