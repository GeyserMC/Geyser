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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.GeyserProvided;
import org.jetbrains.annotations.ApiStatus;

/**
 * Contains predicates checking the {@code minecraft:custom_model_data} item component.
 * For checking for floats, use {@link ItemRangeDispatchPredicate#customModelData},
 * or {@link ItemRangeDispatchPredicate#legacyCustomModelData} for dealing with the pre-1.21.4 custom model data format.
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface CustomModelDataPredicate {

    /**
     * @see ItemConditionPredicate#customModelData(int)
     * @since 2.9.3
     */
    @GeyserProvided
    @ApiStatus.NonExtendable
    interface FlagPredicate extends MinecraftPredicate<ItemPredicateContext> {

        /**
         * @return the index to check the value of a flag on
         * @since 2.9.3
         */
        @NonNegative int index();

        /**
         * @return whether this predicate is negated. When negated, will return true for both false flags and missing flags
         * @since 2.9.3
         */
        boolean negated();
    }

    /**
     * @see ItemMatchPredicate#customModelData(int, String)
     * @since 2.9.3
     */
    @GeyserProvided
    @ApiStatus.NonExtendable
    interface StringPredicate extends MinecraftPredicate<ItemPredicateContext> {

        /**
         * @return the string to compare against. Can be null to check for a missing string
         * @since 2.9.3
         */
        @Nullable String string();

        /**
         * @return the index of the string to match the {@link StringPredicate#string()} against
         * @since 2.9.3
         */
        @NonNegative int index();

        /**
         * @return whether this predicate is negated
         * @since 2.9.3
         */
        boolean negated();
    }
}
