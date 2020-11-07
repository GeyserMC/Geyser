/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import java.util.concurrent.TimeUnit;

@Translator(packet = MovePlayerPacket.class)
public class BedrockMovePlayerTranslator extends PacketTranslator<MovePlayerPacket> {

    @Override
    public void translate(MovePlayerPacket packet, GeyserSession session) {
        PlayerEntity entity = session.getPlayerEntity();
        if (entity == null || !session.isSpawned() || session.getPendingDimSwitches().get() > 0) return;

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

        if (session.getMovementSendIfIdle() != null) {
            session.getMovementSendIfIdle().cancel(true);
        }

        if (!session.confirmTeleport(Vector3d.from(packet.getPosition().getX(), packet.getPosition().getY() - EntityType.PLAYER.getOffset(),
                packet.getPosition().getZ()))) {
            return;
        }

        // If only the pitch and yaw changed
        // This isn't needed, but it makes the packets closer to vanilla
        // It also means you can't "lag back" while only looking, in theory
        if (entity.getPosition().equals(packet.getPosition())) {
            ClientPlayerRotationPacket playerRotationPacket = new ClientPlayerRotationPacket(
                    packet.isOnGround(), packet.getRotation().getY(), packet.getRotation().getX()
            );

            // head yaw, pitch, head yaw
            Vector3f rotation = Vector3f.from(packet.getRotation().getY(), packet.getRotation().getX(), packet.getRotation().getY());
            entity.setPosition(packet.getPosition().sub(0, EntityType.PLAYER.getOffset(), 0));
            entity.setRotation(rotation);
            entity.setOnGround(packet.isOnGround());

            session.sendDownstreamPacket(playerRotationPacket);
            return;
        }

        Vector3d position = adjustBedrockPosition(packet.getPosition(), packet.isOnGround(), session, entity);

        // A null return value cancels the packet
        if (position == null) {
            return;
        }
        
        

        if (!isValidMove(session, packet.getMode(), entity.getPosition(), packet.getPosition())) {
            session.getConnector().getLogger().debug("Recalculating position...");
            recalculatePosition(session, entity, entity.getPosition());
            return;
        }

        ClientPlayerPositionRotationPacket playerPositionRotationPacket = new ClientPlayerPositionRotationPacket(
                packet.isOnGround(), position.getX(), position.getY(), position.getZ(), packet.getRotation().getY(), packet.getRotation().getX()
        );

        // head yaw, pitch, head yaw
        Vector3f rotation = Vector3f.from(packet.getRotation().getY(), packet.getRotation().getX(), packet.getRotation().getY());
        entity.setPosition(packet.getPosition().sub(0, EntityType.PLAYER.getOffset(), 0));
        entity.setRotation(rotation);
        entity.setOnGround(packet.isOnGround());
        // Move parrots to match if applicable
        if (entity.getLeftParrot() != null) {
            entity.getLeftParrot().moveAbsolute(session, entity.getPosition(), entity.getRotation(), true, false);
        }
        if (entity.getRightParrot() != null) {
            entity.getRightParrot().moveAbsolute(session, entity.getPosition(), entity.getRotation(), true, false);
        }

        session.sendDownstreamPacket(playerPositionRotationPacket);

        // Schedule a position send loop if the player is idle
        session.setMovementSendIfIdle(session.getConnector().getGeneralThreadPool().schedule(() -> sendPositionIfIdle(session),
                3, TimeUnit.SECONDS));
    }

    public boolean isValidMove(GeyserSession session, MovePlayerPacket.Mode mode, Vector3f currentPosition, Vector3f newPosition) {
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

    /**
     * Adjust the Bedrock position before sending to the Java server to account for inaccuracies in movement between
     * the two versions.
     *
     * @param oldPosition the current Bedrock position of the client
     * @param onGround whether the Bedrock player is on the ground
     * @param session the current GeyserSession
     * @param entity the PlayerEntity of the connected client
     * @return the position to send to the Java server, or null to cancel sending the packet
     */
    private Vector3d adjustBedrockPosition(Vector3f oldPosition, boolean onGround, GeyserSession session, PlayerEntity entity) {
        // We need to parse the float as a string since casting a float to a double causes us to
        // lose precision and thus, causes players to get stuck when walking near walls
        double javaY = oldPosition.getY() - EntityType.PLAYER.getOffset();

        Vector3d position = Vector3d.from(Double.parseDouble(Float.toString(oldPosition.getX())), javaY,
                Double.parseDouble(Float.toString(oldPosition.getZ())));

        if (session.getConnector().getConfig().isCacheChunks()) {
            // With chunk caching, we can do some proper collision checks

            session.getCollisionManager().updatePlayerBoundingBox(position);

            // Correct player position
            if (!session.getCollisionManager().correctPlayerPosition()) {
                // Cancel the movement if it needs to be cancelled
                recalculatePosition(session, entity, entity.getPosition());
                return null;
            }

            position = Vector3d.from(session.getCollisionManager().getPlayerBoundingBox().getMiddleX(),
                    session.getCollisionManager().getPlayerBoundingBox().getMiddleY() -
                            (session.getCollisionManager().getPlayerBoundingBox().getSizeY() / 2),
                    session.getCollisionManager().getPlayerBoundingBox().getMiddleZ());
        } else {
            // When chunk caching is off, we have to rely on this
            // It rounds the Y position up to the nearest 0.5
            // This snaps players to snap to the top of stairs and slabs like on Java Edition
            // However, it causes issues such as the player floating on carpets
            if (onGround) javaY = Math.ceil(javaY * 2) / 2;
            position = position.up(javaY - position.getY());
        }

        return position;
    }

    // TODO: This makes the player look upwards for some reason, rotation values must be wrong
    public void recalculatePosition(GeyserSession session, Entity entity, Vector3f currentPosition) {
        // Gravity might need to be reset...
        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
        entityDataPacket.getMetadata().putAll(entity.getMetadata());
        session.sendUpstreamPacket(entityDataPacket);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
        movePlayerPacket.setPosition(entity.getPosition());
        movePlayerPacket.setRotation(entity.getBedrockRotation());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.RESPAWN);
        session.sendUpstreamPacket(movePlayerPacket);
    }

    private void sendPositionIfIdle(GeyserSession session) {
        if (session.isClosed()) return;
        PlayerEntity entity = session.getPlayerEntity();
        // Recalculate in case something else changed position
        Vector3d position = adjustBedrockPosition(entity.getPosition(), entity.isOnGround(), session, entity);
        // A null return value cancels the packet
        if (position == null) {
            return;
        }
        ClientPlayerPositionPacket packet = new ClientPlayerPositionPacket(session.getPlayerEntity().isOnGround(),
                position.getX(), position.getY(), position.getZ());
        session.sendDownstreamPacket(packet);
        session.setMovementSendIfIdle(session.getConnector().getGeneralThreadPool().schedule(() -> sendPositionIfIdle(session),
                3, TimeUnit.SECONDS));
    }
}

