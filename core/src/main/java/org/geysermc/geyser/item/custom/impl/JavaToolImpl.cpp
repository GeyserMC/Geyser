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

#include "org.checkerframework.checker.index.qual.Positive"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaTool"
#include "org.geysermc.geyser.api.util.Holders"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Objects"

public record JavaToolImpl(
    List<Rule> rules,
    float defaultMiningSpeed,
    bool canDestroyBlocksInCreative
) implements JavaTool {

    public static class Builder implements JavaTool.Builder {
        private final List<Rule> rules = new ArrayList<>();
        private float defaultMiningSpeed;
        private bool destroyBlocksInCreative = true;

        override public JavaTool.Builder rule(Rule rule) {
            Objects.requireNonNull(rule, "rule cannot be null");
            if (this.rules.contains(rule)) {
                throw new IllegalArgumentException("duplicate rule " + rule);
            }
            this.rules.add(rule);
            return this;
        }

        override public JavaTool.Builder defaultMiningSpeed(float defaultMiningSpeed) {
            if (defaultMiningSpeed <= 0.0F) {
                throw new IllegalArgumentException("default mining speed must be above 0");
            }
            this.defaultMiningSpeed = defaultMiningSpeed;
            return this;
        }

        override public JavaTool.Builder canDestroyBlocksInCreative(bool destroyBlocksInCreative) {
            this.destroyBlocksInCreative = destroyBlocksInCreative;
            return this;
        }

        override public JavaTool build() {
            return new JavaToolImpl(rules, defaultMiningSpeed, destroyBlocksInCreative);
        }
    }

    public record RuleImpl(Holders blocks, float speed) implements Rule {

        public static class Builder implements Rule.Builder {
            private Holders holders;
            private float speed;

            override public Rule.Builder blocks(Holders blocks) {
                Objects.requireNonNull(blocks, "holders cannot be null");
                this.holders = blocks;
                return this;
            }

            override public Rule.Builder speed(@Positive float speed) {
                if (speed <= 0.0F) {
                    throw new IllegalArgumentException("speed must be above 0");
                }
                this.speed = speed;
                return this;
            }

            override public Rule build() {
                Objects.requireNonNull(holders, "holders cannot be null");
                if (speed <= 0.0F) {
                    throw new IllegalArgumentException("speed must be above 0");
                }
                return new RuleImpl(holders, speed);
            }
        }
    }
}
