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

package org.geysermc.geyser.inventory.item;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;

import java.util.Collections;
import java.util.Locale;

@Getter
public enum Potion {
    WATER(0, ArrowParticleColors.NONE),
    MUNDANE(1, ArrowParticleColors.NONE), // 2 is extended?
    THICK(3, ArrowParticleColors.NONE),
    AWKWARD(4, ArrowParticleColors.NONE),
    NIGHT_VISION(5, ArrowParticleColors.NIGHT_VISION),
    LONG_NIGHT_VISION(6, ArrowParticleColors.NIGHT_VISION),
    INVISIBILITY(7, ArrowParticleColors.INVISIBILITY),
    LONG_INVISIBILITY(8, ArrowParticleColors.INVISIBILITY),
    LEAPING(9, ArrowParticleColors.LEAPING),
    LONG_LEAPING(10, ArrowParticleColors.LEAPING),
    STRONG_LEAPING(11, ArrowParticleColors.LEAPING),
    FIRE_RESISTANCE(12, ArrowParticleColors.FIRE_RESISTANCE),
    LONG_FIRE_RESISTANCE(13, ArrowParticleColors.FIRE_RESISTANCE),
    SWIFTNESS(14, ArrowParticleColors.SWIFTNESS),
    LONG_SWIFTNESS(15, ArrowParticleColors.SWIFTNESS),
    STRONG_SWIFTNESS(16, ArrowParticleColors.SWIFTNESS),
    SLOWNESS(17, ArrowParticleColors.SLOWNESS),
    LONG_SLOWNESS(18, ArrowParticleColors.SLOWNESS),
    STRONG_SLOWNESS(42, ArrowParticleColors.SLOWNESS),
    TURTLE_MASTER(37, ArrowParticleColors.TURTLE_MASTER),
    LONG_TURTLE_MASTER(38, ArrowParticleColors.TURTLE_MASTER),
    STRONG_TURTLE_MASTER(39, ArrowParticleColors.TURTLE_MASTER),
    WATER_BREATHING(19, ArrowParticleColors.WATER_BREATHING),
    LONG_WATER_BREATHING(20, ArrowParticleColors.WATER_BREATHING),
    HEALING(21, ArrowParticleColors.HEALING),
    STRONG_HEALING(22, ArrowParticleColors.HEALING),
    HARMING(23, ArrowParticleColors.HARMING),
    STRONG_HARMING(24, ArrowParticleColors.HARMING),
    POISON(25, ArrowParticleColors.POISON),
    LONG_POISON(26, ArrowParticleColors.POISON),
    STRONG_POISON(27, ArrowParticleColors.POISON),
    REGENERATION(28, ArrowParticleColors.REGENERATION),
    LONG_REGENERATION(29, ArrowParticleColors.REGENERATION),
    STRONG_REGENERATION(30, ArrowParticleColors.REGENERATION),
    STRENGTH(31, ArrowParticleColors.STRENGTH),
    LONG_STRENGTH(32, ArrowParticleColors.STRENGTH),
    STRONG_STRENGTH(33, ArrowParticleColors.STRENGTH),
    WEAKNESS(34, ArrowParticleColors.WEAKNESS),
    LONG_WEAKNESS(35, ArrowParticleColors.WEAKNESS),
    LUCK(2, ArrowParticleColors.NONE), // does not exist in Bedrock
    SLOW_FALLING(40, ArrowParticleColors.SLOW_FALLING),
    LONG_SLOW_FALLING(41, ArrowParticleColors.SLOW_FALLING),
    WIND_CHARGING(43, ArrowParticleColors.WIND_CHARGING),
    WEAVING(44, ArrowParticleColors.WEAVING),
    OOZING(45, ArrowParticleColors.OOZING),
    INFESTATION(46, ArrowParticleColors.INFESTATION);

    public static final Potion[] VALUES = values();

    private final String javaIdentifier;
    private final short bedrockId;
    private final int javaColor;

    Potion(int bedrockId, int javaColor) {
        this.javaIdentifier = "minecraft:" + this.name().toLowerCase(Locale.ENGLISH);
        this.bedrockId = (short) bedrockId;
        this.javaColor = javaColor;
    }

    public int tippedArrowId() {
        // +1 likely to offset 0 as nothing?
        return this.bedrockId + 1;
    }

    public PotionContents toComponent() {
        return new PotionContents(this.ordinal(), -1, Collections.emptyList(), null);
    }

    public static Potion getByJavaIdentifier(String javaIdentifier) {
        for (Potion potion : VALUES) {
            if (potion.javaIdentifier.equals(javaIdentifier)) {
                return potion;
            }
        }
        return null;
    }

    public static @Nullable Potion getByJavaId(int javaId) {
        if (javaId >= 0 && javaId < VALUES.length) {
            return VALUES[javaId];
        }
        return null;
    }

    public static @Nullable Potion getByBedrockId(int bedrockId) {
        for (Potion potion : VALUES) {
            if (potion.bedrockId == bedrockId) {
                return potion;
            }
        }
        return null;
    }

    public static @Nullable Potion getByTippedArrowDamage(int bedrockId) {
        return getByBedrockId(bedrockId - 1);
    }

    public static byte toTippedArrowId(int javaParticleColor) {
        for (Potion potion : VALUES) {
            if (potion.javaColor == javaParticleColor) {
                return (byte) (potion.bedrockId + 1);
            }
        }
        return (byte) 0;
    }

    /**
     * For tipped arrow usage
     */
    private static final class ArrowParticleColors {
        static final int NONE = 1;
        static final int NIGHT_VISION = 2039713;
        static final int INVISIBILITY = 8356754;
        static final int LEAPING = 2293580;
        static final int FIRE_RESISTANCE = 14981690;
        static final int SWIFTNESS = 8171462;
        static final int SLOWNESS = 5926017;
        static final int TURTLE_MASTER = 7691106;
        static final int WATER_BREATHING = 3035801;
        static final int HEALING = 16262179;
        static final int HARMING = 4393481;
        static final int POISON = 5149489;
        static final int REGENERATION = 13458603;
        static final int STRENGTH = 9643043;
        static final int WEAKNESS = 4738376;
        static final int LUCK = 3381504;
        static final int SLOW_FALLING = 16773073;
        static final int WIND_CHARGING = 12438015;
        static final int WEAVING = 7891290;
        static final int OOZING = 10092451;
        static final int INFESTATION = 9214860;
    }
}
