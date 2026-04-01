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

#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStopSoundPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.StopSoundPacket"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.SoundUtils"

@Translator(packet = ClientboundStopSoundPacket.class)
public class JavaStopSoundTranslator extends PacketTranslator<ClientboundStopSoundPacket> {

    override public void translate(GeyserSession session, ClientboundStopSoundPacket packet) {

        if (packet.getSound() == null) {
            StopSoundPacket stopPacket = new StopSoundPacket();
            stopPacket.setStoppingAllSound(true);
            stopPacket.setSoundName("");
            session.sendUpstreamPacket(stopPacket);
            return;
        }

        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setSoundName(SoundUtils.translatePlaySound(packet.getSound().asString()));
        stopSoundPacket.setStoppingAllSound(false);

        session.sendUpstreamPacket(stopSoundPacket);
        session.getGeyser().getLogger().debug("[StopSound] Stopped " + packet.getSound() + " -> " + stopSoundPacket.getSoundName());
    }
}
