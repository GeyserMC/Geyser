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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.nukkitx.protocol.bedrock.packet.SetTimePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ServerUpdateTimePacket.class)
public class JavaUpdateTimeTranslator extends PacketTranslator<ServerUpdateTimePacket> {

    @Override
    public void translate(GeyserSession session, ServerUpdateTimePacket packet) {
        // Bedrock sends a GameRulesChangedPacket if there is no daylight cycle
        // Java just sends a negative long if there is no daylight cycle
        long time = packet.getTime();

        // https://minecraft.gamepedia.com/Day-night_cycle#24-hour_Minecraft_day
        SetTimePacket setTimePacket = new SetTimePacket();
        setTimePacket.setTime((int) Math.abs(time) % 24000);
        session.sendUpstreamPacket(setTimePacket);
        if (!session.isDaylightCycle() && time >= 0) {
            // Client thinks there is no daylight cycle but there is
            session.setDaylightCycle(true);
        } else if (session.isDaylightCycle() && time < 0) {
            // Client thinks there is daylight cycle but there isn't
            session.setDaylightCycle(false);
        }
    }
}
