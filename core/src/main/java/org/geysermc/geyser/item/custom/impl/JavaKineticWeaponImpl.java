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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaKineticWeapon;

public record JavaKineticWeaponImpl(@NonNegative int delayTicks, @Nullable Condition dismountConditions) implements JavaKineticWeapon {

    public static class Builder implements JavaKineticWeapon.Builder {
        private int delayTicks = 0;
        private Condition dismountConditions = null;

        @Override
        public Builder delayTicks(int delayTicks) {
            if (delayTicks < 0) {
                throw new IllegalArgumentException("delay ticks must not be negative");
            }
            this.delayTicks = delayTicks;
            return this;
        }

        @Override
        public Builder dismountConditions(@Nullable Condition dismountConditions) {
            this.dismountConditions = dismountConditions;
            return this;
        }

        @Override
        public JavaKineticWeapon build() {
            return new JavaKineticWeaponImpl(delayTicks, dismountConditions);
        }
    }

    public record ConditionImpl(@NonNegative int maxDurationTicks, float minSpeed, float minRelativeSpeed) implements Condition {

        public static class Builder implements Condition.Builder {
            private final int maxDurationTicks;
            private float minSpeed = 0.0F;
            private float minRelativeSpeed = 0.0F;

            public Builder(int maxDurationTicks) {
                if (maxDurationTicks < 0) {
                    throw new IllegalArgumentException("max duration ticks must not be negative");
                }
                this.maxDurationTicks = maxDurationTicks;
            }

            @Override
            public Builder minSpeed(float minSpeed) {
                this.minSpeed = minSpeed;
                return this;
            }

            @Override
            public Builder minRelativeSpeed(float minRelativeSpeed) {
                this.minRelativeSpeed = minRelativeSpeed;
                return this;
            }

            @Override
            public Condition build() {
                return new ConditionImpl(maxDurationTicks, minSpeed, minRelativeSpeed);
            }
        }
    }
}
