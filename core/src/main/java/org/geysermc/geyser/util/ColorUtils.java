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

package org.geysermc.geyser.util;

import net.kyori.adventure.util.HSVLike;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.inventory.item.DyeColor;

import java.util.List;

public final class ColorUtils {

    private ColorUtils() {}

    public static HSVLike toHSV(int rgbValue) {
        int r = (rgbValue & (255 << 16)) >> 16;
        int g = (rgbValue & (255 << 8)) >> 8;
        int b = rgbValue & 255;
        return HSVLike.fromRGB(r, g, b);
    }

    // Adapted from the Adventure project:
    // https://github.com/KyoriPowered/adventure/blob/09edf74409feb52d9147a5a811910de0721acf95/api/src/main/java/net/kyori/adventure/text/format/NamedTextColor.java#L193-L237
    /**
     * Returns a distance metric to the other color.
     *
     * <p>This value is unitless and should only be used to compare with other firework colors.</p>
     *
     * @param other color to compare to
     * @return distance metric
     */
    public static float distance(final HSVLike self, final HSVLike other) {
        // weight hue more heavily than saturation and brightness. kind of magic numbers, but is fine for our use case of downsampling to a set of colors
        final float hueDistance = 3 * Math.min(Math.abs(self.h() - other.h()), 1f - Math.abs(self.h() - other.h()));
        final float saturationDiff = self.s() - other.s();
        final float valueDiff = self.v() - other.v();
        return hueDistance * hueDistance + saturationDiff * saturationDiff + valueDiff * valueDiff;
    }

    // The following six methods are "inspired by" Mojang's ARGB class:
    // https://mcsrc.dev/1/26.1.2/net/minecraft/util/ARGB
    public static int argbAlpha(int color) {
        return color >>> 24;
    }

    public static int argbRed(int color) {
        return color >> 16 & 0xFF;
    }

    public static int argbGreen(int color) {
        return color >> 8 & 0xFF;
    }

    public static int argbBlue(int color) {
        return color & 0xFF;
    }

    public static int argbColor(final int alpha, final int red, final int green, final int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    public static int argbOpaque(final int color) {
        return color | 0xFF000000;
    }

    // "Inspired by" Mojang's DyedItemColor class:
    // https://mcsrc.dev/1/26.1.2/net/minecraft/world/item/component/DyedItemColor
    public static int mixDyes(@Nullable Integer current, List<DyeColor> dyes) {
        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;
        int totalIntensity = 0;
        int colorCount = 0;
        if (current != null) {
            int red = argbRed(current);
            int green = argbGreen(current);
            int blue = argbBlue(current);
            totalIntensity += Math.max(red, Math.max(green, blue));
            totalRed += red;
            totalGreen += green;
            totalBlue += blue;
            colorCount++;
        }

        for (DyeColor dye : dyes) {
            int color = dye.getTextureDiffuseColor();
            int red = argbRed(color);
            int green = argbGreen(color);
            int blue = argbBlue(color);
            totalIntensity += Math.max(red, Math.max(green, blue));
            totalRed += red;
            totalGreen += green;
            totalBlue += blue;
            colorCount++;
        }

        int red = totalRed / colorCount;
        int green = totalGreen / colorCount;
        int blue = totalBlue / colorCount;
        float averageIntensity = (float) totalIntensity / colorCount;
        float resultIntensity = Math.max(red, Math.max(green, blue));
        red = (int) (red * averageIntensity / resultIntensity);
        green = (int) (green * averageIntensity / resultIntensity);
        blue = (int) (blue * averageIntensity / resultIntensity);
        return argbColor(0, red, green, blue);
    }
}
