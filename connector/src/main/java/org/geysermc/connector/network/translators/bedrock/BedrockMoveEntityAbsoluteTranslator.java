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

import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientVehicleMovePacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.connector.entity.BoatEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

/**
 * Sent by the client when moving a horse.
 */
@Translator(packet = MoveEntityAbsolutePacket.class)
public class BedrockMoveEntityAbsoluteTranslator extends PacketTranslator<MoveEntityAbsolutePacket> {

    @Override
    public void translate(MoveEntityAbsolutePacket packet, GeyserSession session) {
        if (session.getRidingVehicleEntity() != null && session.isPassingWorldBorderBoundaries(packet.getPosition(), false)) {
            Vector3f position = Vector3f.from(session.getRidingVehicleEntity().getPosition().getX(), packet.getPosition().getY(),
                    session.getRidingVehicleEntity().getPosition().getZ());
            if (session.getRidingVehicleEntity() instanceof BoatEntity) {
                // Undo the changes usually applied to the boat
                session.getRidingVehicleEntity().as(BoatEntity.class).moveAbsoluteWithoutAdjustments(session,
                        position, session.getRidingVehicleEntity().getRotation(),
                        session.getRidingVehicleEntity().isOnGround(), true);
            } else {
                // This doesn't work if teleported is false
                session.getRidingVehicleEntity().moveAbsolute(session, position,
                        session.getRidingVehicleEntity().getRotation(),
                        session.getRidingVehicleEntity().isOnGround(), true);
            }
            return;
        }
        float y = packet.getPosition().getY();
        if (session.getRidingVehicleEntity() instanceof BoatEntity) {
            // Remove some Y position to prevents boats from looking like they're floating in water
            // Not by the full boat offset because 1.16.100 complains and that's probably not good for the future
            y -= (EntityType.BOAT.getOffset() - 0.5f);
        }
        ClientVehicleMovePacket clientVehicleMovePacket = new ClientVehicleMovePacket(
                packet.getPosition().getX(), y, packet.getPosition().getZ(),
                packet.getRotation().getY() - 90, packet.getRotation().getX()
        );
        session.sendDownstreamPacket(clientVehicleMovePacket);
    }
}
