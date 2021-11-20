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

package org.geysermc.geyser.translator.protocol.java.level;

import com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.level.sound.CustomSound;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundStopSoundPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.SoundMapping;

@Translator(packet = ClientboundStopSoundPacket.class)
public class JavaStopSoundTranslator extends PacketTranslator<ClientboundStopSoundPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundStopSoundPacket packet) {
        // Runs if all sounds are stopped
        if (packet.getSound() == null) {
            StopSoundPacket stopPacket = new StopSoundPacket();
            stopPacket.setStoppingAllSound(true);
            stopPacket.setSoundName("");
            session.sendUpstreamPacket(stopPacket);
            return;
        }

        String packetSound;
        if (packet.getSound() instanceof BuiltinSound) {
            packetSound = ((BuiltinSound) packet.getSound()).getName();
        } else if (packet.getSound() instanceof CustomSound) {
            packetSound = ((CustomSound) packet.getSound()).getName();
        } else {
            session.getGeyser().getLogger().debug("Unknown sound packet, we were unable to map this. " + packet.toString());
            return;
        }
        SoundMapping soundMapping = Registries.SOUNDS.get(packetSound.replace("minecraft:", ""));
        session.getGeyser().getLogger()
                .debug("[StopSound] Sound mapping " + packetSound + " -> "
                        + soundMapping + (soundMapping == null ? "[not found]" : "")
                        + " - " + packet.toString());
        String playsound;
        if (soundMapping == null || soundMapping.getPlaysound() == null) {
            // no mapping
            session.getGeyser().getLogger()
                    .debug("[StopSound] Defaulting to sound server gave us.");
            playsound = packetSound;
        } else {
            playsound = soundMapping.getPlaysound();
        }

        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setSoundName(playsound);
        // packet not mapped in the library
        stopSoundPacket.setStoppingAllSound(false);

        session.sendUpstreamPacket(stopSoundPacket);
        session.getGeyser().getLogger().debug("[StopSound] Packet sent - " + packet.toString() + " --> " + stopSoundPacket);
    }
}
