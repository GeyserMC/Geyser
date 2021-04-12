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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientSteerVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientVehicleMovePacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.packet.PlayerInputPacket;
import org.geysermc.connector.entity.BoatEntity;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.living.animal.horse.AbstractHorseEntity;
import org.geysermc.connector.entity.living.animal.horse.LlamaEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

/**
 * Sent by the client for minecarts and boats.
 */
@Translator(packet = PlayerInputPacket.class)
public class BedrockPlayerInputTranslator extends PacketTranslator<PlayerInputPacket> {

    @Override
    public void translate(PlayerInputPacket packet, GeyserSession session) {
        ClientSteerVehiclePacket clientSteerVehiclePacket = new ClientSteerVehiclePacket(
                packet.getInputMotion().getX(), packet.getInputMotion().getY(), packet.isJumping(), packet.isSneaking()
        );

        session.sendDownstreamPacket(clientSteerVehiclePacket);

        // Bedrock only sends movement vehicle packets while moving
        // This allows horses to take damage while standing on magma
        Entity vehicle = session.getRidingVehicleEntity();
        boolean sendMovement = false;
        if (vehicle instanceof AbstractHorseEntity && !(vehicle instanceof LlamaEntity)) {
            sendMovement = vehicle.isOnGround();
        } else if (vehicle instanceof BoatEntity) {
            if (vehicle.getPassengers().size() == 1) {
                // The player is the only rider
                sendMovement = true;
            } else {
                // Check if the player is the front rider
                Vector3f seatPos = session.getPlayerEntity().getMetadata().getVector3f(EntityData.RIDER_SEAT_POSITION, null);
                if (seatPos != null && seatPos.getX() > 0) {
                    sendMovement = true;
                }
            }
        }
        if (sendMovement) {
            long timeSinceVehicleMove = System.currentTimeMillis() - session.getLastVehicleMoveTimestamp();
            if (timeSinceVehicleMove >= 100) {
                Vector3f vehiclePosition = vehicle.getPosition();
                Vector3f vehicleRotation = vehicle.getRotation();

                if (vehicle instanceof BoatEntity) {
                    // Remove some Y position to prevents boats flying up
                    vehiclePosition = vehiclePosition.down(EntityType.BOAT.getOffset());
                }

                ClientVehicleMovePacket clientVehicleMovePacket = new ClientVehicleMovePacket(
                        vehiclePosition.getX(), vehiclePosition.getY(), vehiclePosition.getZ(),
                        vehicleRotation.getX() - 90, vehicleRotation.getY()
                );
                session.sendDownstreamPacket(clientVehicleMovePacket);
            }
        }
    }
}
