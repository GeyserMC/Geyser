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

import java.util.function.BiFunction;
import java.util.function.Function;

record RangeDispatchPredicate(Property property, double threshold, int index, boolean normalised, boolean negated) implements MinecraftPredicate<ItemPredicateContext> {

    RangeDispatchPredicate(Property property, double threshold, boolean normalised) {
        this(property, threshold, 0, normalised, false);
    }

    RangeDispatchPredicate(Property property, double threshold, int index) {
        this(property, threshold, index, false, false);
    }

    RangeDispatchPredicate(Property property, double threshold) {
        this(property, threshold, 0, false, false);
    }

    public int propertyId() {
        return property.ordinal();
    }

    @Override
    public boolean test(ItemPredicateContext context) {
        Number value = property.getter.apply(context, this);
        if (normalised) {
            if (value == null || property.maxGetter == null) {
                return false;
            }
            Number max = property.maxGetter.apply(context);
            if (max == null || value.doubleValue() == 0.0) {
                return false;
            }
            value = value.doubleValue() / max.doubleValue();
        }

        return negated ? value.doubleValue() < threshold : value.doubleValue() >= threshold;
    }

    @Override
    public @NonNull MinecraftPredicate<ItemPredicateContext> negate() {
        return new RangeDispatchPredicate(property, threshold, index, normalised, !negated);
    }

    enum Property {
        BUNDLE_FULLNESS(ItemPredicateContext::bundleFullness),
        DAMAGE(ItemPredicateContext::damage, ItemPredicateContext::maxDamage),
        COUNT(ItemPredicateContext::count, ItemPredicateContext::maxStackSize),
        CUSTOM_MODEL_DATA((context, predicate) -> context.customModelDataFloat(predicate.index));

        private final BiFunction<ItemPredicateContext, RangeDispatchPredicate, Number> getter;
        private final Function<ItemPredicateContext, Number> maxGetter;

        Property(BiFunction<ItemPredicateContext, RangeDispatchPredicate, Number> getter, Function<ItemPredicateContext, Number> maxGetter) {
            this.getter = getter;
            this.maxGetter = maxGetter;
        }

        Property(BiFunction<ItemPredicateContext, RangeDispatchPredicate, Number> getter) {
            this(getter, null);
        }

        Property(Function<ItemPredicateContext, Number> getter, Function<ItemPredicateContext, Number> maxGetter) {
            this((context, rangeDispatchPredicate) -> getter.apply(context), maxGetter);
        }

        Property(Function<ItemPredicateContext, Number> getter) {
            this((context, rangeDispatchPredicate) -> getter.apply(context), null);
        }
    }
}
