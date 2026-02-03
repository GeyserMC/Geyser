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
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaTool;
import org.geysermc.geyser.api.util.Holders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record JavaToolImpl(
    List<@NonNull Rule> rules,
    float defaultMiningSpeed,
    boolean canDestroyBlocksInCreative
) implements JavaTool {

    public static class Builder implements JavaTool.Builder {
        private final List<Rule> rules = new ArrayList<>();
        private float defaultMiningSpeed;
        private boolean destroyBlocksInCreative = true;

        @Override
        public JavaTool.Builder rule(@NonNull Rule rule) {
            Objects.requireNonNull(rule, "rule cannot be null");
            if (this.rules.contains(rule)) {
                throw new IllegalArgumentException("duplicate rule " + rule);
            }
            this.rules.add(rule);
            return this;
        }

        @Override
        public JavaTool.Builder defaultMiningSpeed(float defaultMiningSpeed) {
            if (defaultMiningSpeed <= 0.0F) {
                throw new IllegalArgumentException("default mining speed must be above 0");
            }
            this.defaultMiningSpeed = defaultMiningSpeed;
            return this;
        }

        @Override
        public JavaTool.Builder canDestroyBlocksInCreative(boolean destroyBlocksInCreative) {
            this.destroyBlocksInCreative = destroyBlocksInCreative;
            return this;
        }

        @Override
        public JavaTool build() {
            return new JavaToolImpl(rules, defaultMiningSpeed, destroyBlocksInCreative);
        }
    }

    public record RuleImpl(Holders blocks, float speed) implements Rule {

        public static class Builder implements Rule.Builder {
            private Holders holders;
            private float speed;

            @Override
            public Rule.Builder blocks(@NonNull Holders blocks) {
                Objects.requireNonNull(blocks, "holders cannot be null");
                this.holders = blocks;
                return this;
            }

            @Override
            public Rule.Builder speed(@Positive float speed) {
                if (speed <= 0.0F) {
                    throw new IllegalArgumentException("speed must be above 0");
                }
                this.speed = speed;
                return this;
            }

            @Override
            public Rule build() {
                Objects.requireNonNull(holders, "holders cannot be null");
                if (speed <= 0.0F) {
                    throw new IllegalArgumentException("speed must be above 0");
                }
                return new RuleImpl(holders, speed);
            }
        }
    }
}
