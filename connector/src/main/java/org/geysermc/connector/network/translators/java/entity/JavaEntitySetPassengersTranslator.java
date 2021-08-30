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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntitySetPassengersPacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.EntityUtils;

import java.util.Arrays;

@Translator(packet = ServerEntitySetPassengersPacket.class)
public class JavaEntitySetPassengersTranslator extends PacketTranslator<ServerEntitySetPassengersPacket> {

    @Override
    public void translate(ServerEntitySetPassengersPacket packet, GeyserSession session) {
        Entity entity;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        }

        if (entity == null) return;

        LongOpenHashSet passengers = entity.getPassengers().clone();
        boolean rider = true;
        for (long passengerId : packet.getPassengerIds()) {
            Entity passenger = session.getEntityCache().getEntityByJavaId(passengerId);
            if (passengerId == session.getPlayerEntity().getEntityId()) {
                passenger = session.getPlayerEntity();
                session.setRidingVehicleEntity(entity);
                // We need to confirm teleports before entering a vehicle, or else we will likely exit right out
                session.confirmTeleport(passenger.getPosition().sub(0, EntityType.PLAYER.getOffset(), 0).toDouble());
            }
            // Passenger hasn't loaded in (likely since we're waiting for a skin response)
            // and entity link needs to be set later
            if (passenger == null && passengerId != 0) {
                session.getEntityCache().addCachedPlayerEntityLink(passengerId, packet.getEntityId());
            }
            if (passenger == null) {
                continue;
            }

            EntityLinkData.Type type = rider ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            linkPacket.setEntityLink(new EntityLinkData(entity.getGeyserId(), passenger.getGeyserId(), type, false));
            session.sendUpstreamPacket(linkPacket);
            passengers.add(passengerId);

            // Head rotation on boats
            if (entity.getEntityType() == EntityType.BOAT) {
                passenger.getMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 1);
                passenger.getMetadata().put(EntityData.RIDER_MAX_ROTATION, 90f);
                passenger.getMetadata().put(EntityData.RIDER_MIN_ROTATION, 1f);
                passenger.getMetadata().put(EntityData.RIDER_ROTATION_OFFSET, -90f);
            } else {
                passenger.getMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 0);
                passenger.getMetadata().put(EntityData.RIDER_MAX_ROTATION, 0f);
                passenger.getMetadata().put(EntityData.RIDER_MIN_ROTATION, 0f);
            }

            passenger.updateBedrockMetadata(session);
            rider = false;
        }

        entity.setPassengers(passengers);

        for (long passengerId : entity.getPassengers()) {
            Entity passenger = session.getEntityCache().getEntityByJavaId(passengerId);
            if (passengerId == session.getPlayerEntity().getEntityId()) {
                passenger = session.getPlayerEntity();
            }
            if (passenger == null) {
                continue;
            }
            if (Arrays.stream(packet.getPassengerIds()).noneMatch(id -> id == passengerId)) {
                SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                linkPacket.setEntityLink(new EntityLinkData(entity.getGeyserId(), passenger.getGeyserId(), EntityLinkData.Type.REMOVE, false));
                session.sendUpstreamPacket(linkPacket);
                passengers.remove(passenger.getEntityId());
                passenger.getMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 0);
                passenger.getMetadata().put(EntityData.RIDER_MAX_ROTATION, 0f);
                passenger.getMetadata().put(EntityData.RIDER_MIN_ROTATION, 0f);
                passenger.getMetadata().put(EntityData.RIDER_ROTATION_OFFSET, 0f);

                EntityUtils.updateMountOffset(passenger, entity, session, false, false, (packet.getPassengerIds().length > 1));
            } else {
                EntityUtils.updateMountOffset(passenger, entity, session, (packet.getPassengerIds()[0] == passengerId), true, (packet.getPassengerIds().length > 1));
            }

            // Force an update to the passenger metadata
            passenger.updateBedrockMetadata(session);
        }

        switch (entity.getEntityType()) {
            case HORSE:
            case SKELETON_HORSE:
            case DONKEY:
            case MULE:
            case RAVAGER:
                entity.getMetadata().put(EntityData.RIDER_MAX_ROTATION, 181.0f);
                entity.updateBedrockMetadata(session);
                break;
        }
    }
}
