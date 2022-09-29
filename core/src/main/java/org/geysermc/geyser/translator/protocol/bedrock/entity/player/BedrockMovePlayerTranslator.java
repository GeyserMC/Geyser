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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = MovePlayerPacket.class)
public class BedrockMovePlayerTranslator extends PacketTranslator<MovePlayerPacket> {

    @Override
    public void translate(GeyserSession session, MovePlayerPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        if (!session.isSpawned()) return;

        session.setLastMovementTimestamp(System.currentTimeMillis());

        // Send book update before the player moves
        session.getBookEditCache().checkForSend();

        // Ignore movement packets until Bedrock's position matches the teleported position
        if (session.getUnconfirmedTeleport() != null) {
            session.confirmTeleport(packet.getPosition().toDouble().sub(0, EntityDefinitions.PLAYER.offset(), 0));
            return;
        }
        float yaw = packet.getRotation().getY();
        float pitch = packet.getRotation().getX();
        float headYaw = packet.getRotation().getY();

        boolean positionChanged = !entity.getPosition().equals(packet.getPosition());
        boolean rotationChanged = entity.getYaw() != yaw || entity.getPitch() != pitch || entity.getHeadYaw() != headYaw;

        if (session.getLookBackScheduledFuture() != null) {
            // Resend the rotation if it was changed by Geyser
            rotationChanged |= !session.getLookBackScheduledFuture().isDone();
            session.getLookBackScheduledFuture().cancel(false);
            session.setLookBackScheduledFuture(null);
        }

        // If only the pitch and yaw changed
        // This isn't needed, but it makes the packets closer to vanilla
        // It also means you can't "lag back" while only looking, in theory
        if (!positionChanged && rotationChanged) {
            ServerboundMovePlayerRotPacket playerRotationPacket = new ServerboundMovePlayerRotPacket(packet.isOnGround(), yaw, pitch);

            entity.setYaw(yaw);
            entity.setPitch(pitch);
            entity.setHeadYaw(headYaw);
            entity.setOnGround(packet.isOnGround());

            session.sendDownstreamPacket(playerRotationPacket);
        } else {
            if (session.getWorldBorder().isPassingIntoBorderBoundaries(packet.getPosition(), true)) {
                return;
            }

            if (isValidMove(session, entity.getPosition(), packet.getPosition())) {
                Vector3d position = session.getCollisionManager().adjustBedrockPosition(packet.getPosition(), packet.isOnGround(), packet.getMode() == MovePlayerPacket.Mode.TELEPORT);
                if (position != null) { // A null return value cancels the packet
                    Packet movePacket;
                    if (rotationChanged) {
                        // Send rotation updates as well
                        movePacket = new ServerboundMovePlayerPosRotPacket(
                                packet.isOnGround(),
                                position.getX(), position.getY(), position.getZ(),
                                yaw, pitch
                        );
                        entity.setYaw(yaw);
                        entity.setPitch(pitch);
                        entity.setHeadYaw(headYaw);
                    } else {
                        // Rotation did not change; don't send an update with rotation
                        movePacket = new ServerboundMovePlayerPosPacket(packet.isOnGround(), position.getX(), position.getY(), position.getZ());
                    }

                    // Compare positions here for void floor fix below before the player's position variable is set to the packet position
                    boolean notMovingUp = entity.getPosition().getY() >= packet.getPosition().getY();

                    entity.setPositionManual(packet.getPosition());
                    entity.setOnGround(packet.isOnGround());

                    // Send final movement changes
                    session.sendDownstreamPacket(movePacket);

                    if (notMovingUp) {
                        int floorY = position.getFloorY();
                        // The void floor is offset about 40 blocks below the bottom of the world
                        BedrockDimension bedrockDimension = session.getChunkCache().getBedrockDimension();
                        int voidFloorLocation = bedrockDimension.minY() - 40;
                        if (floorY <= (voidFloorLocation + 2) && floorY >= voidFloorLocation) {
                            // Work around there being a floor at the bottom of the world and teleport the player below it
                            // Moving from below to above the void floor works fine
                            entity.setPosition(entity.getPosition().sub(0, 4f, 0));
                            MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
                            movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
                            movePlayerPacket.setPosition(entity.getPosition());
                            movePlayerPacket.setRotation(entity.getBedrockRotation());
                            movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
                            movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
                            session.sendUpstreamPacket(movePlayerPacket);
                        }
                    }

                    session.getSkullCache().updateVisibleSkulls();
                }
            } else {
                // Not a valid move
                session.getGeyser().getLogger().debug("Recalculating position...");
                session.getCollisionManager().recalculatePosition();
            }
        }

        // Move parrots to match if applicable
        if (entity.getLeftParrot() != null) {
            entity.getLeftParrot().moveAbsolute(entity.getPosition(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
        }
        if (entity.getRightParrot() != null) {
            entity.getRightParrot().moveAbsolute(entity.getPosition(), entity.getYaw(), entity.getPitch(), entity.getHeadYaw(), true, false);
        }
    }

    private boolean isInvalidNumber(float val) {
        return Float.isNaN(val) || Float.isInfinite(val);
    }

    private boolean isValidMove(GeyserSession session, Vector3f currentPosition, Vector3f newPosition) {
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

