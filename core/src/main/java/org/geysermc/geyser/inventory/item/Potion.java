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
    WATER(0),
    MUNDANE(1),
    THICK(3),
    AWKWARD(4),
    NIGHT_VISION(5),
    LONG_NIGHT_VISION(6),
    INVISIBILITY(7),
    LONG_INVISIBILITY(8),
    LEAPING(9),
    LONG_LEAPING(10),
    STRONG_LEAPING(11),
    FIRE_RESISTANCE(12),
    LONG_FIRE_RESISTANCE(13),
    SWIFTNESS(14),
    LONG_SWIFTNESS(15),
    STRONG_SWIFTNESS(16),
    SLOWNESS(17),
    LONG_SLOWNESS(18),
    STRONG_SLOWNESS(42),
    TURTLE_MASTER(37),
    LONG_TURTLE_MASTER(38),
    STRONG_TURTLE_MASTER(39),
    WATER_BREATHING(19),
    LONG_WATER_BREATHING(20),
    HEALING(21),
    STRONG_HEALING(22),
    HARMING(23),
    STRONG_HARMING(24),
    POISON(25),
    LONG_POISON(26),
    STRONG_POISON(27),
    REGENERATION(28),
    LONG_REGENERATION(29),
    STRONG_REGENERATION(30),
    STRENGTH(31),
    LONG_STRENGTH(32),
    STRONG_STRENGTH(33),
    WEAKNESS(34),
    LONG_WEAKNESS(35),
    LUCK(2), //does not exist
    SLOW_FALLING(40),
    LONG_SLOW_FALLING(41);

    public static final Potion[] VALUES = values();

    private final String javaIdentifier;
    private final short bedrockId;

    Potion(int bedrockId) {
        this.javaIdentifier = "minecraft:" + this.name().toLowerCase(Locale.ENGLISH);
        this.bedrockId = (short) bedrockId;
    }

    public PotionContents toComponent() {
        return new PotionContents(this.ordinal(), -1, Collections.emptyList());
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
}
