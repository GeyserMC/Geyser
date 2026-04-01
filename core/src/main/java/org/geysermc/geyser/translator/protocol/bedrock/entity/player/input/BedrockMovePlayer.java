/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player.input;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionResult;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;


final class BedrockMovePlayer {

    static void translate(GeyserSession session, PlayerAuthInputPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        if (!session.isSpawned()) return;

        
        
        entity.setBedrockInteractRotation(packet.getInteractRotation());

        
        if (session.getUnconfirmedTeleport() != null) {
            session.confirmTeleport(packet.getPosition().down(EntityDefinitions.PLAYER.offset()));
            return;
        }

        
        boolean actualPositionChanged = entity.bedrockPosition().distanceSquared(packet.getPosition()) > 4e-8;

        if (actualPositionChanged) {
            
            session.getBookEditCache().checkForSend();
        }

        if (entity.getBedPosition() != null) {
            
            
            
            return;
        }

        float yaw = packet.getRotation().getY();
        float pitch = packet.getRotation().getX();
        float headYaw = packet.getRotation().getY();

        
        
        
        float javaYaw = entity.getJavaYaw() + MathUtils.wrapDegrees(yaw - entity.getJavaYaw());

        boolean hasVehicle = entity.getVehicle() != null;

        
        boolean positionChangedAndShouldUpdate = !hasVehicle && (session.getInputCache().shouldSendPositionReminder() || actualPositionChanged);
        boolean rotationChanged = hasVehicle || (entity.getJavaYaw() != javaYaw || entity.getPitch() != pitch);

        
        if (isInvalidNumber(yaw) || isInvalidNumber(pitch) || isInvalidNumber(headYaw)) {
            return;
        }

        
        if (entity.isOnGround() && packet.getInputData().contains(PlayerAuthInputData.START_JUMPING)) {
            entity.setLastTickEndVelocity(Vector3f.from(entity.getLastTickEndVelocity().getX(), Math.max(entity.getLastTickEndVelocity().getY(), entity.getJumpVelocity()), entity.getLastTickEndVelocity().getZ()));
        }

        
        boolean onClimbableBlock = entity.isOnClimbableBlock();
        if (onClimbableBlock && packet.getInputData().contains(PlayerAuthInputData.JUMPING)) {
            entity.setLastTickEndVelocity(Vector3f.from(entity.getLastTickEndVelocity().getX(), 0.2F, entity.getLastTickEndVelocity().getZ()));
        }

        entity.setCollidingVertically(packet.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION));

        
        boolean isOnGround;
        if (hasVehicle || session.isNoClip()) {
            
            
            
            isOnGround = false;
        } else {
            isOnGround = entity.isCollidingVertically() && entity.getLastTickEndVelocity().getY() < 0;
        }

        
        
        
        
        if (packet.getPosition().getY() - EntityDefinitions.PLAYER.offset() < session.getBedrockDimension().minY() - 5) {
            
            boolean possibleOnGround = false;

            BoundingBox boundingBox = session.getCollisionManager().getPlayerBoundingBox().clone();

            
            boundingBox.extend(0, packet.getDelta().getY() - 2, 0);

            for (Entity other : session.getEntityCache().getEntities().values()) {
                if (!other.getFlag(EntityFlag.COLLIDABLE)) {
                    continue;
                }

                if (other == entity) {
                    continue;
                }

                final BoundingBox entityBoundingBox = new BoundingBox(other.position().up(other.getBoundingBoxHeight() / 2).toDouble(), other.getBoundingBoxWidth(), other.getBoundingBoxHeight(), other.getBoundingBoxWidth());

                if (entityBoundingBox.checkIntersection(boundingBox)) {
                    possibleOnGround = true;
                    break;
                }
            }

            session.setNoClip(!possibleOnGround);
        }

        
        
        
        boolean horizontalCollision = packet.getInputData().contains(PlayerAuthInputData.HORIZONTAL_COLLISION);

        
        
        
        if (!positionChangedAndShouldUpdate && rotationChanged) {
            ServerboundMovePlayerRotPacket playerRotationPacket = new ServerboundMovePlayerRotPacket(isOnGround, horizontalCollision, javaYaw, pitch);

            entity.setYaw(yaw);
            entity.setJavaYaw(javaYaw);
            entity.setPitch(pitch);
            entity.setHeadYaw(headYaw);

            session.sendDownstreamGamePacket(playerRotationPacket);

            
            if (hasVehicle) {
                entity.setPositionFromBedrockPos(packet.getPosition());
                session.getSkullCache().updateVisibleSkulls();
            }
        } else if (positionChangedAndShouldUpdate) {
            if (isValidMove(session, entity.bedrockPosition(), packet.getPosition())) {
                CollisionResult result = session.getCollisionManager().adjustBedrockPosition(packet.getPosition(), isOnGround, packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT));
                if (result != null) { 
                    Vector3d position = result.correctedMovement();

                    if (!session.getWorldBorder().isPassingIntoBorderBoundaries(position.toFloat(), true)) {
                        Packet movePacket;
                        if (rotationChanged) {
                            
                            movePacket = new ServerboundMovePlayerPosRotPacket(
                                isOnGround,
                                horizontalCollision,
                                position.getX(), position.getY(), position.getZ(),
                                javaYaw, pitch
                            );
                            entity.setYaw(yaw);
                            entity.setJavaYaw(javaYaw);
                            entity.setPitch(pitch);
                            entity.setHeadYaw(headYaw);
                        } else {
                            
                            movePacket = new ServerboundMovePlayerPosPacket(isOnGround, horizontalCollision, position.getX(), position.getY(), position.getZ());
                        }

                        entity.setPositionFromBedrockPos(packet.getPosition());

                        
                        session.sendDownstreamGamePacket(movePacket);

                        session.getInputCache().markPositionPacketSent();
                        session.getSkullCache().updateVisibleSkulls();
                    } else {
                        session.getCollisionManager().recalculatePosition();
                    }
                }
            } else {
                
                session.getGeyser().getLogger().debug("Recalculating position...");
                session.getCollisionManager().recalculatePosition();
            }
        } else if (horizontalCollision != session.getInputCache().lastHorizontalCollision() || isOnGround != entity.isOnGround()) {
            session.sendDownstreamGamePacket(new ServerboundMovePlayerStatusOnlyPacket(isOnGround, horizontalCollision));
        }

        session.getInputCache().setLastHorizontalCollision(horizontalCollision);
        entity.setOnGround(isOnGround);

        entity.setLastTickEndVelocity(packet.getDelta());
        entity.setMotion(packet.getDelta());

        
        if (entity.getLeftParrot() != null) {
            entity.getLeftParrot().moveAbsoluteRaw(entity.position(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
        }
        if (entity.getRightParrot() != null) {
            entity.getRightParrot().moveAbsoluteRaw(entity.position(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
        }
    }

    private static boolean isInvalidNumber(float val) {
        return Float.isNaN(val) || Float.isInfinite(val);
    }

    private static boolean isValidMove(GeyserSession session, Vector3f currentPosition, Vector3f newPosition) {
        if (isInvalidNumber(newPosition.getX()) || isInvalidNumber(newPosition.getY()) || isInvalidNumber(newPosition.getZ())) {
            return false;
        }
        if (currentPosition.distanceSquared(newPosition) > 300) {
            session.getGeyser().getLogger().debug(ChatColor.RED + session.bedrockUsername() + " moved too quickly." +
                    " current position: " + currentPosition + ", new position: " + newPosition);

            return false;
        }

        return true;
    }
}
