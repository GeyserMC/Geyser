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

package org.geysermc.geyser.util;

import com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.level.sound.CustomSound;
import com.github.steveice10.mc.protocol.data.game.level.sound.Sound;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.SoundMapping;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Locale;

public final class SoundUtils {

    /**
     * Maps a sound name to a sound event, null if one
     * does not exist.
     *
     * @param sound the sound name
     * @return a sound event from the given sound
     */
    private static SoundEvent toSoundEvent(String sound) {
        try {
            return SoundEvent.valueOf(sound.toUpperCase(Locale.ROOT).replace(".", "_"));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Translates a Java Custom or Builtin Sound to its Bedrock equivalent
     *
     * @param sound the sound to translate
     * @return a Bedrock sound
     */
    public static String translatePlaySound(Sound sound) {
        String packetSound;
        if (sound instanceof BuiltinSound builtinSound) {
            packetSound = builtinSound.getName();
        } else if (sound instanceof CustomSound customSound) {
            packetSound = customSound.getName();
        } else {
            GeyserImpl.getInstance().getLogger().debug("Unknown sound, we were unable to map this. " + sound);
            return "";
        }

        // Drop the Minecraft namespace if applicable
        if (packetSound.startsWith("minecraft:")) {
            packetSound = packetSound.substring("minecraft:".length());
        }

        SoundMapping soundMapping = Registries.SOUNDS.get(packetSound);
        if (soundMapping == null || soundMapping.getPlaysound() == null) {
            // no mapping
            GeyserImpl.getInstance().getLogger().debug("[PlaySound] Defaulting to sound server gave us for " + sound);
            return packetSound;
        }
        return soundMapping.getPlaysound();
    }

    /**
     * Translates and plays a Java Builtin Sound for a Bedrock client
     *
     * @param session the Bedrock client session.
     * @param javaSound the builtin sound to play
     * @param position the position
     * @param pitch the pitch
     */
    public static void playBuiltinSound(GeyserSession session, BuiltinSound javaSound, Vector3f position, float volume, float pitch) {
        String packetSound = javaSound.getName();

        SoundMapping soundMapping = Registries.SOUNDS.get(packetSound);
        if (soundMapping == null) {
            session.getGeyser().getLogger().debug("[Builtin] Sound mapping for " + packetSound + " not found");
            return;
        }

        if (soundMapping.getPlaysound() != null) {
            // We always prefer the PlaySound mapping because we can control volume and pitch
            PlaySoundPacket playSoundPacket = new PlaySoundPacket();
            playSoundPacket.setSound(soundMapping.getPlaysound());
            playSoundPacket.setPosition(position);
            playSoundPacket.setVolume(volume);
            playSoundPacket.setPitch(pitch);
            session.sendUpstreamPacket(playSoundPacket);
            return;
        }

        if (soundMapping.isLevelEvent()) {
            LevelEventPacket levelEventPacket = new LevelEventPacket();
            levelEventPacket.setPosition(position);
            levelEventPacket.setData(0);
            levelEventPacket.setType(LevelEventType.valueOf(soundMapping.getBedrock()));
            session.sendUpstreamPacket(levelEventPacket);
            return;
        }

        LevelSoundEventPacket soundPacket = new LevelSoundEventPacket();
        SoundEvent sound = SoundUtils.toSoundEvent(soundMapping.getBedrock());
        if (sound == null) {
            sound = SoundUtils.toSoundEvent(packetSound);
        }
        if (sound == null) {
            session.getGeyser().getLogger().debug("[Builtin] Sound for original '" + packetSound + "' to mappings '" + soundMapping.getBedrock()
                    + "' was not a playable level sound, or has yet to be mapped to an enum in SoundEvent.");
            return;
        }

        soundPacket.setSound(sound);
        soundPacket.setPosition(position);
        soundPacket.setIdentifier(soundMapping.getIdentifier());
        if (sound == SoundEvent.NOTE) {
            // Minecraft Wiki: 2^(x/12) = Java pitch where x is -12 to 12
            // Java sends the note value as above starting with -12 and ending at 12
            // Bedrock has a number for each type of note, then proceeds up the scale by adding to that number
            soundPacket.setExtraData(soundMapping.getExtraData() + (int)(Math.round((Math.log10(pitch) / Math.log10(2)) * 12)) + 12);
        } else if (sound == SoundEvent.PLACE && soundMapping.getExtraData() == -1) {
            if (!soundMapping.getIdentifier().equals(":")) {
                int javaId = BlockRegistries.JAVA_IDENTIFIERS.getOrDefault(soundMapping.getIdentifier(), BlockStateValues.JAVA_AIR_ID);
                soundPacket.setExtraData(session.getBlockMappings().getBedrockBlockId(javaId));
            } else {
                session.getGeyser().getLogger().debug("PLACE sound mapping identifier was invalid! Please report: " + soundMapping);
            }
            soundPacket.setIdentifier(":");
        } else {
            soundPacket.setExtraData(soundMapping.getExtraData());
        }

        soundPacket.setBabySound(false); // might need to adjust this in the future
        soundPacket.setRelativeVolumeDisabled(false);
        session.sendUpstreamPacket(soundPacket);
    }

    private SoundUtils() {
    }
}
