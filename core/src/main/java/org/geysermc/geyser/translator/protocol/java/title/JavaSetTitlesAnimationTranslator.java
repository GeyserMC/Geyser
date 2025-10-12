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

package org.geysermc.geyser.translator.protocol.java.title;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitlesAnimationPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundSetTitlesAnimationPacket.class)
public class JavaSetTitlesAnimationTranslator extends PacketTranslator<ClientboundSetTitlesAnimationPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetTitlesAnimationPacket packet) {
        int fadeInTime = packet.getFadeIn();
        int stayTime = packet.getStay();
        int fadeOutTime = packet.getFadeOut();
        session.getWorldCache().setTitleTimes(fadeInTime, stayTime, fadeOutTime);
        // We need a tick rate multiplier as otherwise the timings are incorrect on different tick rates because
        // bedrock can only run at 20 TPS (50ms = 1 tick)
        int tickrateMultiplier = Math.round(session.getMillisecondsPerTick()) / 50;
        SetTitlePacket titlePacket = new SetTitlePacket();
        titlePacket.setType(SetTitlePacket.Type.TIMES);
        titlePacket.setText("");
        titlePacket.setFadeInTime(fadeInTime * tickrateMultiplier);
        titlePacket.setFadeOutTime(fadeOutTime * tickrateMultiplier);
        titlePacket.setStayTime(stayTime * tickrateMultiplier);
        titlePacket.setXuid("");
        titlePacket.setPlatformOnlineId("");
        session.sendUpstreamPacket(titlePacket);
    }
}
