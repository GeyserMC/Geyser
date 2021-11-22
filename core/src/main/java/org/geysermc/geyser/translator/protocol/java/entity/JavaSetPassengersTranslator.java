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

package org.geysermc.geyser.translator.protocol.java.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;

import java.util.Arrays;

@Translator(packet = ClientboundSetPassengersPacket.class)
public class JavaSetPassengersTranslator extends PacketTranslator<ClientboundSetPassengersPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetPassengersPacket packet) {
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
                session.confirmTeleport(passenger.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0).toDouble());
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
            if (entity.getDefinition() == EntityDefinitions.BOAT) {
                passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 1);
                passenger.getDirtyMetadata().put(EntityData.RIDER_MAX_ROTATION, 90f);
                passenger.getDirtyMetadata().put(EntityData.RIDER_MIN_ROTATION, 1f);
                passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_OFFSET, -90f);
            } else {
                passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 0);
                passenger.getDirtyMetadata().put(EntityData.RIDER_MAX_ROTATION, 0f);
                passenger.getDirtyMetadata().put(EntityData.RIDER_MIN_ROTATION, 0f);
            }

            passenger.updateBedrockMetadata();
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
                passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_LOCKED, (byte) 0);
                passenger.getDirtyMetadata().put(EntityData.RIDER_MAX_ROTATION, 0f);
                passenger.getDirtyMetadata().put(EntityData.RIDER_MIN_ROTATION, 0f);
                passenger.getDirtyMetadata().put(EntityData.RIDER_ROTATION_OFFSET, 0f);

                EntityUtils.updateMountOffset(passenger, entity, false, false, (packet.getPassengerIds().length > 1));
            } else {
                EntityUtils.updateMountOffset(passenger, entity, (packet.getPassengerIds()[0] == passengerId), true, (packet.getPassengerIds().length > 1));
            }

            // Force an update to the passenger metadata
            passenger.updateBedrockMetadata();
        }

        switch (entity.getDefinition().entityType()) {
            case HORSE, SKELETON_HORSE, DONKEY, MULE, RAVAGER -> {
                entity.getDirtyMetadata().put(EntityData.RIDER_MAX_ROTATION, 181.0f);
                entity.updateBedrockMetadata();
            }
        }
    }
}
