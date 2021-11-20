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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

/**
 * Sent by the client when moving a horse.
 */
@Translator(packet = MoveEntityAbsolutePacket.class)
public class BedrockMoveEntityAbsoluteTranslator extends PacketTranslator<MoveEntityAbsolutePacket> {

    @Override
    public void translate(GeyserSession session, MoveEntityAbsolutePacket packet) {
        session.setLastVehicleMoveTimestamp(System.currentTimeMillis());

        Entity ridingEntity = session.getRidingVehicleEntity();
        if (ridingEntity != null && session.getWorldBorder().isPassingIntoBorderBoundaries(packet.getPosition(), false)) {
            Vector3f position = Vector3f.from(ridingEntity.getPosition().getX(), packet.getPosition().getY(),
                    ridingEntity.getPosition().getZ());
            if (ridingEntity instanceof BoatEntity) {
                // Undo the changes usually applied to the boat
                ridingEntity.as(BoatEntity.class)
                        .moveAbsoluteWithoutAdjustments(position, ridingEntity.getYaw(),
                        ridingEntity.isOnGround(), true);
            } else {
                // This doesn't work if teleported is false
                ridingEntity.moveAbsolute(position,
                        ridingEntity.getYaw(), ridingEntity.getPitch(), ridingEntity.getHeadYaw(),
                        ridingEntity.isOnGround(), true);
            }
            return;
        }

        float y = packet.getPosition().getY();
        if (ridingEntity instanceof BoatEntity) {
            // Remove the offset to prevents boats from looking like they're floating in water
            y -= EntityDefinitions.BOAT.offset();
        }
        ServerboundMoveVehiclePacket ServerboundMoveVehiclePacket = new ServerboundMoveVehiclePacket(
                packet.getPosition().getX(), y, packet.getPosition().getZ(),
                packet.getRotation().getY() - 90, packet.getRotation().getX()
        );
        session.sendDownstreamPacket(ServerboundMoveVehiclePacket);
    }
}
