/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import static com.nukkitx.math.vector.Vector2f.from;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.api.ChatColor;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

import static java.lang.Math.abs;

public class BedrockMovePlayerTranslator extends PacketTranslator<MovePlayerPacket> {

    @Override
    public void translate(MovePlayerPacket packet, GeyserSession session) {
        PlayerEntity entity = session.getPlayerEntity();
        if (entity == null || !session.isSpawned()) return;

        if (!session.getUpstream().isInitialized()) {
            MoveEntityAbsolutePacket moveEntityBack = new MoveEntityAbsolutePacket();
            moveEntityBack.setRuntimeEntityId(entity.getGeyserId());
            moveEntityBack.setPosition(entity.getPosition());
            moveEntityBack.setRotation(entity.getBedrockRotation());
            moveEntityBack.setTeleported(true);
            moveEntityBack.setOnGround(true);
            session.getUpstream().sendPacketImmediately(moveEntityBack);
            return;
        }

        if (!isValidMove(session, packet.getMode(), entity.getPosition(), packet.getPosition())) {
            session.getConnector().getLogger().debug("Recalculating position...");
            recalculatePosition(session, entity, entity.getPosition());
            return;
        }

        double javaY = packet.getPosition().getY() - EntityType.PLAYER.getOffset();

        ClientPlayerPositionRotationPacket playerPositionRotationPacket = new ClientPlayerPositionRotationPacket(
                packet.isOnGround(), packet.getPosition().getX(), Math.ceil(javaY * 2) / 2,
                packet.getPosition().getZ(), packet.getRotation().getY(), packet.getRotation().getX()
        );

        // head yaw, pitch, head yaw
        Vector3f rotation = Vector3f.from(packet.getRotation().getY(), packet.getRotation().getX(), packet.getRotation().getY());

        entity.moveAbsolute(packet.getPosition().sub(0, EntityType.PLAYER.getOffset(), 0), rotation);

        /*
        boolean colliding = false;
        Position position = new Position((int) packet.getPosition().getX(),
                (int) Math.ceil(javaY * 2) / 2, (int) packet.getPosition().getZ());

        BlockEntry block = session.getChunkCache().getBlockAt(position);
        if (!block.getJavaIdentifier().contains("air"))
            colliding = true;

        if (!colliding)
         */
        session.getDownstream().getSession().send(playerPositionRotationPacket);
    }

    public boolean isValidMove(GeyserSession session, MovePlayerPacket.Mode mode, Vector3f currentPosition, Vector3f newPosition) {
        if (mode != MovePlayerPacket.Mode.NORMAL)
            return true;

        double yRange = abs(newPosition.getY() - currentPosition.getY());

        if (yRange > 30 || dist(newPosition, currentPosition) > 10) {
            session.getConnector().getLogger().debug(ChatColor.RED + session.getName() + " moved too quickly." +
                    " current position: " + currentPosition + ", new position: " + newPosition);

            return false;
        }

        return true;
    }

    private double dist(Vector3f a, Vector3f b) {
        return from(a.getX(), a.getZ()).distance(from(b.getX(), b.getZ()));
    }

    public void recalculatePosition(GeyserSession session, Entity entity, Vector3f currentPosition) {
        // Gravity might need to be reset...
        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
        entityDataPacket.getMetadata().putAll(entity.getMetadata());
        session.getUpstream().sendPacket(entityDataPacket);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
        movePlayerPacket.setPosition(entity.getPosition());
        movePlayerPacket.setRotation(entity.getBedrockRotation());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.RESET);
        movePlayerPacket.setOnGround(true);
        entity.setMovePending(false);
        session.getUpstream().sendPacket(movePlayerPacket);
    }
}
