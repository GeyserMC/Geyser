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
import org.geysermc.geyser.entity.type.BoatEntity;
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

/**
 * Holds processing input coming in from the {@link PlayerAuthInputPacket} packet.
 */
final class BedrockMovePlayer {

    static void translate(GeyserSession session, PlayerAuthInputPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        if (!session.isSpawned()) return;

        // We need to save player interact rotation value, as this rotation is used for Touch device and indicate where the player is touching.
        // This is needed so that we can interact with where player actually touch on the screen on Bedrock and not just from the center of the screen.
        entity.setBedrockInteractRotation(packet.getInteractRotation());

        // Ignore movement packets until Bedrock's position matches the teleported position
        if (session.getUnconfirmedTeleport() != null) {
            session.confirmTeleport(packet.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0));
            return;
        }

        // This is vanilla behaviour, LocalPlayer#sendPosition 1.21.8.
        boolean actualPositionChanged = entity.getPosition().distanceSquared(packet.getPosition()) > 4e-8;

        if (actualPositionChanged) {
            // Send book update before the player moves
            session.getBookEditCache().checkForSend();
        }

        if (entity.getBedPosition() != null) {
            // https://github.com/GeyserMC/Geyser/issues/5001
            // Bedrock 1.21.22 started sending a MovePlayerPacket as soon as it got into a bed.
            // This trips up Fabric.
            return;
        }

        float yaw = packet.getRotation().getY();
        float pitch = packet.getRotation().getX();
        float headYaw = packet.getRotation().getY();

        // Even though on Java Edition the yaw rotation should never get wrapped and can get larger than 180, on Bedrock Edition
        // the client always seems to wrap and limit it to -180 and 180, which is not vanilla behaviour, and doesn't cause problems
        // on the surface - however, some anticheat checks for this, so we account for it
        float javaYaw = entity.getJavaYaw() + MathUtils.wrapDegrees(yaw - entity.getJavaYaw());

        boolean hasVehicle = entity.getVehicle() != null;

        // shouldSendPositionReminder also increments a tick counter, so make sure it's always called unless the player is on a vehicle.
        boolean positionChangedAndShouldUpdate = !hasVehicle && (session.getInputCache().shouldSendPositionReminder() || actualPositionChanged);
        boolean rotationChanged = hasVehicle || (entity.getJavaYaw() != javaYaw || entity.getPitch() != pitch);

        // Drop invalid rotation packets
        if (isInvalidNumber(yaw) || isInvalidNumber(pitch) || isInvalidNumber(headYaw)) {
            return;
        }

        // Simulate jumping since it happened this tick, not from the last tick end.
        if (entity.isOnGround() && packet.getInputData().contains(PlayerAuthInputData.START_JUMPING)) {
            entity.setLastTickEndVelocity(Vector3f.from(entity.getLastTickEndVelocity().getX(), Math.max(entity.getLastTickEndVelocity().getY(), entity.getJumpVelocity()), entity.getLastTickEndVelocity().getZ()));
        }

        // Due to how ladder works on Bedrock, we won't get climbing velocity from tick end unless if you're colliding horizontally. So we account for it ourselves.
        boolean onClimbableBlock = entity.isOnClimbableBlock();
        if (onClimbableBlock && packet.getInputData().contains(PlayerAuthInputData.JUMPING)) {
            entity.setLastTickEndVelocity(Vector3f.from(entity.getLastTickEndVelocity().getX(), 0.2F, entity.getLastTickEndVelocity().getZ()));
        }

        // Client is telling us it wants to move down, but something is blocking it from doing so.
        boolean isOnGround;
        if (hasVehicle || session.isNoClip()) {
            // VERTICAL_COLLISION is not accurate while in a vehicle (as of 1.21.62)
            // If the player is riding a vehicle or is in spectator mode, onGround is always set to false for the player
            // Also do this if player have no clip ability since they shouldn't be able to collide with anything.
            isOnGround = false;
        } else {
            isOnGround = packet.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION) && entity.getLastTickEndVelocity().getY() < 0;
        }

        // Resolve https://github.com/GeyserMC/Geyser/issues/3521, no void floor on java so player not supposed to collide with anything.
        // Therefore, we're fixing this by allowing player to no clip to clip through the floor, not only this fixed the issue but
        // player y velocity should match java perfectly, much better than teleport player right down :)
        // Shouldn't mess with anything because beyond this point there is nothing to collide and not even entities since they're prob dead.
        if (packet.getPosition().getY() - EntityDefinitions.PLAYER.offset() < session.getBedrockDimension().minY() - 5) {
            // Ensuring that we still can collide with collidable entity that are also in the void (eg: boat, shulker)
            boolean possibleOnGround = false;

            BoundingBox boundingBox = session.getCollisionManager().getPlayerBoundingBox().clone();

            // Extend down by y velocity subtract by 2 so that we are a "little" ahead and can send no clip in time before player hit the entity.
            boundingBox.extend(0, packet.getDelta().getY() - 2, 0);

            for (Entity other : session.getEntityCache().getEntities().values()) {
                if (!other.getFlag(EntityFlag.COLLIDABLE)) {
                    continue;
                }

                if (other == entity) {
                    continue;
                }

                final BoundingBox entityBoundingBox = new BoundingBox(0, 0, 0, other.getBoundingBoxWidth(), other.getBoundingBoxHeight(), other.getBoundingBoxWidth());

                // Also offset the position down for boat as their position is offset.
                entityBoundingBox.translate(other.getPosition().down(other instanceof BoatEntity ? other.getDefinition().offset() : 0).toDouble());

                if (entityBoundingBox.checkIntersection(boundingBox)) {
                    possibleOnGround = true;
                    break;
                }
            }

            session.setNoClip(!possibleOnGround);
        }

        entity.setLastTickEndVelocity(packet.getDelta());
        entity.setMotion(packet.getDelta());

        // This takes into account no movement sent from the client, but the player is trying to move anyway.
        // (Press into a wall in a corner - you're trying to move but nothing actually happens)
        // This isn't sent when a player is riding a vehicle (as of 1.21.62)
        boolean horizontalCollision = packet.getInputData().contains(PlayerAuthInputData.HORIZONTAL_COLLISION);

        // If only the pitch and yaw changed
        // This isn't needed, but it makes the packets closer to vanilla
        // It also means you can't "lag back" while only looking, in theory
        if (!positionChangedAndShouldUpdate && rotationChanged) {
            ServerboundMovePlayerRotPacket playerRotationPacket = new ServerboundMovePlayerRotPacket(isOnGround, horizontalCollision, javaYaw, pitch);

            entity.setYaw(yaw);
            entity.setJavaYaw(javaYaw);
            entity.setPitch(pitch);
            entity.setHeadYaw(headYaw);

            session.sendDownstreamGamePacket(playerRotationPacket);

            // Player position MUST be updated on our end, otherwise e.g. chunk loading breaks
            if (hasVehicle) {
                entity.setPositionManual(packet.getPosition());
                session.getSkullCache().updateVisibleSkulls();
            }
        } else if (positionChangedAndShouldUpdate) {
            if (isValidMove(session, entity.getPosition(), packet.getPosition())) {
                CollisionResult result = session.getCollisionManager().adjustBedrockPosition(packet.getPosition(), isOnGround, packet.getInputData().contains(PlayerAuthInputData.HANDLE_TELEPORT));
                if (result != null) { // A null return value cancels the packet
                    Vector3d position = result.correctedMovement();

                    if (!session.getWorldBorder().isPassingIntoBorderBoundaries(position.toFloat(), true)) {
                        Packet movePacket;
                        if (rotationChanged) {
                            // Send rotation updates as well
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
                            // Rotation did not change; don't send an update with rotation
                            movePacket = new ServerboundMovePlayerPosPacket(isOnGround, horizontalCollision, position.getX(), position.getY(), position.getZ());
                        }

                        entity.setPositionManual(packet.getPosition());

                        // Send final movement changes
                        session.sendDownstreamGamePacket(movePacket);

                        session.getInputCache().markPositionPacketSent();
                        session.getSkullCache().updateVisibleSkulls();
                    } else {
                        session.getCollisionManager().recalculatePosition();
                    }
                }
            } else {
                // Not a valid move
                session.getGeyser().getLogger().debug("Recalculating position...");
                session.getCollisionManager().recalculatePosition();
            }
        } else if (horizontalCollision != session.getInputCache().lastHorizontalCollision() || isOnGround != entity.isOnGround()) {
            session.sendDownstreamGamePacket(new ServerboundMovePlayerStatusOnlyPacket(isOnGround, horizontalCollision));
        }

        session.getInputCache().setLastHorizontalCollision(horizontalCollision);
        entity.setOnGround(isOnGround);

        // Move parrots to match if applicable
        if (entity.getLeftParrot() != null) {
            entity.getLeftParrot().moveAbsoluteRaw(entity.getPosition(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
        }
        if (entity.getRightParrot() != null) {
            entity.getRightParrot().moveAbsoluteRaw(entity.getPosition(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
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
