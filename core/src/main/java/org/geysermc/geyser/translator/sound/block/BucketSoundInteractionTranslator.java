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

package org.geysermc.geyser.translator.sound.block;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.sound.BlockSoundInteractionTranslator;
import org.geysermc.geyser.translator.sound.SoundTranslator;

@SoundTranslator(items = "bucket", ignoreSneakingWhileHolding = true)
public class BucketSoundInteractionTranslator implements BlockSoundInteractionTranslator {

    @Override
    public void translate(GeyserSession session, Vector3f position, String identifier) {
        if (session.getBucketScheduledFuture() == null) {
            return; // No bucket was really interacted with
        }
        GeyserItemStack itemStack = session.getPlayerInventory().getItemInHand();
        String handItemIdentifier = itemStack.getMapping(session).getJavaIdentifier();
        if (!BlockSoundInteractionTranslator.canInteract(session, itemStack, identifier)) {
            return;
        }
        LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
        soundEventPacket.setPosition(position);
        soundEventPacket.setIdentifier(":");
        soundEventPacket.setRelativeVolumeDisabled(false);
        soundEventPacket.setBabySound(false);
        soundEventPacket.setExtraData(-1);
        SoundEvent soundEvent = null;
        switch (handItemIdentifier) {
            case "minecraft:bucket":
                if (identifier.contains("water[")) {
                    soundEvent = SoundEvent.BUCKET_FILL_WATER;
                } else if (identifier.contains("lava[")) {
                    soundEvent = SoundEvent.BUCKET_FILL_LAVA;
                } else if (identifier.contains("powder_snow")) {
                    soundEvent = SoundEvent.BUCKET_FILL_POWDER_SNOW;
                }
                break;
            case "minecraft:lava_bucket":
                soundEvent = SoundEvent.BUCKET_EMPTY_LAVA;
                break;
            case "minecraft:axolotl_bucket":
            case "minecraft:cod_bucket":
            case "minecraft:salmon_bucket":
            case "minecraft:pufferfish_bucket":
            case "minecraft:tropical_fish_bucket":
                soundEvent = SoundEvent.BUCKET_EMPTY_FISH;
                break;
            case "minecraft:water_bucket":
                soundEvent = SoundEvent.BUCKET_EMPTY_WATER;
                break;
            case "minecraft:powder_snow_bucket":
                soundEvent = SoundEvent.BUCKET_EMPTY_POWDER_SNOW;
                break;
        }
        if (soundEvent != null) {
            soundEventPacket.setSound(soundEvent);
            session.sendUpstreamPacket(soundEventPacket);
            session.setBucketScheduledFuture(null);
        }
    }
}
