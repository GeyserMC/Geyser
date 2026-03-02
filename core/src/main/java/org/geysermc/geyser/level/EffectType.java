/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level;

import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

@Getter
public enum EffectType {
    NONE(0, null, 0x000000),

    SPEED(1, Effect.SPEED, 0x33EBFF),
    SLOWNESS(2, Effect.SLOWNESS, 0x8BAFE0),
    HASTE(3, Effect.HASTE, 0xD9C043),
    MINING_FATIGUE(4, Effect.MINING_FATIGUE, 0x4A4217),
    STRENGTH(5, Effect.STRENGTH, 0xFFC700),
    INSTANT_HEALTH(6, Effect.INSTANT_HEALTH, 0xF82423),
    INSTANT_DAMAGE(7, Effect.INSTANT_DAMAGE, 0xA9656A),
    JUMP_BOOST(8, Effect.JUMP_BOOST, 0xFDFF84),
    NAUSEA(9, Effect.NAUSEA, 0x551D4A),
    REGENERATION(10, Effect.REGENERATION, 0xCD5CAB),
    RESISTANCE(11, Effect.RESISTANCE, 0x9146F0),
    FIRE_RESISTANCE(12, Effect.FIRE_RESISTANCE, 0xFF9900),
    WATER_BREATHING(13, Effect.WATER_BREATHING, 0x98DAC0),
    INVISIBILITY(14, Effect.INVISIBILITY, 0xF6F6F6),
    BLINDNESS(15, Effect.BLINDNESS, 0x1F1F23),
    NIGHT_VISION(16, Effect.NIGHT_VISION, 0xC2FF66),
    HUNGER(17, Effect.HUNGER, 0x587653),
    WEAKNESS(18, Effect.WEAKNESS, 0x484D48),
    POISON(19, Effect.POISON, 0x87A363),
    WITHER(20, Effect.WITHER, 0x736156),
    HEALTH_BOOST(21, Effect.HEALTH_BOOST, 0xF87D23),
    ABSORPTION(22, Effect.ABSORPTION, 0x2552A5),
    SATURATION(23, Effect.SATURATION, 0xF82423),
    LEVITATION(24, Effect.LEVITATION, 0xCEFFFF),
    FATAL_POISON(25, null, 0x4E9331), // Bedrock-exclusive effect, maybe useful later if we map to closest color
    CONDUIT_POWER(26, Effect.CONDUIT_POWER, 0x1DC2D1),
    SLOW_FALLING(27, Effect.SLOW_FALLING, 0xF3CFB9),
    BAD_OMEN(28, Effect.BAD_OMEN, 0x0B6138),
    HERO_OF_THE_VILLAGE(29, Effect.HERO_OF_THE_VILLAGE, 0x44FF44),
    DARKNESS(30, Effect.DARKNESS, 0x292721),
    TRIAL_OMEN(31, Effect.TRIAL_OMEN, 0x16A6A6),
    WIND_CHARGED(32, Effect.WIND_CHARGED, 0xBDC9FF),
    WEAVING(33, Effect.WEAVING, 0x78695A),
    OOZING(34, Effect.OOZING, 0x99FFA3),
    INFESTED(35, Effect.INFESTED, 0x8C9B8C),
    RAID_OMEN(36, Effect.RAID_OMEN, 0xDE4058),
    BREATH_OF_THE_NAUTILUS(37, Effect.BREATH_OF_THE_NAUTILUS, 0x00FFEE),

    // All Java-exclusive effects as of 1.16.2
    GLOWING(0, Effect.GLOWING, 0x94A061),
    LUCK(0, Effect.LUCK, 0x59C106),
    BAD_LUCK(0, Effect.UNLUCK, 0xC0A44D),
    DOLPHINS_GRACE(0, Effect.DOLPHINS_GRACE, 0x88A3BE);

    private final int bedrockId;
    private final Effect javaEffect;
    private final int color;

    EffectType(int bedrockId, Effect javaEffect, int color) {
        this.bedrockId = bedrockId;
        this.javaEffect = javaEffect;
        this.color = color;
    }

    public static EffectType fromJavaEffect(Effect effect) {
        for (EffectType type : values()) {
            if (type.getJavaEffect() == effect) {
                return type;
            }
        }
        return EffectType.NONE;
    }

    public static EffectType fromColor(int color) {
        color = color & 0xFFFFFF; // Ignore alpha channel
        for (EffectType type : values()) {
            if (type.getColor() == color) {
                return type;
            }
        }
        return EffectType.NONE;
    }
}
