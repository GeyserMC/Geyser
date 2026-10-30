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

package org.geysermc.geyser.session.cache;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@RequiredArgsConstructor
public class EntityEffectCache {
    /**
     * Java's dolphin's grace lowers water drag to 0.96, which Bedrock can't do. Instead, raise the underwater
     * acceleration. Bedrock's higher drag means it reaches top speed much sooner, so matching Java's terminal
     * velocity (0.05) outpaces Java players in practice; 0.04 matches the distance covered sprint-swimming for ~5 seconds.
     */
    private static final float DOLPHINS_GRACE_UNDERWATER_MOVEMENT = 0.04f;

    private final GeyserSession session;

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

    /* Used to calculate jumping velocity */
    @Getter
    private int jumpPower;

    public void setEffect(Effect effect, int effectAmplifier) {
        switch (effect) {
            case CONDUIT_POWER -> conduitPower = effectAmplifier + 1;
            case HASTE -> haste = effectAmplifier + 1;
            case MINING_FATIGUE -> miningFatigue = effectAmplifier + 1;
            case JUMP_BOOST -> jumpPower = effectAmplifier + 1;
            case DOLPHINS_GRACE -> updateUnderwaterMovement(DOLPHINS_GRACE_UNDERWATER_MOVEMENT);
        }
        entityEffects.add(effect);
    }

    public void removeEffect(Effect effect) {
        switch (effect) {
            case CONDUIT_POWER -> conduitPower = 0;
            case HASTE -> haste = 0;
            case MINING_FATIGUE -> miningFatigue = 0;
            case JUMP_BOOST -> jumpPower = 0;
            case DOLPHINS_GRACE -> updateUnderwaterMovement(GeyserAttributeType.UNDERWATER_MOVEMENT.getDefaultValue());
        }
        entityEffects.remove(effect);
    }

    /**
     * Clears all effects without them being individually removed, as the server re-sends them.
     */
    public void clearEffects() {
        if (entityEffects.contains(Effect.DOLPHINS_GRACE)) {
            updateUnderwaterMovement(GeyserAttributeType.UNDERWATER_MOVEMENT.getDefaultValue());
        }
        entityEffects.clear();
    }

    private void updateUnderwaterMovement(float value) {
        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.setRuntimeEntityId(session.getPlayerEntity().geyserId());
        packet.getAttributes().add(GeyserAttributeType.UNDERWATER_MOVEMENT.getAttribute(value));
        session.sendUpstreamPacket(packet);
    }
}
