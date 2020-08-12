/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import org.geysermc.connector.entity.type.EntityType;

public class EntityUtils {

    /**
     * Convert Java edition effect IDs to Bedrock edition
     *
     * @param effect Effect to convert
     * @return The numeric ID for the Bedrock edition effect
     */
    public static int toBedrockEffectId(Effect effect) {
        switch (effect) {
            case GLOWING:
            case LUCK:
            case UNLUCK:
            case DOLPHINS_GRACE:
                // All Java-exclusive effects as of 1.16.2
                return 0;
            case LEVITATION:
                return 24;
            case CONDUIT_POWER:
                return 26;
            case SLOW_FALLING:
                return 27;
            case BAD_OMEN:
                return 28;
            case HERO_OF_THE_VILLAGE:
                return 29;
            default:
                return effect.ordinal() + 1;
        }
    }

    /**
     * Converts a MobType to a Bedrock edition EntityType, returns null if the EntityType is not found
     *
     * @param type The MobType to convert
     * @return Converted EntityType
     */
    public static EntityType toBedrockEntity(com.github.steveice10.mc.protocol.data.game.entity.type.EntityType type) {
        try {
            return EntityType.valueOf(type.name());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
