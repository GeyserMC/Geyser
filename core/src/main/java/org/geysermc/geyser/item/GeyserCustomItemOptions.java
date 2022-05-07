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

package org.geysermc.geyser.item;

import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.util.TriState;

import java.util.OptionalInt;

public record GeyserCustomItemOptions(TriState unbreaking,
                                      OptionalInt customModelData,
                                      OptionalInt damagePredicate) implements CustomItemOptions {

    public static class CustomItemOptionsBuilder implements CustomItemOptions.Builder {
        private TriState unbreaking = TriState.NOT_SET;
        private OptionalInt customModelData = OptionalInt.empty();
        private OptionalInt damagePredicate = OptionalInt.empty();

        @Override
        public Builder unbreaking(boolean unbreaking) {
            if (unbreaking) {
                this.unbreaking = TriState.TRUE;
            } else {
                this.unbreaking = TriState.FALSE;
            }
            return this;
        }

        @Override
        public Builder customModelData(int customModelData) {
            this.customModelData = OptionalInt.of(customModelData);
            return this;
        }

        @Override
        public Builder damagePredicate(int damagePredicate) {
            this.damagePredicate = OptionalInt.of(damagePredicate);
            return this;
        }

        @Override
        public CustomItemOptions build() {
            return new GeyserCustomItemOptions(unbreaking, customModelData, damagePredicate);
        }
    }
}
