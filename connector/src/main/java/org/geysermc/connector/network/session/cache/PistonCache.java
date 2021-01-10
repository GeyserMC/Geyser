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
import org.geysermc.connector.network.translators.collision.CollisionManager;
import org.geysermc.connector.network.translators.world.block.entity.PistonBlockEntity;

import java.util.Map;

public class PistonCache {
    private final GeyserSession session;

    private final Map<Vector3i, PistonBlockEntity> pistons = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    @Getter @Setter
    private Vector3d playerDisplacement = Vector3d.ZERO;
    @Getter @Setter
    private Vector3f playerMotion = Vector3f.ZERO;

    /**
     * Stores whether a player has/will collide with any moving blocks.
     * This is used to cancel movement from Bedrock that pushes players
     * out of collision.
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

    public void tick() {
        resetPlayerMovement();
        pistons.values().forEach(PistonBlockEntity::updateMovement);
        sendPlayerMovement();
        // Update blocks after movement, so that players don't get stuck inside blocks
        pistons.values().forEach(PistonBlockEntity::updateBlocks);

        pistons.entrySet().removeIf((entry) -> entry.getValue().isDone());
    }

    public void resetPlayerMovement() {
        playerDisplacement = Vector3d.ZERO;
        playerCollided = false;
        playerSlimeCollision = false;
    }

    public void sendPlayerMovement() {
        SessionPlayerEntity playerEntity = session.getPlayerEntity();
        // Sending movement packets cancels motion from slime blocks
        if (!playerDisplacement.equals(Vector3d.ZERO) && !isInMotion()) {
            CollisionManager collisionManager = session.getCollisionManager();
            if (collisionManager.correctPlayerPosition()) {
                Vector3d position = Vector3d.from(collisionManager.getPlayerBoundingBox().getMiddleX(), collisionManager.getPlayerBoundingBox().getMiddleY() - (collisionManager.getPlayerBoundingBox().getSizeY() / 2), collisionManager.getPlayerBoundingBox().getMiddleZ());

                boolean isOnGround = playerDisplacement.getY() != 0 || playerEntity.isOnGround();

                playerEntity.moveAbsolute(session, position.toFloat(), playerEntity.getRotation(), isOnGround, true);

                ClientPlayerPositionPacket playerPositionPacket = new ClientPlayerPositionPacket(playerEntity.isOnGround(), position.getX(), position.getY(), position.getZ());
                session.sendDownstreamPacket(playerPositionPacket);

                session.setLastMovementTimestamp(System.currentTimeMillis());
            }
        }
        if (!playerMotion.equals(Vector3f.ZERO)) {
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

    public PistonBlockEntity getPistonAt(Vector3i position) {
        return pistons.get(position);
    }

    public void putPiston(PistonBlockEntity pistonBlockEntity) {
        pistons.put(pistonBlockEntity.getPosition(), pistonBlockEntity);
    }

    public void clear() {
        pistons.clear();
    }

    private boolean isInMotion() {
        return !playerMotion.equals(Vector3f.ZERO) || playerSlimeCollision;
    }

    private boolean isColliding() {
        return !playerDisplacement.equals(Vector3d.ZERO) || playerCollided;
    }

    /**
     * Check whether a movement packet should be canceled.
     * This cancels packets when being pushed by a piston and
     * when not being launched by a slime block.
     * @return True if the packet should be canceled
     */
    public boolean shouldCancelMovement() {
        return !isInMotion() && isColliding();
    }
}
