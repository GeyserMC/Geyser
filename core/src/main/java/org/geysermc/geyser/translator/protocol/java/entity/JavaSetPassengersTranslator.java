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

package org.geysermc.geyser.translator.protocol.java.entity;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;

import java.util.ArrayList;
import java.util.List;

@Translator(packet = ClientboundSetPassengersPacket.class)
public class JavaSetPassengersTranslator extends PacketTranslator<ClientboundSetPassengersPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetPassengersPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null) return;

        // Handle new/existing passengers
        List<Entity> newPassengers = new ArrayList<>();
        for (int passengerId : packet.getPassengerIds()) {
            Entity passenger = session.getEntityCache().getEntityByJavaId(passengerId);
            if (passenger == session.getPlayerEntity()) {
                session.getPlayerEntity().setVehicle(entity);
                // We need to confirm teleports before entering a vehicle, or else we will likely exit right out
                session.confirmTeleport(passenger.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0).toDouble());

                if (entity instanceof ClientVehicle clientVehicle) {
                    clientVehicle.getVehicleComponent().onMount();
                }
            }
            if (passenger == null) {
                // Can occur if the passenger is outside the client's tracking range
                // In this case, another SetPassengers packet will be sent when the passenger is spawned.
                continue;
            }

            boolean rider = packet.getPassengerIds()[0] == passengerId;
            EntityLinkData.Type type = rider ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            linkPacket.setEntityLink(new EntityLinkData(entity.getGeyserId(), passenger.getGeyserId(), type, false, false, 0f));
            session.sendUpstreamPacket(linkPacket);
            newPassengers.add(passenger);

            passenger.setVehicle(entity);
            EntityUtils.updateRiderRotationLock(passenger, entity, true);
            EntityUtils.updateMountOffset(passenger, entity, rider, true, (packet.getPassengerIds().length > 1));
            // Force an update to the passenger metadata
            passenger.updateBedrockMetadata();
        }

        // Handle passengers that were removed
        for (Entity passenger : entity.getPassengers()) {
            if (passenger == null) {
                continue;
            }
            if (!newPassengers.contains(passenger)) {
                SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                linkPacket.setEntityLink(new EntityLinkData(entity.getGeyserId(), passenger.getGeyserId(), EntityLinkData.Type.REMOVE, false, false, 0f));
                session.sendUpstreamPacket(linkPacket);

                passenger.setVehicle(null);
                EntityUtils.updateRiderRotationLock(passenger, entity, false);
                EntityUtils.updateMountOffset(passenger, entity, false, false, (packet.getPassengerIds().length > 1));
                // Force an update to the passenger metadata
                passenger.updateBedrockMetadata();

                if (passenger == session.getPlayerEntity()) {
                    //TODO test
                    if (session.getMountVehicleScheduledFuture() != null) {
                        // Cancel this task as it is now unnecessary.
                        // Note that this isn't present in JavaSetPassengersTranslator as that code is not called for players
                        // as of Java 1.19.3, but the scheduled future checks for the vehicle being null anyway.
                        session.getMountVehicleScheduledFuture().cancel(false);
                    }

                    if (entity instanceof ClientVehicle clientVehicle) {
                        clientVehicle.getVehicleComponent().onDismount();
                    }
                }
            }
        }

        entity.setPassengers(newPassengers);

        switch (entity.getDefinition().entityType()) {
            case HORSE, SKELETON_HORSE, DONKEY, MULE, RAVAGER -> {
                entity.getDirtyMetadata().put(EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES, 181.0f);
                entity.updateBedrockMetadata();
            }
        }
    }
}
