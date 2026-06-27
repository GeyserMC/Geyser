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

import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaAttackRange;

public record JavaAttackRangeImpl(@IntRange(from = 0, to = 64) float minReach, @IntRange(from = 0, to = 64) float maxReach,
                              @IntRange(from = 0, to = 64) float minCreativeReach, @IntRange(from = 0, to = 64) float maxCreativeReach,
                              @IntRange(from = 0, to = 1) float hitboxMargin) implements JavaAttackRange {

    public static class Builder implements JavaAttackRange.Builder {
        private float minReach = 0.0F;
        private float maxReach = 3.0F;
        private float minCreativeReach = 0.0F;
        private float maxCreativeReach = 5.0F;
        private float hitboxMargin = 0.3F;

        @Override
        public Builder minReach(@IntRange(from = 0, to = 64) float minReach) {
            this.minReach = validateReachArgument(minReach);
            return this;
        }

        @Override
        public Builder maxReach(@IntRange(from = 0, to = 64) float maxReach) {
            this.maxReach = validateReachArgument(maxReach);
            return this;
        }

        @Override
        public Builder minCreativeReach(@IntRange(from = 0, to = 64) float minCreativeReach) {
            this.minCreativeReach = validateReachArgument(minCreativeReach);
            return this;
        }

        @Override
        public Builder maxCreativeReach(@IntRange(from = 0, to = 64) float maxCreativeReach) {
            this.maxCreativeReach = validateReachArgument(maxCreativeReach);
            return this;
        }

        @Override
        public Builder hitboxMargin(@IntRange(from = 0, to = 1) float hitboxMargin) {
            if (hitboxMargin < 0.0F || hitboxMargin > 1.0F) {
                throw new IllegalArgumentException("hitbox margin must be between 0 and 1 (inclusive)");
            }
            this.hitboxMargin = hitboxMargin;
            return this;
        }

        @Override
        public JavaAttackRange build() {
            return new JavaAttackRangeImpl(minReach, maxReach, minCreativeReach, maxCreativeReach, hitboxMargin);
        }

        private static float validateReachArgument(float reach) {
            if (reach < 0.0F || reach > 64.0F) {
                throw new IllegalArgumentException("reach must be between 0 and 64 (inclusive)");
            }
            return reach;
        }
    }
}
