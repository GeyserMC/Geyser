/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientSteerBoatPacket;
import com.nukkitx.protocol.bedrock.packet.AnimatePacket;

import java.util.concurrent.TimeUnit;

@Translator(packet = AnimatePacket.class)
public class BedrockAnimateTranslator extends PacketTranslator<AnimatePacket> {

    private boolean isSteeringLeft;
    private boolean isSteeringRight;

    @Override
    public void translate(AnimatePacket packet, GeyserSession session) {
        // Stop the player sending animations before they have fully spawned into the server
        if (!session.isSpawned()) {
            return;
        }

        switch (packet.getAction()) {
            case SWING_ARM:
                // Delay so entity damage can be processed first
                session.getConnector().getGeneralThreadPool().schedule(() ->
                        session.getDownstream().getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND)),
                        25,
                        TimeUnit.MILLISECONDS
                );
                break;
            // These two might need to be flipped, but my recommendation is getting moving working first
            case ROW_LEFT:
                // Packet value is a float of how long one has been rowing, so we convert that into a boolean
                isSteeringLeft = packet.getRowingTime() > 0.0;
                ClientSteerBoatPacket steerLeftPacket = new ClientSteerBoatPacket(isSteeringRight, isSteeringLeft);
                session.getDownstream().getSession().send(steerLeftPacket);
                break;
            case ROW_RIGHT:
                isSteeringRight = packet.getRowingTime() > 0.0;
                ClientSteerBoatPacket steerRightPacket = new ClientSteerBoatPacket(isSteeringRight, isSteeringLeft);
                session.getDownstream().getSession().send(steerRightPacket);
                break;
        }
    }
}
