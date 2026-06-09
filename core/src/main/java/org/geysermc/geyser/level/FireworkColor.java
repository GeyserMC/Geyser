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

package org.geysermc.geyser.level;

import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.util.ColorUtils;

import java.util.Locale;

public enum FireworkColor {
    BLACK(1973019),
    RED(11743532),
    GREEN(3887386),
    BROWN(5320730),
    BLUE(2437522),
    PURPLE(8073150),
    CYAN(2651799),
    LIGHT_GRAY(11250603, "silver"),
    GRAY(4408131),
    PINK(14188952),
    LIME(4312372),
    YELLOW(14602026),
    LIGHT_BLUE(6719955, "lightBlue"),
    MAGENTA(12801229),
    ORANGE(15435844),
    WHITE(15790320);

    private static final FireworkColor[] VALUES = values();

    private final TextColor color;

    @Getter
    private final String name;

    FireworkColor(int rgbValue, String name) {
        this.color = TextColor.color(rgbValue);
        this.name = name;
    }

    FireworkColor(int rgbValue) {
        this.color = TextColor.color(rgbValue);
        this.name = this.name().toLowerCase(Locale.ROOT);
    }

    public static byte fromJavaRGB(int rgbValue) {
        HSVLike hsv = ColorUtils.toHSV(rgbValue);
        return (byte) nearestTo(hsv).ordinal();
    }

    // Adapted from the Adventure project:
    // https://github.com/KyoriPowered/adventure/blob/09edf74409feb52d9147a5a811910de0721acf95/api/src/main/java/net/kyori/adventure/text/format/NamedTextColor.java#L193-L237
    /**
     * Find the firework color nearest to the provided color.
     *
     * @param any color to match
     * @return nearest named color. will always return a value
     * @since 4.0.0
     */
    private static @NonNull FireworkColor nearestTo(final HSVLike any) {
        float matchedDistance = Float.MAX_VALUE;
        FireworkColor match = VALUES[0];
        for (final FireworkColor potential : VALUES) {
            final float distance = ColorUtils.distance(any, potential.color.asHSV());
            if (distance < matchedDistance) {
                match = potential;
                matchedDistance = distance;
            }
            if (distance == 0) {
                break; // same colour! whoo!
            }
        }
        return match;
    }

    public static int fromBedrockId(int id) {
        for (FireworkColor fireworkColor : VALUES) {
            if (fireworkColor.ordinal() == id) {
                return fireworkColor.color.value();
            }
        }

        return WHITE.color.value();
    }
}
