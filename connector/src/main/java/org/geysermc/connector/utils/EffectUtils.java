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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import lombok.NonNull;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.registry.Registries;

/**
 * Util for particles and effects.
 */
public class EffectUtils {

    /**
     * Used for area effect clouds.
     *
     * @param type the Java particle to search for
     * @return the Bedrock integer ID of the particle, or -1 if it does not exist
     */
    public static int getParticleId(GeyserSession session, @NonNull ParticleType type) {
        LevelEventType levelEventType = Registries.PARTICLES.get(type).getLevelEventType();
        if (levelEventType == null) {
            return -1;
        }

        // Remove the legacy bit applied to particles for LevelEventType serialization
        return session.getUpstream().getSession().getPacketCodec().getHelper().getLevelEventId(levelEventType) & ~0x4000;
    }
}
