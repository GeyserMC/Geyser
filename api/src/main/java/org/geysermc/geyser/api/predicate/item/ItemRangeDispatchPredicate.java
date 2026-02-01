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

import org.checkerframework.checker.index.qual.NonNegative;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * Contains factories for often-used "range dispatch" predicates, which check if a value in {@link ItemPredicateContext} is at or above a certain threshold.
 *
 * <p>Predicates created through these factories support conflict detection and proper sorting when used with custom items.
 * It is as such preferred to use these over custom defined predicates when possible.</p>
 *
 * @see RangeDispatchPredicate
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface ItemRangeDispatchPredicate {

    /**
     * Creates a predicate checking the item's bundle fullness (the summed stack count of all the items in a bundle).
     *
     * <p>Usually used with bundles, but works for any item with the {@code minecraft:bundle_contents} component.</p>
     *
     * @see ItemPredicateContext#bundleFullness()
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> bundleFullness(int threshold) {
        return GeyserApi.api().provider(RangeDispatchPredicate.class, RangeDispatchPredicate.Property.BUNDLE_FULLNESS, threshold);
    }

    /**
     * Creates a predicate checking the item's damage value.
     *
     * @see ItemPredicateContext#damage()
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> damage(int threshold) {
        return GeyserApi.api().provider(RangeDispatchPredicate.class, RangeDispatchPredicate.Property.DAMAGE, threshold);
    }

    /**
     * Creates a predicate checking the item's damage value, normalized ({@code damage / max_damage}). Always returns false is {@code max_damage} is 0.
     *
     * @see ItemPredicateContext#damage()
     * @see ItemPredicateContext#maxDamage()
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> normalizedDamage(double threshold) {
        return GeyserApi.api().provider(RangeDispatchPredicate.class, RangeDispatchPredicate.Property.DAMAGE, threshold, true);
    }

    /**
     * Creates a predicate checking the item's stack count.
     *
     * @see ItemPredicateContext#count()
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> count(int threshold) {
        return GeyserApi.api().provider(RangeDispatchPredicate.class, RangeDispatchPredicate.Property.COUNT, threshold);
    }

    /**
     * Creates a predicate checking the item's stack count, normalized ({@code count / max_stack_size}).
     *
     * @see ItemPredicateContext#count()
     * @see ItemPredicateContext#maxStackSize()
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> normalizedCount(double threshold) {
        return GeyserApi.api().provider(RangeDispatchPredicate.class, RangeDispatchPredicate.Property.COUNT, threshold, true);
    }

    /**
     * Creates a predicate checking for the first of the item's custom model data floats, which is the custom model data value on Java servers below 1.21.4.
     *
     * @see ItemPredicateContext#customModelDataFloat(int)
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> legacyCustomModelData(int data) {
        return GeyserApi.api().provider(RangeDispatchPredicate.class, RangeDispatchPredicate.Property.CUSTOM_MODEL_DATA, data);
    }

    /**
     * Creates a predicate checking one of the item's custom model data floats.
     *
     * @see ItemPredicateContext#customModelDataFloat(int)
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> customModelData(@NonNegative int index, float data) {
        return GeyserApi.api().provider(RangeDispatchPredicate.class, RangeDispatchPredicate.Property.CUSTOM_MODEL_DATA, data, index);
    }
}
