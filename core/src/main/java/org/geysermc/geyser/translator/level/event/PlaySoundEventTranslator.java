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

package org.geysermc.geyser.translator.level.event;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public record PlaySoundEventTranslator(String name, float volume, boolean pitchSub, float pitchMul,
                                       float pitchAdd, boolean relative) implements LevelEventTranslator {
    @Override
    public void translate(GeyserSession session, ClientboundLevelEventPacket packet) {
        Random rand = ThreadLocalRandom.current();
        PlaySoundPacket playSoundPacket = new PlaySoundPacket();
        playSoundPacket.setSound(name);
        playSoundPacket.setPosition(!relative ? session.getPlayerEntity().getPosition() : Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()).add(0.5f, 0.5f, 0.5f));
        playSoundPacket.setVolume(volume);
        playSoundPacket.setPitch((pitchSub ? (rand.nextFloat() - rand.nextFloat()) : rand.nextFloat()) * pitchMul + pitchAdd); //replicates java client randomness
        session.sendUpstreamPacket(playSoundPacket);
    }
}
