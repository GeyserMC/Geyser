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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayBuiltinSoundPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.SoundUtils;

@Translator(packet = ServerPlayBuiltinSoundPacket.class)
public class JavaPlayBuiltinSoundTranslator extends PacketTranslator<ServerPlayBuiltinSoundPacket> {

    @Override
    public void translate(ServerPlayBuiltinSoundPacket packet, GeyserSession session) {
        String packetSound = packet.getSound().getName();

        SoundUtils.SoundMapping soundMapping = SoundUtils.fromJava(packetSound);
        session.getConnector().getLogger().debug("[Builtin] Sound mapping " + packetSound + " -> "
                        + soundMapping + (soundMapping == null ? "[not found]" : "")
                        + " - " + packet.toString());
        if (soundMapping == null) {
            return;
        }

        LevelSoundEventPacket soundPacket = new LevelSoundEventPacket();
        SoundEvent sound = SoundUtils.toSoundEvent(soundMapping.getBedrock());
        if (sound == null) {
            sound = SoundUtils.toSoundEvent(soundMapping.getBedrock());
        }
        if (sound == null) {
            sound = SoundUtils.toSoundEvent(packetSound);
        }
        if (sound == null) {
            session.getConnector().getLogger().debug("[Builtin] Sound for original " + packetSound + " to mappings " + soundPacket
                            + " was not a playable level sound, or has yet to be mapped to an enum in "
                            + "NukkitX SoundEvent ");

        } else {
            session.getConnector().getLogger().debug("[Builtin] Sound for original " + packetSound + " to mappings " + soundPacket
                            + " was not found in NukkitX SoundEvent, but original packet sound name was.");
        }

        soundPacket.setSound(sound);
        soundPacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
        if (sound == SoundEvent.NOTE) {
            // Minecraft Wiki: 2^(x/12) = Java pitch where x is -12 to 12
            // Java sends the note value as above starting with -12 and ending at 12
            // Bedrock has a number for each type of note, then proceeds up the scale by adding to that number
            soundPacket.setExtraData(soundMapping.getExtraData() + (int)(Math.round((Math.log10(packet.getPitch()) / Math.log10(2)) * 12)) + 12);
        } else {
            soundPacket.setExtraData(soundMapping.getExtraData());
        }
        soundPacket.setIdentifier(":"); // ???
        soundPacket.setBabySound(false); // might need to adjust this in the future
        soundPacket.setRelativeVolumeDisabled(false);
        session.getUpstream().sendPacket(soundPacket);
        session.getConnector().getLogger().debug("Packet sent - " + packet.toString() + " --> " + soundPacket.toString());
    }
}
