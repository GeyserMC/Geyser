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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

public class EntityEffectCache {
    /**
     * Used to clear effects on dimension switch.
     */
    @Getter
    private final Set<Effect> entityEffects = EnumSet.noneOf(Effect.class);

    /* Used to track mining speed */
    @Getter
    private int conduitPower;
    @Getter
    private int haste;
    @Getter
    private int miningFatigue;

    public void setEffect(Effect effect, int effectAmplifier) {
        switch (effect) {
            case CONDUIT_POWER -> conduitPower = effectAmplifier + 1;
            case HASTE -> haste = effectAmplifier + 1;
            case MINING_FATIGUE -> miningFatigue = effectAmplifier + 1;
        }
        entityEffects.add(effect);
    }

    public void removeEffect(Effect effect) {
        switch (effect) {
            case CONDUIT_POWER -> conduitPower = 0;
            case HASTE -> haste = 0;
            case MINING_FATIGUE -> miningFatigue = 0;
        }
        entityEffects.remove(effect);
    }
}
