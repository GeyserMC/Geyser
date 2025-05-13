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

import org.geysermc.geyser.api.predicate.PredicateCreator;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

/**
 * Contains creators for often-used "range dispatch" predicates, which check if a value in {@link org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext} is at or above a certain threshold.
 *
 * <p>Predicates created through these creators support conflict detection and proper sorting when used with custom items. It is as such preferred to use these over custom defined predicates when possible.</p>
 */
public interface ItemRangeDispatchPredicate {

    /**
     * Checks the item's bundle fullness. Returns the total stack count of all the items in a bundle.
     *
     * <p>Usually used with bundles, but works for any item with the {@code minecraft:bundle_contents} component.</p>
     *
     * @see ItemPredicateContext#bundleFullness()
     */
    PredicateCreator<ItemPredicateContext, Integer> BUNDLE_FULLNESS = threshold -> new RangeDispatchPredicate(RangeDispatchPredicate.Property.BUNDLE_FULLNESS, threshold);

    /**
     * Checks the item's damage value.
     *
     * @see ItemPredicateContext#damage()
     */
    PredicateCreator<ItemPredicateContext, Integer> DAMAGE = threshold -> new RangeDispatchPredicate(RangeDispatchPredicate.Property.DAMAGE, threshold);

    /**
     * Checks the item's damage value, normalised ({@code damage / max_damage}). Always returns false is {@code max_damage} is 0.
     *
     * @see ItemPredicateContext#damage()
     * @see ItemPredicateContext#maxDamage()
     */
    PredicateCreator<ItemPredicateContext, Double> DAMAGE_NORMALISED = threshold -> new RangeDispatchPredicate(RangeDispatchPredicate.Property.DAMAGE, threshold, true);

    /**
     * Checks the item's stack count.
     *
     * @see ItemPredicateContext#count()
     */
    PredicateCreator<ItemPredicateContext, Integer> COUNT = threshold -> new RangeDispatchPredicate(RangeDispatchPredicate.Property.COUNT, threshold);

    /**
     * Checks the item's stack count, normalised ({@code count / max_stack_size}).
     *
     * @see ItemPredicateContext#count()
     * @see ItemPredicateContext#maxStackSize() ()
     */
    PredicateCreator<ItemPredicateContext, Double> COUNT_NORMALISED = threshold -> new RangeDispatchPredicate(RangeDispatchPredicate.Property.COUNT, threshold, true);

    /**
     * Checks for the first of the item's custom model data floats, which is the custom model data value on Java servers below 1.21.4.
     *
     * @see ItemPredicateContext#customModelDataFloat(int)
     */
    PredicateCreator<ItemPredicateContext, Integer> LEGACY_CUSTOM_MODEL_DATA = data -> new RangeDispatchPredicate(RangeDispatchPredicate.Property.CUSTOM_MODEL_DATA, data, 0);

    /**
     * Checks one of the item's custom model data floats.
     *
     * @see ItemPredicateContext#customModelDataFloat(int)
     */
    PredicateCreator<ItemPredicateContext, CustomModelDataFloat> CUSTOM_MODEL_DATA = data -> new RangeDispatchPredicate(RangeDispatchPredicate.Property.CUSTOM_MODEL_DATA, data.value(), data.index());
}
