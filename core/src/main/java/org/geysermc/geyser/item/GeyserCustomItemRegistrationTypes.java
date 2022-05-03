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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.CustomItemRegistrationTypes;

import java.util.Optional;
import java.util.OptionalInt;

public record GeyserCustomItemRegistrationTypes(Optional<Boolean> unbreaking,
                                                OptionalInt customModelData,
                                                OptionalInt damagePredicate) implements CustomItemRegistrationTypes {

    public static class Builder implements CustomItemRegistrationTypes.Builder {
        private Optional<Boolean> unbreaking = Optional.empty();
        private OptionalInt customModelData = OptionalInt.empty();
        private OptionalInt damagePredicate = OptionalInt.empty();

        @Override
        public CustomItemRegistrationTypes.Builder unbreaking(@NonNull Optional<Boolean> unbreaking) {
            this.unbreaking = unbreaking;
            return this;
        }

        @Override
        public CustomItemRegistrationTypes.Builder unbreaking(boolean unbreaking) {
            this.unbreaking = Optional.of(unbreaking);
            return this;
        }

        @Override
        public CustomItemRegistrationTypes.Builder customModelData(@NonNull OptionalInt customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        @Override
        public CustomItemRegistrationTypes.Builder customModelData(int customModelData) {
            this.customModelData = OptionalInt.of(customModelData);
            return this;
        }

        @Override
        public CustomItemRegistrationTypes.Builder damagePredicate(@NonNull OptionalInt damagePredicate) {
            this.damagePredicate = damagePredicate;
            return this;
        }

        @Override
        public CustomItemRegistrationTypes.Builder damagePredicate(int damagePredicate) {
            this.damagePredicate = OptionalInt.of(damagePredicate);
            return this;
        }

        @Override
        public CustomItemRegistrationTypes build() {
            return new GeyserCustomItemRegistrationTypes(unbreaking, customModelData, damagePredicate);
        }
    }
}
