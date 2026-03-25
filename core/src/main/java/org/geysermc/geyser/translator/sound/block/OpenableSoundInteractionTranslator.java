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

package org.geysermc.geyser.translator.sound.block;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.sound.BlockSoundInteractionTranslator;
import org.geysermc.geyser.translator.sound.SoundTranslator;

@SoundTranslator(blocks = {"door", "fence_gate"})
public class OpenableSoundInteractionTranslator implements BlockSoundInteractionTranslator {

    @Override
    public void translate(GeyserSession session, Vector3f position, BlockState state) {
        String identifier = state.toString();
        if (identifier.contains("iron")) return;
        SoundEvent event = getSound(state.getValue(Properties.OPEN, false), identifier);
        LevelSoundEventPacket levelSoundEventPacket = new LevelSoundEventPacket();
        levelSoundEventPacket.setPosition(position.add(0.5, 0.5, 0.5));
        levelSoundEventPacket.setBabySound(false);
        levelSoundEventPacket.setRelativeVolumeDisabled(false);
        levelSoundEventPacket.setIdentifier(":");
        levelSoundEventPacket.setSound(event);
        levelSoundEventPacket.setExtraData(session.getBlockMappings().getBedrockBlock(state).getRuntimeId());
        session.sendUpstreamPacket(levelSoundEventPacket);
    }

    private SoundEvent getSound(boolean open, String identifier) {
        if (identifier.contains("_door")) {
            return open ? SoundEvent.DOOR_OPEN : SoundEvent.DOOR_CLOSE;
        }
        
        if (identifier.contains("_trapdoor")) {
            return open ? SoundEvent.TRAPDOOR_OPEN : SoundEvent.TRAPDOOR_CLOSE;
        }
        
        // Fence Gate
        return open ? SoundEvent.FENCE_GATE_OPEN : SoundEvent.FENCE_GATE_CLOSE;
    }
}
