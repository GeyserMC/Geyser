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

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.player.SessionPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionManager;
import org.geysermc.connector.network.translators.collision.CollisionTranslator;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.world.block.entity.PistonBlockEntity;
import org.geysermc.connector.utils.Axis;

import java.util.List;
import java.util.Map;

public class PistonCache {
    private final GeyserSession session;

    private final Map<Vector3i, PistonBlockEntity> pistons = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    @Getter
    private Vector3d playerDisplacement = Vector3d.ZERO;

    @Getter @Setter
    private Vector3f playerMotion = Vector3f.ZERO;

    /**
     * Stores whether a player has/will collide with any moving blocks.
     * This is used to prevent motion from being reset while inside a moving block.
     */
    @Getter @Setter
    private boolean playerCollided = false;

    /**
     * Stores whether a player has/will collide with any slime blocks.
     * This is used to prevent movement from being canceled when players
     * are about to hit a slime block.
     */
    @Getter @Setter
    private boolean playerSlimeCollision = false;

    public PistonCache(GeyserSession session) {
        this.session = session;
    }

    public synchronized void tick() {
        resetPlayerMovement();
        pistons.values().forEach(PistonBlockEntity::updateMovement);
        sendPlayerMovement(true);
        sendPlayerMotion();
        // Update blocks after movement, so that players don't get stuck inside blocks
        pistons.values().forEach(PistonBlockEntity::updateBlocks);
    }

    public synchronized Vector3d correctPlayerMovement(Vector3d movement) {
        double movementX = movement.getX();
        double movementY = movement.getY();
        double movementZ = movement.getZ();

        BoundingBox playerBoundingBox = session.getCollisionManager().getPlayerBoundingBox();
        playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() + CollisionManager.COLLISION_TOLERANCE * 2);
        playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() + CollisionManager.COLLISION_TOLERANCE * 2);

        BoundingBox movementBoundingBox = playerBoundingBox.clone();
        movementBoundingBox.extend(movement);
        List<Vector3i> collidableBlocks = session.getCollisionManager().getCollidableBlocks(movementBoundingBox);

        // TODO Check collisions with the world?
        // TODO Step up
        if (Math.abs(movementY) > CollisionManager.COLLISION_TOLERANCE) {
            for (PistonBlockEntity piston : pistons.values()) {
                movementY = piston.computeCollisionOffset(playerBoundingBox, Axis.Y, movementY);
            }
            //movementY = computeWorldCollisionOffset(playerBoundingBox, Axis.Y, movementY, collidableBlocks);
            playerBoundingBox.translate(0, movementY, 0);
        }
        boolean checkZFirst = Math.abs(movementZ) > Math.abs(movementX);
        if (Math.abs(movementZ) > CollisionManager.COLLISION_TOLERANCE && checkZFirst) {
            for (PistonBlockEntity piston : pistons.values()) {
                movementZ = piston.computeCollisionOffset(playerBoundingBox, Axis.Z, movementZ);
            }
            //movementZ = computeWorldCollisionOffset(playerBoundingBox, Axis.Z, movementZ, collidableBlocks);
            playerBoundingBox.translate(0, 0, movementZ);
        }
        if (Math.abs(movementX) > CollisionManager.COLLISION_TOLERANCE) {
            for (PistonBlockEntity piston : pistons.values()) {
                movementX = piston.computeCollisionOffset(playerBoundingBox, Axis.X, movementX);
            }
            //movementX = computeWorldCollisionOffset(playerBoundingBox, Axis.X, movementX, collidableBlocks);
            playerBoundingBox.translate(movementX, 0, 0);
        }
        if (Math.abs(movementZ) > CollisionManager.COLLISION_TOLERANCE && !checkZFirst) {
            for (PistonBlockEntity piston : pistons.values()) {
                movementZ = piston.computeCollisionOffset(playerBoundingBox, Axis.Z, movementZ);
            }
            //movementZ = computeWorldCollisionOffset(playerBoundingBox, Axis.Z, movementZ, collidableBlocks);
            playerBoundingBox.translate(0, 0, movementZ);
        }

        playerBoundingBox.setSizeX(playerBoundingBox.getSizeX() - CollisionManager.COLLISION_TOLERANCE * 2);
        playerBoundingBox.setSizeZ(playerBoundingBox.getSizeZ() - CollisionManager.COLLISION_TOLERANCE * 2);

        pistons.entrySet().removeIf((entry) -> entry.getValue().isDone());
        return Vector3d.from(movementX, movementY, movementZ);
    }

    public double computeWorldCollisionOffset(BoundingBox boundingBox, Axis axis, double offset, List<Vector3i> collidableBlocks) {
        for (Vector3i blockPos : collidableBlocks) {
            int blockId = session.getConnector().getWorldManager().getBlockAt(session, blockPos);
            BlockCollision blockCollision = CollisionTranslator.getCollision(blockId, 0, 0, 0);
            offset = blockCollision.computeCollisionOffset(blockPos.toDouble(), boundingBox, axis, offset);
        }
        return offset;
    }

    private void resetPlayerMovement() {
        playerDisplacement = Vector3d.ZERO;
        playerCollided = false;
        playerSlimeCollision = false;
    }

    public void sendPlayerMovement(boolean sendToJava) {
        SessionPlayerEntity playerEntity = session.getPlayerEntity();
        if (!playerDisplacement.equals(Vector3d.ZERO) || playerDisplacement.getY() > 0) {
            CollisionManager collisionManager = session.getCollisionManager();
            if (collisionManager.correctPlayerPosition()) {
                Vector3d position = collisionManager.getPlayerBoundingBox().getBottomCenter();

                boolean isOnGround = playerDisplacement.getY() > 0 || playerEntity.isOnGround();

                if (playerMotion.getX() == 0 && playerMotion.getZ() == 0) {
                    playerEntity.moveAbsolute(session, position.toFloat(), playerEntity.getRotation(), isOnGround, true);
                }

                if (sendToJava) {
                    ClientPlayerPositionPacket playerPositionPacket = new ClientPlayerPositionPacket(isOnGround, position.getX(), position.getY(), position.getZ());
                    session.sendDownstreamPacket(playerPositionPacket);

                    session.setLastMovementTimestamp(System.currentTimeMillis());
                }
            }
        }
    }

    public void sendPlayerMotion() {
        if (!playerMotion.equals(Vector3f.ZERO)) {
            SessionPlayerEntity playerEntity = session.getPlayerEntity();
            playerEntity.setMotion(playerMotion);
            SetEntityMotionPacket setEntityMotionPacket = new SetEntityMotionPacket();
            setEntityMotionPacket.setRuntimeEntityId(playerEntity.getGeyserId());
            setEntityMotionPacket.setMotion(playerMotion);
            session.sendUpstreamPacket(setEntityMotionPacket);

            if (!isColliding()) {
                playerMotion = Vector3f.ZERO;
            }
        }
    }

    /**
     * Set the player displacement and move the player's bounding box
     * Displacement is capped to a range of -0.51 to 0.51
     *
     * @param displacement The new player displacement
     */
    public void setPlayerDisplacement(Vector3d displacement) {
        // Clamp to range -0.51 to 0.51
        displacement = displacement.max(-0.51d, -0.51d, -0.51d).min(0.51d, 0.51d, 0.51d);
        Vector3d delta = displacement.sub(playerDisplacement);
        session.getCollisionManager().getPlayerBoundingBox().translate(delta.getX(), delta.getY(), delta.getZ());
        playerDisplacement = displacement;
    }

    public synchronized PistonBlockEntity getPistonAt(Vector3i position) {
        return pistons.get(position);
    }

    public synchronized void putPiston(PistonBlockEntity pistonBlockEntity) {
        pistons.put(pistonBlockEntity.getPosition(), pistonBlockEntity);
    }

    public void clear() {
        pistons.clear();
    }

    private boolean isColliding() {
        return !playerDisplacement.equals(Vector3d.ZERO) || playerCollided;
    }
}
