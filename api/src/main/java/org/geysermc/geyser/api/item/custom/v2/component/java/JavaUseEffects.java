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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

/**
 * The use effects component is used to specify how the player behaves when using the item.
 *
 * <p>Currently, the {@code can_sprint} property is not supported on Bedrock.</p>
 * @since 2.9.3
 */
public interface JavaUseEffects {

    /**
     * The speed multiplier to apply to the player while using the item. Defaults to 0.2.
     *
     * @return the speed multiplier to apply while using the item
     * @since 2.9.3
     */
    @IntRange(from = 0, to = 1) float speedMultiplier();

    /**
     * Creates a builder for the use effects component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    /**
     * Creates a use effects component.
     *
     * @param speedMultiplier the speed multiplier to apply while using the item
     * @return the new use effects component
     * @since 2.9.3
     */
    static @NonNull JavaUseEffects of(@IntRange(from = 0, to = 1) float speedMultiplier) {
        return builder()
            .speedMultiplier(speedMultiplier)
            .build();
    }

    /**
     * Builder for the use effects component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaUseEffects> {

        /**
         * Sets the speed multiplier to apply while using the item.
         *
         * @param speedMultiplier the speed multiplier to apply while using the item
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder speedMultiplier(@IntRange(from = 0, to = 1) float speedMultiplier);

        /**
         * Creates the use effects component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaUseEffects build();
    }
}
