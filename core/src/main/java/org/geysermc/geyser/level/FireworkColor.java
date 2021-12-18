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

package org.geysermc.geyser.level;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;

public enum FireworkColor {
    BLACK(1973019),
    RED(11743532),
    GREEN(3887386),
    BROWN(5320730),
    BLUE(2437522),
    PURPLE(8073150),
    CYAN(2651799),
    LIGHT_GRAY(11250603),
    GRAY(4408131),
    PINK(14188952),
    LIME(4312372),
    YELLOW(14602026),
    LIGHT_BLUE(6719955),
    MAGENTA(12801229),
    ORANGE(15435844),
    WHITE(15790320);

    private static final FireworkColor[] VALUES = values();

    private final TextColor color;

    FireworkColor(int rgbValue) {
        this.color = TextColor.color(rgbValue);
    }

    private static HSVLike toHSV(int rgbValue) {
        int r = (rgbValue & (255 << 16)) >> 16;
        int g = (rgbValue & (255 << 8)) >> 8;
        int b = rgbValue & 255;
        return HSVLike.fromRGB(r, g, b);
    }

    public static byte fromJavaRGB(int rgbValue) {
        HSVLike hsv = toHSV(rgbValue);
        return (byte) nearestTo(hsv).ordinal();
    }

    // The following two methods were adapted from the Adventure project:
    // https://github.com/KyoriPowered/adventure/blob/09edf74409feb52d9147a5a811910de0721acf95/api/src/main/java/net/kyori/adventure/text/format/NamedTextColor.java#L193-L237
    /**
     * Find the firework color nearest to the provided color.
     *
     * @param any color to match
     * @return nearest named color. will always return a value
     * @since 4.0.0
     */
    private static FireworkColor nearestTo(final HSVLike any) {
        float matchedDistance = Float.MAX_VALUE;
        FireworkColor match = VALUES[0];
        for (final FireworkColor potential : VALUES) {
            final float distance = distance(any, potential.color.asHSV());
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

    /**
     * Returns a distance metric to the other color.
     *
     * <p>This value is unitless and should only be used to compare with other firework colors.</p>
     *
     * @param other color to compare to
     * @return distance metric
     */
    private static float distance(final HSVLike self, final HSVLike other) {
        // weight hue more heavily than saturation and brightness. kind of magic numbers, but is fine for our use case of downsampling to a set of colors
        final float hueDistance = 3 * Math.min(Math.abs(self.h() - other.h()), 1f - Math.abs(self.h() - other.h()));
        final float saturationDiff = self.s() - other.s();
        final float valueDiff = self.v() - other.v();
        return hueDistance * hueDistance + saturationDiff * saturationDiff + valueDiff * valueDiff;
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
