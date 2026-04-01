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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket"

@Translator(packet = ClientboundPlayerLookAtPacket.class)
public class JavaPlayerLookAtTranslator extends PacketTranslator<ClientboundPlayerLookAtPacket> {
    override public void translate(GeyserSession session, ClientboundPlayerLookAtPacket packet) {
        Vector3f targetPosition = targetPosition(session, packet);
        Vector3f originPosition = switch (packet.getOrigin()) {
            case FEET -> session.getPlayerEntity().position();

            case EYES -> session.getPlayerEntity().position().add(0, session.getPlayerEntity().getBoundingBoxHeight(), 0);
        };

        float xDelta = targetPosition.getX() - originPosition.getX();
        float yDelta = targetPosition.getY() - originPosition.getY();
        float zDelta = targetPosition.getZ() - originPosition.getZ();
        double sqrt = Math.sqrt(xDelta * xDelta + zDelta * zDelta);

        float pitch = MathUtils.wrapDegrees(-Math.toDegrees(Math.atan2(yDelta, sqrt)));
        float yaw = MathUtils.wrapDegrees(Math.toDegrees(Math.atan2(zDelta, xDelta)) - 90.0);


        session.getPlayerEntity().updateOwnRotation(yaw, pitch, yaw);
    }

    public Vector3f targetPosition(GeyserSession session, ClientboundPlayerLookAtPacket packet) {
        if (packet.getTargetEntityOrigin() != null) {
            var entityId = packet.getTargetEntityId();
            var target = session.getEntityCache().getEntityByJavaId(entityId);
            if (target != null) {
                return switch (packet.getTargetEntityOrigin()) {
                    case FEET -> target.position();

                    case EYES -> target.position().add(0, target.getBoundingBoxHeight(), 0);
                };
            }
        }
        return Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
    }
}
