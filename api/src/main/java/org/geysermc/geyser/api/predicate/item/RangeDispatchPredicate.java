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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.GeyserProvided;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a predicate that tests if a specific property is above a specific threshold.
 * These can be created using the factories in the {@link ItemRangeDispatchPredicate} interface.
 *
 * @see ItemRangeDispatchPredicate
 * @since 2.9.3
 */
@GeyserProvided
@ApiStatus.NonExtendable
public interface RangeDispatchPredicate extends MinecraftPredicate<ItemPredicateContext> {

    /**
     * @see Property
     * @return the property type to check against
     * @since 2.9.3
     */
    @NonNull Property property();

    /**
     * @return the threshold above which this predicate is true
     * @since 2.9.3
     */
    double threshold();

    /**
     * Only used for {@link Property#CUSTOM_MODEL_DATA}.
     * If this predicate is any other property, this method will return 0.
     *
     * @return the index
     * @since 2.9.3
     */
    @NonNegative int index();

    /**
     * Some predicates, such as {@link Property#DAMAGE} and {@link Property#COUNT},
     * can be normalized against their maximum properties. If the property is
     * not one of the two listed, this will always return false.
     *
     * @return whether this predicate is normalized
     * @since 2.9.3
     */
    boolean normalized();

    /**
     * @return whether this predicate is negated
     * @since 2.9.3
     */
    boolean negated();

    /**
     * The different properties available to check the range of
     * @since 2.9.3
     */
    enum Property {
        /**
         * Checks the amount of items in a bundle
         * @since 2.9.3
         */
        BUNDLE_FULLNESS,

        /**
         * Checks the damage of the item
         * @since 2.9.3
         */
        DAMAGE,

        /**
         * Checks the amount of items in the item stack
         * @since 2.9.3
         */
        COUNT,

        /**
         * Checks the floats list in the custom model data component
         * @since 2.9.3
         */
        CUSTOM_MODEL_DATA
    }
}
