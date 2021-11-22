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

package org.geysermc.geyser.translator.protocol.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.RespawnPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.TeleportCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.text.GeyserLocale;

@Translator(packet = ClientboundPlayerPositionPacket.class)
public class JavaPlayerPositionTranslator extends PacketTranslator<ClientboundPlayerPositionPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundPlayerPositionPacket packet) {
        if (!session.isLoggedIn())
            return;

        PlayerEntity entity = session.getPlayerEntity();

        if (!session.isSpawned()) {
            Vector3f pos = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
            entity.setPosition(pos);
            entity.setYaw(packet.getYaw());
            entity.setPitch(packet.getPitch());
            entity.setHeadYaw(packet.getYaw());

            RespawnPacket respawnPacket = new RespawnPacket();
            respawnPacket.setRuntimeEntityId(0); // Bedrock server behavior
            respawnPacket.setPosition(entity.getPosition());
            respawnPacket.setState(RespawnPacket.State.SERVER_READY);
            session.sendUpstreamPacket(respawnPacket);

            entity.updateBedrockMetadata();

            MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
            movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
            movePlayerPacket.setPosition(entity.getPosition());
            movePlayerPacket.setRotation(Vector3f.from(packet.getPitch(), packet.getYaw(), 0));
            movePlayerPacket.setMode(MovePlayerPacket.Mode.RESPAWN);

            session.sendUpstreamPacket(movePlayerPacket);
            session.setSpawned(true);

            ServerboundAcceptTeleportationPacket teleportConfirmPacket = new ServerboundAcceptTeleportationPacket(packet.getTeleportId());
            session.sendDownstreamPacket(teleportConfirmPacket);

            ChunkUtils.updateChunkPosition(session, pos.toInt());

            session.getGeyser().getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.entity.player.spawn", packet.getX(), packet.getY(), packet.getZ()));
            return;
        }

        session.setSpawned(true);

        if (packet.isDismountVehicle() && session.getRidingVehicleEntity() != null) {
            Entity vehicle = session.getRidingVehicleEntity();
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            linkPacket.setEntityLink(new EntityLinkData(vehicle.getGeyserId(), entity.getGeyserId(), EntityLinkData.Type.REMOVE, false, false));
            session.sendUpstreamPacket(linkPacket);
            vehicle.getPassengers().remove(entity.getEntityId());
            entity.getDirtyMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 0);
            entity.getDirtyMetadata().put(EntityData.RIDER_MAX_ROTATION, 0f);
            entity.getDirtyMetadata().put(EntityData.RIDER_MIN_ROTATION, 0f);
            entity.getDirtyMetadata().put(EntityData.RIDER_ROTATION_OFFSET, 0f);
            session.setRidingVehicleEntity(null);
            entity.updateBedrockMetadata();

            EntityUtils.updateMountOffset(entity, vehicle, false, false, entity.getPassengers().size() > 1);
        }

        // If coordinates are relative, then add to the existing coordinate
        double newX = packet.getX() +
                (packet.getRelative().contains(PositionElement.X) ? entity.getPosition().getX() : 0);
        double newY = packet.getY() +
                (packet.getRelative().contains(PositionElement.Y) ? entity.getPosition().getY() - EntityDefinitions.PLAYER.offset() : 0);
        double newZ = packet.getZ() +
                (packet.getRelative().contains(PositionElement.Z) ? entity.getPosition().getZ() : 0);

        float newPitch = packet.getPitch() +
                (packet.getRelative().contains(PositionElement.PITCH) ? entity.getBedrockRotation().getX() : 0);
        float newYaw = packet.getYaw() +
                (packet.getRelative().contains(PositionElement.YAW) ? entity.getBedrockRotation().getY() : 0);

        session.getGeyser().getLogger().debug("Teleport from " + entity.getPosition().getX() + " " + (entity.getPosition().getY() - EntityDefinitions.PLAYER.offset()) + " " + entity.getPosition().getZ());

        session.addTeleport(new TeleportCache(newX, newY, newZ, newPitch, newYaw, packet.getTeleportId()));

        Vector3f lastPlayerPosition = entity.getPosition().down(EntityDefinitions.PLAYER.offset());
        float lastPlayerPitch = entity.getBedrockRotation().getX();
        Vector3f teleportDestination = Vector3f.from(newX, newY, newZ);
        entity.moveAbsolute(teleportDestination, newYaw, newPitch, true, true);

        session.getGeyser().getLogger().debug("to " + entity.getPosition().getX() + " " + (entity.getPosition().getY() - EntityDefinitions.PLAYER.offset()) + " " + entity.getPosition().getZ());

        // Bedrock ignores teleports that are extremely close to the player's original position and orientation,
        // so check if we can immediately confirm the teleport
        if (lastPlayerPosition.distanceSquared(teleportDestination) < 0.001 && Math.abs(newPitch - lastPlayerPitch) < 5) {
            session.confirmTeleport(lastPlayerPosition.toDouble());
        }
    }
}
