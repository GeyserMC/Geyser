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

package org.geysermc.connector.network.translators.effect;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import lombok.Value;
import org.geysermc.connector.network.session.GeyserSession;

@Value
public class SoundEventEffect implements Effect {
    /**
     * Bedrock sound event
     */
    SoundEvent soundEvent;

    /**
     * Entity identifier. Usually an empty string
     */
    String identifier;

    /**
     * Extra data. Usually -1
     */
    int extraData;

    @Override
    public void handleEffectPacket(GeyserSession session, ServerPlayEffectPacket packet) {
        LevelSoundEventPacket levelSoundEvent = new LevelSoundEventPacket();
        levelSoundEvent.setSound(soundEvent);
        levelSoundEvent.setIdentifier(identifier);
        levelSoundEvent.setExtraData(extraData);
        levelSoundEvent.setRelativeVolumeDisabled(packet.isBroadcast());
        levelSoundEvent.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()).add(0.5f, 0.5f, 0.5f));
        levelSoundEvent.setBabySound(false);
        session.sendUpstreamPacket(levelSoundEvent);
    }
}
