/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.util.ColorUtils;

import java.util.Locale;

@Getter
public enum DyeColor {
    WHITE(0xf9fffe),
    ORANGE(0xf9801d),
    MAGENTA(0xc74ebd),
    LIGHT_BLUE(0x3ab3da),
    YELLOW(0xfed83d),
    LIME(0x80c71f),
    PINK(0xf38baa),
    GRAY(0x474f52),
    LIGHT_GRAY(0x9d9d97),
    CYAN(0x169c9c),
    PURPLE(0x8932b8),
    BLUE(0x3c44aa),
    BROWN(0x835432),
    GREEN(0x5e7c16),
    RED(0xb02e26),
    BLACK(0x1d1d21);

    private static final DyeColor[] VALUES = values();

    private final String javaIdentifier;
    // Take from Mojang's own DyeColor: https://mcsrc.dev/1/26.1.2/net/minecraft/world/item/DyeColor
    private final int textureDiffuseColor;

    DyeColor(int textureDiffuseColor) {
        this.javaIdentifier = this.name().toLowerCase(Locale.ROOT);
        this.textureDiffuseColor = ColorUtils.argbOpaque(textureDiffuseColor);
    }

    public static @Nullable DyeColor getById(int id) {
        if (id >= 0 && id < VALUES.length) {
            return VALUES[id];
        }
        return null;
    }

    public static DyeColor getOrDefault(@Nullable Integer id, DyeColor defaultValue) {
        if (id == null) {
            return defaultValue;
        }
        DyeColor color = getById(id);
        return color == null ? defaultValue : color;
    }

    public static @Nullable DyeColor getByJavaIdentifier(String javaIdentifier) {
        for (DyeColor dyeColor : VALUES) {
            if (dyeColor.javaIdentifier.equals(javaIdentifier)) {
                return dyeColor;
            }
        }
        return null;
    }
}
