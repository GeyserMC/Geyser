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

import java.util.Locale;

@Getter
public enum BannerPattern {
    BASE("b"),
    SQUARE_BOTTOM_LEFT("bl"),
    SQUARE_BOTTOM_RIGHT("br"),
    SQUARE_TOP_LEFT("tl"),
    SQUARE_TOP_RIGHT("tr"),
    STRIPE_BOTTOM("bs"),
    STRIPE_TOP("ts"),
    STRIPE_LEFT("ls"),
    STRIPE_RIGHT("rs"),
    STRIPE_CENTER("cs"),
    STRIPE_MIDDLE("ms"),
    STRIPE_DOWNRIGHT("drs"),
    STRIPE_DOWNLEFT("dls"),
    SMALL_STRIPES("ss"),
    CROSS("cr"),
    STRAIGHT_CROSS("sc"),
    TRIANGLE_BOTTOM("bt"),
    TRIANGLE_TOP("tt"),
    TRIANGLES_BOTTOM("bts"),
    TRIANGLES_TOP("tts"),
    DIAGONAL_LEFT("ld"),
    DIAGONAL_UP_RIGHT("rd"),
    DIAGONAL_UP_LEFT("lud"),
    DIAGONAL_RIGHT("rud"),
    CIRCLE("mc"),
    RHOMBUS("mr"),
    HALF_VERTICAL("vh"),
    HALF_HORIZONTAL("hh"),
    HALF_VERTICAL_RIGHT("vhr"),
    HALF_HORIZONTAL_BOTTOM("hhb"),
    BORDER("bo"),
    CURLY_BORDER("cbo"),
    GRADIENT("gra"),
    GRADIENT_UP("gru"),
    BRICKS("bri"),
    GLOBE("glb"),
    CREEPER("cre"),
    SKULL("sku"),
    FLOWER("flo"),
    MOJANG("moj"),
    PIGLIN("pig");

    private static final BannerPattern[] VALUES = values();

    private final String javaIdentifier;
    private final String bedrockIdentifier;

    BannerPattern(String bedrockIdentifier) {
        this.javaIdentifier = "minecraft:" + this.name().toLowerCase(Locale.ROOT);
        this.bedrockIdentifier = bedrockIdentifier;
    }

    public static @Nullable BannerPattern getByJavaIdentifier(String javaIdentifier) {
        for (BannerPattern bannerPattern : VALUES) {
            if (bannerPattern.javaIdentifier.equals(javaIdentifier)) {
                return bannerPattern;
            }
        }
        return null;
    }

    public static @Nullable BannerPattern getByBedrockIdentifier(String bedrockIdentifier) {
        for (BannerPattern bannerPattern : VALUES) {
            if (bannerPattern.bedrockIdentifier.equals(bedrockIdentifier)) {
                return bannerPattern;
            }
        }
        return null;
    }
}
