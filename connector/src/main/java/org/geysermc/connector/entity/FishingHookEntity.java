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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionManager;
import org.geysermc.connector.network.translators.collision.CollisionTranslator;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FishingHookEntity extends ThrowableEntity {

    private boolean hooked = false;

    private final BoundingBox boundingBox;

    private boolean inWater = false;

    public FishingHookEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation, PlayerEntity owner) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        this.boundingBox = new BoundingBox(0.125, 0.125, 0.125, 0.25, 0.25, 0.25);

        // Silence splash sounds from bedrock
        this.metadata.putFloat(EntityData.BOUNDING_BOX_HEIGHT, 1e6f);

        this.metadata.put(EntityData.OWNER_EID, owner.getGeyserId());
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 7) { // Hooked entity
            int hookedEntityId = (int) entityMetadata.getValue() - 1;
            Entity entity = session.getEntityCache().getEntityByJavaId(hookedEntityId);
            if (entity == null && session.getPlayerEntity().getEntityId() == hookedEntityId) {
                entity = session.getPlayerEntity();
            }

            if (entity != null) {
                metadata.put(EntityData.TARGET_EID, entity.getGeyserId());
                hooked = true;
            } else {
                hooked = false;
            }
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    protected void moveAbsoluteImmediate(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        boundingBox.setMiddleX(position.getX());
        boundingBox.setMiddleY(position.getY() - boundingBox.getSizeY() / 2);
        boundingBox.setMiddleZ(position.getZ());
        double minY = boundingBox.getMiddleY() - boundingBox.getSizeY() / 2;

        CollisionManager collisionManager = session.getCollisionManager();
        List<Vector3i> collidableBlocks = collisionManager.getCollidableBlocks(boundingBox);
        boolean touchingWater = false;
        for (Vector3i blockPos : collidableBlocks) {
            int blockID = session.getConnector().getWorldManager().getBlockAt(session, blockPos);
            BlockCollision blockCollision = CollisionTranslator.getCollision(blockID, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (blockCollision != null && blockCollision.checkIntersection(boundingBox)) {
                // TODO Push bounding box out of collision to improve movement
                return;
            }

            int waterLevel = BlockStateValues.getWaterLevel(blockID);
            if (BlockTranslator.isWaterlogged(blockID)) {
                waterLevel = 0;
            }
            if (waterLevel >= 0) {
                // Flowing water is the same height as still water
                if (waterLevel >= 8) {
                    waterLevel = 0;
                }
                double waterMaxY = blockPos.getY() + 1 - (waterLevel + 1) / 9.0;
                if (minY <= waterMaxY) {
                    touchingWater = true;
                }
            }
        }

        if (touchingWater) {
            if (!inWater) {
                inWater = true;
                // Splash
                if (!metadata.getFlags().getFlag(EntityFlag.SILENT)) {
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
        } else {
            inWater = false;
        }

        super.moveAbsoluteImmediate(session, position, rotation, isOnGround, teleported);
    }

    @Override
    public void tick(GeyserSession session) {
        if (hooked || !isInAir(session) && !isInWater(session) || isOnGround()) {
            motion = Vector3f.ZERO;
            return;
        }
        float gravity = getGravity(session);
        motion = motion.down(gravity);

        moveAbsoluteImmediate(session, position.add(motion), rotation, onGround, false);

        float drag = getDrag(session);
        motion = motion.mul(drag);
    }

    @Override
    protected float getGravity(GeyserSession session) {
        if (!isInWater(session) && !onGround) {
            return 0.03f;
        }
        return 0;
    }

    /**
     * @param session the session of the Bedrock client.
     * @return true if this entity is currently in air.
     */
    protected boolean isInAir(GeyserSession session) {
        if (session.getConnector().getConfig().isCacheChunks()) {
            if (0 <= position.getFloorY() && position.getFloorY() <= 255) {
                int block = session.getConnector().getWorldManager().getBlockAt(session, position.toInt());
                return block == BlockTranslator.JAVA_AIR_ID;
            }
        }
        return false;
    }

    @Override
    protected float getDrag(GeyserSession session) {
        return 0.92f;
    }
}
