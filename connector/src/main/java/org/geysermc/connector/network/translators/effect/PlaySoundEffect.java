/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.effect;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import lombok.Value;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Value
public class PlaySoundEffect implements Effect {
    /**
     * Bedrock playsound identifier
     */
    String name;

    /**
     * Volume of the sound
     */
    float volume;

    /**
     * If true, the initial value used for random pitch is the difference between two random floats.
     * If false, it is a single random float
     */
    boolean pitchSub;

    /**
     * Multiplier for random pitch value
     */
    float pitchMul;

    /**
     * Constant addition to random pitch value after multiplier
     */
    float pitchAdd;

    /**
     * True if the sound is meant to be played in 3d space
     */
    boolean relative;

    @Override
    public void handleEffectPacket(GeyserSession session, ServerPlayEffectPacket packet) {
        Random rand = ThreadLocalRandom.current();
        PlaySoundPacket playSoundPacket = new PlaySoundPacket();
        playSoundPacket.setSound(name);
        playSoundPacket.setPosition(!relative ? session.getPlayerEntity().getPosition() : Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()).add(0.5f, 0.5f, 0.5f));
        playSoundPacket.setVolume(volume);
        playSoundPacket.setPitch((pitchSub ? (rand.nextFloat() - rand.nextFloat()) : rand.nextFloat()) * pitchMul + pitchAdd); //replicates java client randomness
        session.sendUpstreamPacket(playSoundPacket);
    }
}
