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

package org.geysermc.connector.network.translators.item;

import lombok.Getter;

import java.util.Locale;

/**
 * Potion identifiers and their respective Bedrock IDs used with arrows.
 * https://minecraft.gamepedia.com/Arrow#Item_Data
 */
@Getter
public enum TippedArrowPotion {
    MUNDANE(2, ArrowParticleColors.NONE), // 3 is extended?
    THICK(4, ArrowParticleColors.NONE),
    AWKWARD(5, ArrowParticleColors.NONE),
    NIGHT_VISION(6, ArrowParticleColors.NIGHT_VISION),
    LONG_NIGHT_VISION(7, ArrowParticleColors.NIGHT_VISION),
    INVISIBILITY(8, ArrowParticleColors.INVISIBILITY),
    LONG_INVISIBILITY(9, ArrowParticleColors.INVISIBILITY),
    LEAPING(10, ArrowParticleColors.LEAPING),
    LONG_LEAPING(11, ArrowParticleColors.LEAPING),
    STRONG_LEAPING(12, ArrowParticleColors.LEAPING),
    FIRE_RESISTANCE(13, ArrowParticleColors.FIRE_RESISTANCE),
    LONG_FIRE_RESISTANCE(14, ArrowParticleColors.FIRE_RESISTANCE),
    SWIFTNESS(15, ArrowParticleColors.SWIFTNESS),
    LONG_SWIFTNESS(16, ArrowParticleColors.SWIFTNESS),
    STRONG_SWIFTNESS(17, ArrowParticleColors.SWIFTNESS),
    SLOWNESS(18, ArrowParticleColors.SLOWNESS),
    LONG_SLOWNESS(19, ArrowParticleColors.SLOWNESS),
    STRONG_SLOWNESS(43, ArrowParticleColors.SLOWNESS),
    WATER_BREATHING(20, ArrowParticleColors.WATER_BREATHING),
    LONG_WATER_BREATHING(21, ArrowParticleColors.WATER_BREATHING),
    HEALING(22, ArrowParticleColors.HEALING),
    STRONG_HEALING(23, ArrowParticleColors.HEALING),
    HARMING(24, ArrowParticleColors.HARMING),
    STRONG_HARMING(25, ArrowParticleColors.HARMING),
    POISON(26, ArrowParticleColors.POISON),
    LONG_POISON(27, ArrowParticleColors.POISON),
    STRONG_POISON(28, ArrowParticleColors.POISON),
    REGENERATION(29, ArrowParticleColors.REGENERATION),
    LONG_REGENERATION(30, ArrowParticleColors.REGENERATION),
    STRONG_REGENERATION(31, ArrowParticleColors.REGENERATION),
    STRENGTH(32, ArrowParticleColors.STRENGTH),
    LONG_STRENGTH(33, ArrowParticleColors.STRENGTH),
    STRONG_STRENGTH(34, ArrowParticleColors.STRENGTH),
    WEAKNESS(35, ArrowParticleColors.WEAKNESS),
    LONG_WEAKNESS(36, ArrowParticleColors.WEAKNESS),
    LUCK(2, ArrowParticleColors.NONE), // does not exist in Bedrock
    TURTLE_MASTER(38, ArrowParticleColors.TURTLE_MASTER),
    LONG_TURTLE_MASTER(39, ArrowParticleColors.TURTLE_MASTER),
    STRONG_TURTLE_MASTER(40, ArrowParticleColors.TURTLE_MASTER),
    SLOW_FALLING(41, ArrowParticleColors.SLOW_FALLING),
    LONG_SLOW_FALLING(42, ArrowParticleColors.SLOW_FALLING);

    private static final TippedArrowPotion[] VALUES = values();

    private final String javaIdentifier;
    private final short bedrockId;
    /**
     * The Java color associated with this ID.
     * Used for looking up Java arrow color entity metadata as Bedrock potion IDs, which is what is used for entities in Bedrock
     */
    private final int javaColor;

    TippedArrowPotion(int bedrockId, ArrowParticleColors arrowParticleColor) {
        this.javaIdentifier = "minecraft:" + this.name().toLowerCase(Locale.ENGLISH);
        this.bedrockId = (short) bedrockId;
        this.javaColor = arrowParticleColor.getColor();
    }

    public static TippedArrowPotion getByJavaIdentifier(String javaIdentifier) {
        for (TippedArrowPotion potion : VALUES) {
            if (potion.javaIdentifier.equals(javaIdentifier)) {
                return potion;
            }
        }
        return null;
    }

    public static TippedArrowPotion getByBedrockId(int bedrockId) {
        for (TippedArrowPotion potion : VALUES) {
            if (potion.bedrockId == bedrockId) {
                return potion;
            }
        }
        return null;
    }

    /**
     * @param color the potion color to look up
     * @return the tipped arrow potion that most closely resembles that color.
     */
    public static TippedArrowPotion getByJavaColor(int color) {
        for (TippedArrowPotion potion : VALUES) {
            if (potion.javaColor == color) {
                return potion;
            }
        }
        return null;
    }

    private enum ArrowParticleColors {
        NONE(-1),
        NIGHT_VISION(2039713),
        INVISIBILITY(8356754),
        LEAPING(2293580),
        FIRE_RESISTANCE(14981690),
        SWIFTNESS(8171462),
        SLOWNESS(5926017),
        TURTLE_MASTER(7691106),
        WATER_BREATHING(3035801),
        HEALING(16262179),
        HARMING(4393481),
        POISON(5149489),
        REGENERATION(13458603),
        STRENGTH(9643043),
        WEAKNESS(4738376),
        LUCK(3381504),
        SLOW_FALLING(16773073);

        @Getter
        private final int color;

        ArrowParticleColors(int color) {
            this.color = color;
        }
    }
}
