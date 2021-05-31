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

@Getter
public enum Potion {
    WATER(0),
    MUNDANE(1),
    THICK(3),
    AWKWARD(4),
    NIGHT_VISION(5),
    LONG_NIGHT_VISION(6),
    INVISIBILITY(7),
    LONG_INVISIBILITY(8),
    LEAPING(9),
    STRONG_LEAPING(11),
    LONG_LEAPING(10),
    FIRE_RESISTANCE(12),
    LONG_FIRE_RESISTANCE(13),
    SWIFTNESS(14),
    STRONG_SWIFTNESS(16),
    LONG_SWIFTNESS(15),
    SLOWNESS(17),
    STRONG_SLOWNESS(42),
    LONG_SLOWNESS(18),
    WATER_BREATHING(19),
    LONG_WATER_BREATHING(20),
    HEALING(21),
    STRONG_HEALING(22),
    HARMING(23),
    STRONG_HARMING(24),
    POISON(25),
    STRONG_POISON(27),
    LONG_POISON(26),
    REGENERATION(28),
    STRONG_REGENERATION(30),
    LONG_REGENERATION(29),
    STRENGTH(31),
    STRONG_STRENGTH(33),
    LONG_STRENGTH(32),
    WEAKNESS(34),
    LONG_WEAKNESS(35),
    LUCK(2), //does not exist
    TURTLE_MASTER(37),
    STRONG_TURTLE_MASTER(39),
    LONG_TURTLE_MASTER(38),
    SLOW_FALLING(40),
    LONG_SLOW_FALLING(41);

    public static final Potion[] VALUES = values();

    private final String javaIdentifier;
    private final short bedrockId;

    Potion(int bedrockId) {
        this.javaIdentifier = "minecraft:" + this.name().toLowerCase(Locale.ENGLISH);
        this.bedrockId = (short) bedrockId;
    }

    public static Potion getByJavaIdentifier(String javaIdentifier) {
        for (Potion potion : VALUES) {
            if (potion.javaIdentifier.equals(javaIdentifier)) {
                return potion;
            }
        }
        return null;
    }

    public static Potion getByBedrockId(int bedrockId) {
        for (Potion potion : VALUES) {
            if (potion.bedrockId == bedrockId) {
                return potion;
            }
        }
        return null;
    }
}
