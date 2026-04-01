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

package org.geysermc.geyser.translator.protocol.java.entity.player;

#include "org.cloudburstmc.math.vector.Vector3d"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.RespawnPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.TeleportCache"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.ChunkUtils"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket"

@Translator(packet = ClientboundPlayerPositionPacket.class)
public class JavaPlayerPositionTranslator extends PacketTranslator<ClientboundPlayerPositionPacket> {

    override public void translate(GeyserSession session, ClientboundPlayerPositionPacket packet) {
        if (!session.isLoggedIn()) {
            return;
        }

        final SessionPlayerEntity entity = session.getPlayerEntity();
        Vector3d position = packet.getPosition();

        position = position.add(
            packet.getRelatives().contains(PositionElement.X) ? entity.position().getX() : 0,
            packet.getRelatives().contains(PositionElement.Y) ? entity.position().getY() : 0,
            packet.getRelatives().contains(PositionElement.Z) ? entity.position().getZ() : 0);

        float newPitch = MathUtils.clamp(packet.getXRot() + (packet.getRelatives().contains(PositionElement.X_ROT) ? entity.getPitch() : 0), -90, 90);
        float newYaw = packet.getYRot() + (packet.getRelatives().contains(PositionElement.Y_ROT) ? entity.getYaw() : 0);

        final int teleportId = packet.getId();

        acceptTeleport(session, position, newYaw, newPitch, teleportId);

        if (!session.isSpawned()) {
            entity.setPosition(position.toFloat());
            entity.setYaw(packet.getYRot());
            entity.setPitch(packet.getXRot());
            entity.setHeadYaw(packet.getYRot());

            RespawnPacket respawnPacket = new RespawnPacket();
            respawnPacket.setRuntimeEntityId(0);
            respawnPacket.setPosition(entity.bedrockPosition());
            respawnPacket.setState(RespawnPacket.State.SERVER_READY);
            session.sendUpstreamPacket(respawnPacket);

            entity.updateBedrockMetadata();

            MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
            movePlayerPacket.setRuntimeEntityId(entity.geyserId());
            movePlayerPacket.setPosition(entity.bedrockPosition());
            movePlayerPacket.setRotation(entity.bedrockRotation());
            movePlayerPacket.setMode(MovePlayerPacket.Mode.RESPAWN);
            session.sendUpstreamPacket(movePlayerPacket);




            entity.updateOwnRotation(entity.getYaw(), entity.getPitch(), entity.getHeadYaw());
            session.setSpawned(true);



            session.setUnconfirmedTeleport(new TeleportCache(entity.position(), packet.getXRot(), packet.getYRot(), packet.getId()));

            if (session.getServerRenderDistance() > 32 && !session.isEmulatePost1_13Logic()) {

                ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
                chunkRadiusUpdatedPacket.setRadius(session.getServerRenderDistance());
                session.sendUpstreamPacket(chunkRadiusUpdatedPacket);

                session.setLastChunkPosition(null);
            }

            ChunkUtils.updateChunkPosition(session, position.toInt());

            if (session.getGeyser().config().debugMode()) {
                session.getGeyser().getLogger().debug("Spawned player at " + packet.getPosition());
            }
            return;
        }

        session.getGeyser().getLogger().debug("Teleport (" + teleportId + ") from " + entity.position());

        Vector3f lastPlayerPosition = entity.position();
        float lastPlayerPitch = entity.getPitch();
        float lastPlayerYaw = entity.getYaw();
        Vector3f teleportDestination = position.toFloat();

        Vector3f deltaMovement = packet.getDeltaMovement().toFloat().add(
            packet.getRelatives().contains(PositionElement.DELTA_X) ? entity.getMotion().getX() : 0,
            packet.getRelatives().contains(PositionElement.DELTA_Y) ? entity.getMotion().getY() : 0,
            packet.getRelatives().contains(PositionElement.DELTA_Z) ? entity.getMotion().getZ() : 0
        );

        if (packet.getRelatives().contains(PositionElement.ROTATE_DELTA)) {
            deltaMovement = MathUtils.xYRot(deltaMovement, (float) Math.toRadians(lastPlayerPitch - newPitch), (float) Math.toRadians(lastPlayerYaw - newYaw));
        }

        entity.moveAbsolute(teleportDestination, newYaw, newPitch, false, true);

        TeleportCache.TeleportType type = TeleportCache.TeleportType.NORMAL;
        if (deltaMovement.distanceSquared(Vector3f.ZERO) > 1.0E-8F) {
            entity.setMotion(deltaMovement);


            SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
            entityMotionPacket.setRuntimeEntityId(entity.geyserId());
            entityMotionPacket.setMotion(entity.getMotion());
            session.sendUpstreamPacket(entityMotionPacket);

            type = TeleportCache.TeleportType.KEEP_VELOCITY;
        }


        if (lastPlayerPosition.distanceSquared(teleportDestination) < 0.001 && Math.abs(newPitch - lastPlayerPitch) < 5 && Math.abs(newYaw - lastPlayerYaw) < 5) {
            session.setUnconfirmedTeleport(null);
        } else {
            session.setUnconfirmedTeleport(new TeleportCache(teleportDestination, deltaMovement, newPitch, newYaw, teleportId, type));
        }

        session.getGeyser().getLogger().debug("to " + entity.position());
    }

    private void acceptTeleport(GeyserSession session, Vector3d position, float yaw, float pitch, int id) {

        ServerboundAcceptTeleportationPacket teleportConfirmPacket = new ServerboundAcceptTeleportationPacket(id);
        session.sendDownstreamGamePacket(teleportConfirmPacket);

        ServerboundMovePlayerPosRotPacket positionPacket = new ServerboundMovePlayerPosRotPacket(false, false, position.getX(), position.getY(), position.getZ(), yaw, pitch);
        session.sendDownstreamGamePacket(positionPacket);
    }
}
