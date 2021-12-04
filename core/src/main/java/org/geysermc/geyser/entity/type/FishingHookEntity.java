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

package org.geysermc.geyser.entity.type;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import lombok.Getter;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.level.block.BlockPositionIterator;
import org.geysermc.geyser.util.BlockUtils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FishingHookEntity extends ThrowableEntity {

    private boolean hooked = false;
    private boolean inWater = false;

    @Getter
    private final boolean isOwnerSessionPlayer;
    @Getter
    private long bedrockTargetId;

    private final BoundingBox boundingBox;

    public FishingHookEntity(GeyserSession session, long entityId, long geyserId, UUID uuid, Vector3f position, Vector3f motion, float yaw, float pitch, PlayerEntity owner) {
        super(session, entityId, geyserId, uuid, EntityDefinitions.FISHING_BOBBER, position, motion, yaw, pitch, 0f);

        this.boundingBox = new BoundingBox(0.125, 0.125, 0.125, 0.25, 0.25, 0.25);

        // In Java, the splash sound depends on the entity's velocity, but in Bedrock the volume doesn't change.
        // This splash can be confused with the sound from catching a fish. This silences the splash from Bedrock,
        // so that it can be handled by moveAbsoluteImmediate.
        setBoundingBoxHeight(128);

        isOwnerSessionPlayer = owner.getGeyserId() == session.getPlayerEntity().getGeyserId();
        this.dirtyMetadata.put(EntityData.OWNER_EID, owner.getGeyserId());
    }

    @Override
    public void spawnEntity() {

        super.spawnEntity();
    }

    public void setHookedEntity(IntEntityMetadata entityMetadata) {
        int hookedEntityId = entityMetadata.getPrimitiveValue() - 1;
        Entity entity;
        if (session.getPlayerEntity().getEntityId() == hookedEntityId) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(hookedEntityId);
        }

        if (entity != null) {
            bedrockTargetId = entity.getGeyserId();
            dirtyMetadata.put(EntityData.TARGET_EID, bedrockTargetId);
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

            int waterLevel = BlockStateValues.getWaterLevel(blockID);
            if (BlockRegistries.WATERLOGGED.get().contains(blockID)) {
                waterLevel = 0;
            }
            if (waterLevel >= 0) {
                double waterMaxY = iter.getY() + 1 - (waterLevel + 1) / 9.0;
                // Falling water is a full block
                if (waterLevel >= 8) {
                    waterMaxY = iter.getY() + 1;
                }
                if (position.getY() <= waterMaxY) {
                    touchingWater = true;
                }
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
        if (!getFlag(EntityFlag.SILENT)) {
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
        if (hooked || !isInAir() && !isInWater() || isOnGround()) {
            motion = Vector3f.ZERO;
            return;
        }
        float gravity = getGravity();
        motion = motion.down(gravity);

        moveAbsoluteImmediate(position.add(motion), yaw, pitch, headYaw, onGround, false);

        float drag = getDrag();
        motion = motion.mul(drag);
    }

    @Override
    protected float getGravity() {
        if (!isInWater() && !onGround) {
            return 0.03f;
        }
        return 0;
    }

    /**
     * @return true if this entity is currently in air.
     */
    protected boolean isInAir() {
        int block = session.getGeyser().getWorldManager().getBlockAt(session, position.toInt());
        return block == BlockStateValues.JAVA_AIR_ID;
    }

    @Override
    protected float getDrag() {
        return 0.92f;
    }
}
