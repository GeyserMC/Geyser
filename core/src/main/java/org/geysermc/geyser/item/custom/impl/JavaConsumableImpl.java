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

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaConsumable;

import java.util.Objects;

public record JavaConsumableImpl(
    float consumeSeconds,
    @NonNull Animation animation
) implements JavaConsumable {

    public static class Builder implements JavaConsumable.Builder {
        private float consumeSeconds = 1.6F;
        private Animation animation = Animation.EAT;

        @Override
        public Builder consumeSeconds(@Positive float consumeSeconds) {
            if (consumeSeconds <= 0.0F) {
                throw new IllegalArgumentException("consume seconds must be above 0");
            }
            this.consumeSeconds = consumeSeconds;
            return this;
        }

        @Override
        public Builder animation(@NonNull Animation animation) {
            Objects.requireNonNull(animation, "animation cannot be null");
            this.animation = animation;
            return this;
        }

        @Override
        public JavaConsumable build() {
            return new JavaConsumableImpl(consumeSeconds, animation);
        }
    }
}
