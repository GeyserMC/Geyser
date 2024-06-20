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

import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FishingHookEntity extends ThrowableEntity {

    private boolean hooked = false;
    private boolean inWater = false;

    @Getter
    private final long bedrockOwnerId;
    @Getter
    private long bedrockTargetId;

    private final BoundingBox boundingBox;

    public FishingHookEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw, PlayerEntity owner) {
        super(session, entityId, geyserId, uuid, EntityDefinitions.FISHING_BOBBER, position, motion, yaw, pitch, 0f);

        this.boundingBox = new BoundingBox(0.125, 0.125, 0.125, 0.25, 0.25, 0.25);

        // In Java, the splash sound depends on the entity's velocity, but in Bedrock the volume doesn't change.
        // This splash can be confused with the sound from catching a fish. This silences the splash from Bedrock,
        // so that it can be handled by moveAbsoluteImmediate.
        setBoundingBoxHeight(128);

        this.bedrockOwnerId = owner.getGeyserId();
        this.dirtyMetadata.put(EntityDataTypes.OWNER_EID, this.bedrockOwnerId);
    }

    public void setHookedEntity(IntEntityMetadata entityMetadata) {
        int hookedEntityId = entityMetadata.getPrimitiveValue() - 1;
        Entity entity = session.getEntityCache().getEntityByJavaId(hookedEntityId);
        if (entity != null) {
            bedrockTargetId = entity.getGeyserId();
            dirtyMetadata.put(EntityDataTypes.TARGET_EID, bedrockTargetId);
            hooked = true;
        } else {
            hooked = false;
        }
    }

    @Override
    protected void moveAbsoluteImmediate(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        boundingBox.setMiddleX(position.getX());
        boundingBox.setMiddleY(position.getY() + boundingBox.getSizeY() / 2);
        boundingBox.setMiddleZ(position.getZ());

        boolean touchingWater = false;
        boolean collided = false;
        for (BlockPositionIterator iter = session.getCollisionManager().collidableBlocksIterator(boundingBox); iter.hasNext(); iter.next()) {
            int blockID = session.getGeyser().getWorldManager().getBlockAt(session, iter.getX(), iter.getY(), iter.getZ());
            BlockCollision blockCollision = BlockUtils.getCollision(blockID);
            if (blockCollision != null) {
                if (blockCollision.checkIntersection(iter.getX(), iter.getY(), iter.getZ(), boundingBox)) {
                    // TODO Push bounding box out of collision to improve movement
                    collided = true;
                }
            }

            double waterHeight = BlockStateValues.getWaterHeight(blockID);
            if (waterHeight != -1 && position.getY() <= (iter.getY() + waterHeight)) {
                touchingWater = true;
            }
        }

        if (!inWater && touchingWater) {
            sendSplashSound(session);
        }
        inWater = touchingWater;

        if (!collided) {
            super.moveAbsoluteImmediate(position, yaw, pitch, headYaw, isOnGround, teleported);
        } else {
            super.moveAbsoluteImmediate(this.position, yaw, pitch, headYaw, true, true);
        }
    }

    private void sendSplashSound(GeyserSession session) {
        if (!silent) {
            float volume = (float) (0.2f * Math.sqrt(0.2 * (motion.getX() * motion.getX() + motion.getZ() * motion.getZ()) + motion.getY() * motion.getY()));
            if (volume > 1) {
                volume = 1;
            }
            PlaySoundPacket playSoundPacket = new PlaySoundPacket();
            playSoundPacket.setSound("random.splash");
            playSoundPacket.setPosition(position);
            playSoundPacket.setVolume(volume);
            playSoundPacket.setPitch(1f + ThreadLocalRandom.current().nextFloat() * 0.3f);
            session.sendUpstreamPacket(playSoundPacket);
        }
    }

    @Override
    public void tick() {
        if (removedInVoid()) {
            return;
        }
        if (hooked || !isInAir() && !isInWater() || isOnGround()) {
            motion = Vector3f.ZERO;
            return;
        }
        float gravity = getGravity();
        motion = motion.down(gravity);

        moveAbsoluteImmediate(position.add(motion), getYaw(), getPitch(), getHeadYaw(), isOnGround(), false);

        float drag = getDrag();
        motion = motion.mul(drag);
    }

    @Override
    protected float getGravity() {
        if (!isInWater() && !isOnGround()) {
            return 0.03f;
        }
        return 0;
    }

    /**
     * @return true if this entity is currently in air.
     */
    protected boolean isInAir() {
        int block = session.getGeyser().getWorldManager().getBlockAt(session, position.toInt());
        return block == Block.JAVA_AIR_ID;
    }

    @Override
    protected float getDrag() {
        return 0.92f;
    }
}
