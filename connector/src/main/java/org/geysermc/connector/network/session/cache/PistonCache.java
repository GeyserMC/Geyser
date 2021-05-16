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
import org.geysermc.connector.network.translators.world.block.entity.PistonBlockEntity;
import org.geysermc.connector.utils.Axis;

import java.util.Map;

public class PistonCache {
    private final GeyserSession session;

    /**
     * Maps the position of a piston to its block entity
     */
    private final Map<Vector3i, PistonBlockEntity> pistons = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    /**
     * Maps the position of a moving block to the piston moving it
     */
    @Getter
    private final Map<Vector3i, PistonBlockEntity> movingBlocksMap = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

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
        sendPlayerMovement();
        sendPlayerMotion();
        // Update blocks after movement, so that players don't get stuck inside blocks
        pistons.values().forEach(PistonBlockEntity::updateBlocks);

        pistons.entrySet().removeIf((entry) -> entry.getValue().isDone());
        if (pistons.isEmpty() && !movingBlocksMap.isEmpty()) {
            session.getConnector().getLogger().error("Moving block map de-synced");
            for (Map.Entry<Vector3i, PistonBlockEntity> entry : movingBlocksMap.entrySet()) {
                Vector3i position = entry.getKey();
                PistonBlockEntity pistonBlockEntity = entry.getValue();
                session.getConnector().getLogger().error("Moving Block at " + position + " was previously owned by piston at " + pistonBlockEntity.getPosition());
            }
        }
    }

    private void resetPlayerMovement() {
        playerDisplacement = Vector3d.ZERO;
        playerCollided = false;
        playerSlimeCollision = false;
    }

    private void sendPlayerMovement() {
        SessionPlayerEntity playerEntity = session.getPlayerEntity();
        if (!playerDisplacement.equals(Vector3d.ZERO) || playerDisplacement.getY() > 0) {
            CollisionManager collisionManager = session.getCollisionManager();
            BoundingBox playerBoundingBox = collisionManager.getPlayerBoundingBox();
            playerBoundingBox.translate(-playerDisplacement.getX(), -playerDisplacement.getY(), -playerDisplacement.getZ());
            playerDisplacement = collisionManager.correctPlayerMovement(playerDisplacement, true);
            playerBoundingBox.translate(playerDisplacement.getX(), playerDisplacement.getY(), playerDisplacement.getZ());
            if (collisionManager.correctPlayerPosition()) {
                Vector3d position = collisionManager.getPlayerBoundingBox().getBottomCenter();

                boolean isOnGround = playerDisplacement.getY() > 0 || playerEntity.isOnGround();

                if (playerMotion.getX() == 0 && playerMotion.getZ() == 0) {
                    playerEntity.moveAbsolute(session, position.toFloat(), playerEntity.getRotation(), isOnGround, true);
                }
            }
        }
    }

    private void sendPlayerMotion() {
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

    public synchronized double computeCollisionOffset(Vector3i blockPos, BoundingBox boundingBox, Axis axis, double offset) {
        PistonBlockEntity piston = movingBlocksMap.get(blockPos);
        if (piston != null) {
            return piston.computeCollisionOffset(blockPos, boundingBox, axis, offset);
        }
        return offset;
    }

    public synchronized PistonBlockEntity getPistonAt(Vector3i position) {
        return pistons.get(position);
    }

    public synchronized void putPiston(PistonBlockEntity pistonBlockEntity) {
        pistons.put(pistonBlockEntity.getPosition(), pistonBlockEntity);
    }

    public void clear() {
        pistons.clear();
        movingBlocksMap.clear();
    }

    private boolean isColliding() {
        return !playerDisplacement.equals(Vector3d.ZERO) || playerCollided;
    }
}
