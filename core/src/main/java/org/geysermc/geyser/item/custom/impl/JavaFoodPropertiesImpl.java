/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.custom.impl;

import org.checkerframework.checker.index.qual.NonNegative;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaFoodProperties;

public record JavaFoodPropertiesImpl(
    int nutrition,
    float saturation,
    boolean canAlwaysEat
) implements JavaFoodProperties {

    @SuppressWarnings("ConstantValue") // must enforce api
    public static class Builder implements JavaFoodProperties.Builder {
        private int nutrition;
        private float saturation;
        private boolean canAlwaysEat;

        @Override
        public JavaFoodProperties.Builder nutrition(@NonNegative int nutrition) {
            if (nutrition < 0) throw new IllegalArgumentException("nutrition cannot be negative");
            this.nutrition = nutrition;
            return this;
        }

        @Override
        public JavaFoodProperties.Builder saturation(@NonNegative float saturation) {
            if (saturation < 0) throw new IllegalArgumentException("saturation cannot be negative");
            this.saturation = saturation;
            return this;
        }

        @Override
        public JavaFoodProperties.Builder canAlwaysEat(boolean canAlwaysEat) {
            this.canAlwaysEat = canAlwaysEat;
            return this;
        }

        @Override
        public JavaFoodProperties build() {
            return new JavaFoodPropertiesImpl(nutrition, saturation, canAlwaysEat);
        }
    }

}
