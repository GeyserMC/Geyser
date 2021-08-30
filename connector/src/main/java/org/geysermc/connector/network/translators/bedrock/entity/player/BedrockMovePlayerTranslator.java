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

package org.geysermc.connector.network.translators.bedrock.entity.player;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.entity.player.SessionPlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = MovePlayerPacket.class)
public class BedrockMovePlayerTranslator extends PacketTranslator<MovePlayerPacket> {
    /* The upper and lower bounds to check for the void floor that only exists in Bedrock */
    private static final int BEDROCK_OVERWORLD_VOID_FLOOR_UPPER_Y;
    private static final int BEDROCK_OVERWORLD_VOID_FLOOR_LOWER_Y;

    static {
        BEDROCK_OVERWORLD_VOID_FLOOR_UPPER_Y = GeyserConnector.getInstance().getConfig().isExtendedWorldHeight() ? -104 : -40;
        BEDROCK_OVERWORLD_VOID_FLOOR_LOWER_Y = BEDROCK_OVERWORLD_VOID_FLOOR_UPPER_Y + 2;
    }

    @Override
    public void translate(GeyserSession session, MovePlayerPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        if (!session.isSpawned()) return;

        if (!session.getUpstream().isInitialized()) {
            MoveEntityAbsolutePacket moveEntityBack = new MoveEntityAbsolutePacket();
            moveEntityBack.setRuntimeEntityId(entity.getGeyserId());
            moveEntityBack.setPosition(entity.getPosition());
            moveEntityBack.setRotation(entity.getBedrockRotation());
            moveEntityBack.setTeleported(true);
            moveEntityBack.setOnGround(true);
            session.sendUpstreamPacketImmediately(moveEntityBack);
            return;
        }

        session.setLastMovementTimestamp(System.currentTimeMillis());

        // Send book update before the player moves
        session.getBookEditCache().checkForSend();

        session.confirmTeleport(packet.getPosition().toDouble().sub(0, EntityType.PLAYER.getOffset(), 0));
        // head yaw, pitch, head yaw
        Vector3f rotation = Vector3f.from(packet.getRotation().getY(), packet.getRotation().getX(), packet.getRotation().getY());

        boolean positionChanged = !entity.getPosition().equals(packet.getPosition());
        boolean rotationChanged = !entity.getRotation().equals(rotation);

        // If only the pitch and yaw changed
        // This isn't needed, but it makes the packets closer to vanilla
        // It also means you can't "lag back" while only looking, in theory
        if (!positionChanged && rotationChanged) {
            ClientPlayerRotationPacket playerRotationPacket = new ClientPlayerRotationPacket(
                    packet.isOnGround(), packet.getRotation().getY(), packet.getRotation().getX());

            entity.setRotation(rotation);
            entity.setOnGround(packet.isOnGround());

            session.sendDownstreamPacket(playerRotationPacket);
        } else {
            Vector3d position = session.getCollisionManager().adjustBedrockPosition(packet.getPosition(), packet.isOnGround());
            if (position != null) { // A null return value cancels the packet
                if (isValidMove(session, packet.getMode(), entity.getPosition(), packet.getPosition())) {
                    Packet movePacket;
                    if (rotationChanged) {
                        // Send rotation updates as well
                        movePacket = new ClientPlayerPositionRotationPacket(packet.isOnGround(), position.getX(), position.getY(), position.getZ(),
                                packet.getRotation().getY(), packet.getRotation().getX());
                        entity.setRotation(rotation);
                    } else {
                        // Rotation did not change; don't send an update with rotation
                        movePacket = new ClientPlayerPositionPacket(packet.isOnGround(), position.getX(), position.getY(), position.getZ());
                    }

                    // Compare positions here for void floor fix below before the player's position variable is set to the packet position
                    boolean notMovingUp = entity.getPosition().getY() >= packet.getPosition().getY();

                    entity.setPositionManual(packet.getPosition());
                    entity.setOnGround(packet.isOnGround());

                    // Send final movement changes
                    session.sendDownstreamPacket(movePacket);

                    if (notMovingUp) {
                        int floorY = position.getFloorY();
                        // If the client believes the world has extended height, then it also believes the void floor
                        // still exists, just at a lower spot
                        boolean extendedWorld = session.getChunkCache().isExtendedHeight();
                        if (floorY <= (extendedWorld ? BEDROCK_OVERWORLD_VOID_FLOOR_LOWER_Y : -38)
                                && floorY >= (extendedWorld ? BEDROCK_OVERWORLD_VOID_FLOOR_UPPER_Y : -40)) {
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
                } else {
                    // Not a valid move
                    session.getConnector().getLogger().debug("Recalculating position...");
                    session.getCollisionManager().recalculatePosition();
                }
            }
        }

        // Move parrots to match if applicable
        if (entity.getLeftParrot() != null) {
            entity.getLeftParrot().moveAbsolute(session, entity.getPosition(), entity.getRotation(), true, false);
        }
        if (entity.getRightParrot() != null) {
            entity.getRightParrot().moveAbsolute(session, entity.getPosition(), entity.getRotation(), true, false);
        }
    }

    private boolean isValidMove(GeyserSession session, MovePlayerPacket.Mode mode, Vector3f currentPosition, Vector3f newPosition) {
        if (mode != MovePlayerPacket.Mode.NORMAL)
            return true;

        double xRange = newPosition.getX() - currentPosition.getX();
        double yRange = newPosition.getY() - currentPosition.getY();
        double zRange = newPosition.getZ() - currentPosition.getZ();

        if (xRange < 0)
            xRange = -xRange;
        if (yRange < 0)
            yRange = -yRange;
        if (zRange < 0)
            zRange = -zRange;

        if ((xRange + yRange + zRange) > 100) {
            session.getConnector().getLogger().debug(ChatColor.RED + session.getName() + " moved too quickly." +
                    " current position: " + currentPosition + ", new position: " + newPosition);

            return false;
        }

        return true;
    }
}

