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

package org.geysermc.geyser.item.custom.impl.predicates;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.RangeDispatchPredicate;
import org.geysermc.geyser.impl.GeyserCoreProvided;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@GeyserCoreProvided
public record GeyserRangeDispatchPredicate(@NonNull GeyserRangeDispatchProperty rangeProperty, double threshold, @NonNegative int index, boolean normalized, boolean negated) implements RangeDispatchPredicate {

    @Override
    public @NonNull Property property() {
        return Property.valueOf(rangeProperty.name());
    }

    public GeyserRangeDispatchPredicate {
        Objects.requireNonNull(rangeProperty, "range property cannot be null");
        //noinspection ConstantValue - must check API input
        if (index < 0) {
            throw new IllegalArgumentException("Negative index: " + index);
        }
    }

    public GeyserRangeDispatchPredicate(GeyserRangeDispatchProperty property, double threshold, boolean normalized) {
        this(property, threshold, 0, normalized, false);
    }

    public GeyserRangeDispatchPredicate(GeyserRangeDispatchProperty property, double threshold, int index) {
        this(property, threshold, index, false, false);
    }

    public GeyserRangeDispatchPredicate(GeyserRangeDispatchProperty property, double threshold) {
        this(property, threshold, 0, false, false);
    }

    @Override
    public boolean test(ItemPredicateContext context) {
        Number value = rangeProperty.getter.apply(context, this);
        if (normalized) {
            if (rangeProperty.maxGetter == null) {
                return false;
            }
            Number max = rangeProperty.maxGetter.apply(context);
            if (max == null || max.doubleValue() == 0.0) {
                return false;
            }
            value = value.doubleValue() / max.doubleValue();
        }

        return negated ? value.doubleValue() < threshold : value.doubleValue() >= threshold;
    }

    @Override
    public @NonNull MinecraftPredicate<ItemPredicateContext> negate() {
        return new GeyserRangeDispatchPredicate(rangeProperty, threshold, index, normalized, !negated);
    }

    public enum GeyserRangeDispatchProperty {
        BUNDLE_FULLNESS(ItemPredicateContext::bundleFullness),
        DAMAGE(ItemPredicateContext::damage, ItemPredicateContext::maxDamage),
        COUNT(ItemPredicateContext::count, ItemPredicateContext::maxStackSize),
        CUSTOM_MODEL_DATA((context, predicate) -> context.customModelDataFloat(predicate.index()));

        private final BiFunction<ItemPredicateContext, RangeDispatchPredicate, Number> getter;
        private final Function<ItemPredicateContext, Number> maxGetter;

        GeyserRangeDispatchProperty(BiFunction<ItemPredicateContext, RangeDispatchPredicate, Number> getter, Function<ItemPredicateContext, Number> maxGetter) {
            this.getter = getter;
            this.maxGetter = maxGetter;
        }

        GeyserRangeDispatchProperty(BiFunction<ItemPredicateContext, RangeDispatchPredicate, Number> getter) {
            this(getter, null);
        }

        GeyserRangeDispatchProperty(Function<ItemPredicateContext, Number> getter, Function<ItemPredicateContext, Number> maxGetter) {
            this((context, rangeDispatchPredicate) -> getter.apply(context), maxGetter);
        }

        GeyserRangeDispatchProperty(Function<ItemPredicateContext, Number> getter) {
            this((context, rangeDispatchPredicate) -> getter.apply(context), null);
        }
    }
}
