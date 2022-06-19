/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;


public class GeyserCustomBlockPermutation implements CustomBlockPermutation {
    private final String condition;
    private final CustomBlockComponents components;

    public GeyserCustomBlockPermutation(String condition, CustomBlockComponents components) {
        this.condition = condition;
        this.components = components;
    }

    @Override
    public @NonNull String condition() {
        return condition;
    }

    @Override
    public CustomBlockComponents components() {
        return components;
    }

    public static class BuilderImpl implements Builder {
        private String condition;
        private CustomBlockComponents components;

        @Override
        public Builder condition(@NonNull String condition) {
            this.condition = condition;
            return this;
        }

        @Override
        public Builder components(CustomBlockComponents components) {
            this.components = components;
            return this;
        }

        @Override
        public CustomBlockPermutation build() {
            if (condition == null) {
                throw new IllegalArgumentException("Condition must be set");
            }
            return new GeyserCustomBlockPermutation(condition, components);
        }
    }
}
