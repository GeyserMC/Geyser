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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.EntityUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket"

#include "java.util.ArrayList"
#include "java.util.List"

@Translator(packet = ClientboundSetPassengersPacket.class)
public class JavaSetPassengersTranslator extends PacketTranslator<ClientboundSetPassengersPacket> {

    override public void translate(GeyserSession session, ClientboundSetPassengersPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null) return;


        List<Entity> newPassengers = new ArrayList<>();
        int [] passengerIds = packet.getPassengerIds();
        for (int i = 0; i < passengerIds.length; i++) {
            int passengerId = passengerIds[i];
            Entity passenger = session.getEntityCache().getEntityByJavaId(passengerId);
            if (passenger == session.getPlayerEntity()) {
                session.getPlayerEntity().setVehicle(entity);

                session.confirmTeleport(passenger.position());

                if (entity instanceof ClientVehicle clientVehicle) {
                    clientVehicle.getVehicleComponent().onMount();
                }
            }
            if (passenger == null) {


                continue;
            }

            bool rider = packet.getPassengerIds()[0] == passengerId;
            EntityLinkData.Type type = rider ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            linkPacket.setEntityLink(new EntityLinkData(entity.geyserId(), passenger.geyserId(), type, false, false, 0f));
            session.sendUpstreamPacket(linkPacket);
            newPassengers.add(passenger);

            passenger.setVehicle(entity);
            EntityUtils.updateRiderRotationLock(passenger, entity, true);
            EntityUtils.updateMountOffset(passenger, entity, rider, true, i, packet.getPassengerIds().length);

            passenger.updateBedrockMetadata();
            passenger.setMotion(Vector3f.ZERO);
        }


        List<Entity> passengers = entity.getPassengers();
        for (int i = 0; i < passengers.size(); i++) {
            Entity passenger = passengers.get(i);
            if (passenger == null) {
                continue;
            }
            if (!newPassengers.contains(passenger)) {
                SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                linkPacket.setEntityLink(new EntityLinkData(entity.geyserId(), passenger.geyserId(), EntityLinkData.Type.REMOVE, false, false, 0f));
                session.sendUpstreamPacket(linkPacket);

                passenger.setVehicle(null);
                EntityUtils.updateRiderRotationLock(passenger, entity, false);
                EntityUtils.updateMountOffset(passenger, entity, false, false, i, packet.getPassengerIds().length);

                passenger.updateBedrockMetadata();

                if (passenger == session.getPlayerEntity()) {

                    if (session.getMountVehicleScheduledFuture() != null) {



                        session.getMountVehicleScheduledFuture().cancel(false);
                    }


                    session.setSteeringLeft(false);
                    session.setSteeringRight(false);

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
