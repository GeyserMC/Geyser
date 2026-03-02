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

package org.geysermc.geyser.translator.protocol.java.level;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundSetTimePacket.class)
public class JavaSetTimeTranslator extends PacketTranslator<ClientboundSetTimePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetTimePacket packet) {
        session.setWorldTicks(packet.getGameTime());

        long time = packet.getDayTime();

        // https://minecraft.wiki/w/Day-night_cycle#24-hour_Minecraft_day
        SetTimePacket setTimePacket = new SetTimePacket();
        // We use modulus to prevent an integer overflow
        // 24000 is the range of ticks that a Minecraft day can be; we times by 8 so all moon phases are visible
        // (Last verified behavior: Bedrock 1.18.12 / Java 1.18.2)
        setTimePacket.setTime((int) (Math.abs(time) % (24000 * 8)));
        session.sendUpstreamPacket(setTimePacket);

        // We need to send a gamerule if this changed
        if (session.isDaylightCycle() != packet.isTickDayTime()) {
            session.setDaylightCycle(packet.isTickDayTime());
        }
    }
}
