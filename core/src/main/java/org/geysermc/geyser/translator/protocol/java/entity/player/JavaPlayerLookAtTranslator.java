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

import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket;

@Translator(packet = ClientboundPlayerLookAtPacket.class)
public class JavaPlayerLookAtTranslator extends PacketTranslator<ClientboundPlayerLookAtPacket> {
    @Override
    public void translate(GeyserSession session, ClientboundPlayerLookAtPacket packet) {
        var targetPosition = targetPosition(session, packet);
        var selfPosition = session.getPlayerEntity().getPosition();

        var xDelta = targetPosition.getX() - selfPosition.getX();
        var yDelta = targetPosition.getY() - selfPosition.getY();
        var zDelta = targetPosition.getZ() - selfPosition.getZ();
        var sqrt = Math.sqrt(xDelta * xDelta + zDelta * zDelta);

        var yaw = MathUtils.wrapDegrees(-Math.toDegrees(Math.atan2(yDelta, sqrt)));
        var pitch = MathUtils.wrapDegrees(Math.toDegrees(Math.atan2(zDelta, xDelta)) - 90.0);

        var self = session.getPlayerEntity();
        // headYaw is also set to yaw in this packet
        self.updateOwnRotation(yaw, pitch, yaw);
    }

    public Vector3f targetPosition(GeyserSession session, ClientboundPlayerLookAtPacket packet) {
        if (packet.getTargetEntityOrigin() != null) {
            var entityId = packet.getTargetEntityId();
            var target = session.getEntityCache().getEntityByJavaId(entityId);
            if (target != null) {
                return switch (packet.getTargetEntityOrigin()) {
                    case FEET -> target.getPosition();
                    case EYES -> target.getPosition().add(0, target.getBoundingBoxHeight(), 0);
                };
            }
        }
        return Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
    }
}
