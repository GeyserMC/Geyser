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

package org.geysermc.geyser.translator.level.event;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.geyser.session.GeyserSession;

public record SoundEventEventTranslator(SoundEvent soundEvent,
                                        String identifier, int extraData) implements LevelEventTranslator {
    @Override
    public void translate(GeyserSession session, ClientboundLevelEventPacket packet) {
        LevelSoundEventPacket levelSoundEvent = new LevelSoundEventPacket();
        levelSoundEvent.setSound(soundEvent);
        levelSoundEvent.setIdentifier(identifier);
        levelSoundEvent.setExtraData(extraData);
        levelSoundEvent.setRelativeVolumeDisabled(packet.isBroadcast());
        levelSoundEvent.setPosition(Vector3f.from(packet.getPosition().getX() + 0.5f, packet.getPosition().getY() + 0.5f, packet.getPosition().getZ() + 0.5f));
        levelSoundEvent.setBabySound(false);
        session.sendUpstreamPacket(levelSoundEvent);
    }
}
