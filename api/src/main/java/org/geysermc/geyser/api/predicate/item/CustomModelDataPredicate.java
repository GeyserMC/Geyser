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

package org.geysermc.geyser.api.predicate.item;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

import java.util.Objects;

final class CustomModelDataPredicate {

    private CustomModelDataPredicate() {}

    record FlagPredicate(int index, boolean negated) implements MinecraftPredicate<ItemPredicateContext> {

        @Override
        public boolean test(ItemPredicateContext context) {
            return negated != context.customModelDataFlag(index);
        }

        @Override
        public @NonNull MinecraftPredicate<ItemPredicateContext> negate() {
            return new FlagPredicate(index, !negated);
        }
    }

    record StringPredicate(String string, int index, boolean negated) implements MinecraftPredicate<ItemPredicateContext> {

        @Override
        public boolean test(ItemPredicateContext context) {
            return negated != Objects.equals(string, context.customModelDataString(index));
        }

        @Override
        public @NonNull MinecraftPredicate<ItemPredicateContext> negate() {
            return new StringPredicate(string, index, !negated);
        }
    }
}
