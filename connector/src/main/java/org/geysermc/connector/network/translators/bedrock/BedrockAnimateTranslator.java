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

@Translator(packet = AnimatePacket.class)
public class BedrockAnimateTranslator extends PacketTranslator<AnimatePacket> {

    private boolean is_steering_left;
    private boolean is_steering_right;

    @Override
    public void translate(AnimatePacket packet, GeyserSession session) {
        switch (packet.getAction()) {
            case SWING_ARM:
                ClientPlayerSwingArmPacket swingArmPacket = new ClientPlayerSwingArmPacket(Hand.MAIN_HAND);
                session.getDownstream().getSession().send(swingArmPacket);
                break;
            case ROW_LEFT:
                System.out.println("Animating rowing left...");
                System.out.println(packet.getRowingTime());
                // Packet value is a float of how long one has been rowing, so we convert that into a boolean
                is_steering_left = packet.getRowingTime() > 0.0;
                ClientSteerBoatPacket steerLeftPacket = new ClientSteerBoatPacket(is_steering_right, is_steering_left);
                session.getDownstream().getSession().send(steerLeftPacket);
                break;
            case ROW_RIGHT:
                System.out.println("Animating rowing right...");
                System.out.println(packet.getRowingTime());
                is_steering_right = packet.getRowingTime() > 0.0;
                ClientSteerBoatPacket steerRightPacket = new ClientSteerBoatPacket(is_steering_right, is_steering_left);
                session.getDownstream().getSession().send(steerRightPacket);
                break;
        }
    }
}
