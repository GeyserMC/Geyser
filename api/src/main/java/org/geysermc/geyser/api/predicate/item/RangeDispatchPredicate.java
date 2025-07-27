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
 */
@ApiStatus.NonExtendable
public interface RangeDispatchPredicate extends MinecraftPredicate<ItemPredicateContext>, GeyserProvided {

    /**
     * @see Property
     * @return the property type to check against
     */
    @NonNull Property property();

    /**
     * @return the threshold above which this predicate is true
     */
    double threshold();

    /**
     * Only used for {@link Property#CUSTOM_MODEL_DATA}.
     * If this predicate is any other property, this method will return 0.
     *
     * @return the index
     */
    @NonNegative int index();

    /**
     * Some predicates, such as {@link Property#DAMAGE} and {@link Property#COUNT},
     * can be normalised against their maximum properties. If the property is
     * not one of the two listed, this will always return false.
     *
     * @return whether this predicate is normalised
     */
    boolean normalised();

    /**
     * @return whether this predicate is negated
     */
    boolean negated();

    /**
     * The different properties available to check the range of
     */
    enum Property {
        BUNDLE_FULLNESS,
        DAMAGE,
        COUNT,
        CUSTOM_MODEL_DATA
    }
}
