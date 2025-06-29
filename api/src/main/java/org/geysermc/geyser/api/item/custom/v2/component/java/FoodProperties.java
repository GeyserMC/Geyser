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

package org.geysermc.geyser.api.item.custom.v2.component.java;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

/**
 * The food properties component can be used to define properties
 * for consumable items. This includes setting the nutrition and
 * saturation values, and whether the item can always be eaten.
 */
public interface FoodProperties {

    /**
     * The nutrition of the item. Defaults to {@code 0}.
     *
     * @return the nutrition
     */
    @NonNegative int nutrition();

    /**
     * The saturation of the item. Defaults to {@code 0.0}.
     *
     * @return the saturation
     */
    @NonNegative float saturation();

    /**
     * Whether this item can always be eaten,
     * even when not hungry. In vanilla, this would
     * include items such as golden apples. Defaults to {@code false}.
     *
     * @return whether the item can always be eaten
     */
    boolean canAlwaysEat();

    /**
     * Creates a builder for the food properties component.
     *
     * @return a new builder
     */
    static Builder builder() {
        return GeyserApi.api().provider(FoodProperties.Builder.class);
    }

    /**
     * Creates a food properties component.
     *
     * @param nutrition the nutrition of the item
     * @param saturation the saturation of the item
     * @param canAlwaysEat whether the item can always be eaten
     * @return the food properties component
     */
    static FoodProperties of(int nutrition, float saturation, boolean canAlwaysEat) {
        return FoodProperties.builder().nutrition(nutrition).saturation(saturation).canAlwaysEat(canAlwaysEat).build();
    }

    /**
     * Builder for the food properties component.
     */
    interface Builder extends GenericBuilder<FoodProperties> {

        /**
         * Sets the nutrition of the item which is added to the hunger bar.
         *
         * @param nutrition the nutrition of the item.
         * @see FoodProperties#nutrition()
         * @return this builder
         */
        @This
        Builder nutrition(@NonNegative int nutrition);

        /**
         * Sets the saturation of the item.
         * 
         * @param saturation the saturation of the item
         * @see FoodProperties#saturation()
         * @return this builder
         */
        @This
        Builder saturation(@NonNegative float saturation);

        /**
         * Sets whether this item can always be eaten,
         * even when the hunger bar is full.
         *
         * @param canAlwaysEat whether the item can always be eaten
         * @see FoodProperties#canAlwaysEat()
         * @return this builder
         */
        @This
        Builder canAlwaysEat(boolean canAlwaysEat);

        /**
         * Creates the food properties component.
         *
         * @return the new component
         */
        @Override
        FoodProperties build();
    }
}
