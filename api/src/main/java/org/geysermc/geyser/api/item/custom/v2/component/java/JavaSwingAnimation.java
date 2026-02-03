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

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

/**
 * The swing animation component is used to specify the swing animation to play when attacking or interacting using the item.
 *
 * <p>Currently, only the duration property is supported on Bedrock.</p>
 * @since 2.9.3
 */
public interface JavaSwingAnimation {

    /**
     * The duration of the swing animation, in ticks. Defaults to 6.
     *
     * @return the duration of the swing animation, in ticks
     * @since 2.9.3
     */
    @Positive int duration();

    /**
     * Creates a builder for the swing animation component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    /**
     * Creates a swing animation component.
     *
     * @param duration the duration of the swing animation, in ticks
     * @return the new swing animation component
     * @since 2.9.3
     */
    static @NonNull JavaSwingAnimation of(@Positive int duration) {
        return builder()
            .duration(duration)
            .build();
    }

    /**
     * Builder for the swing animation component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaSwingAnimation> {

        /**
         * Sets the duration of the swing animation, in ticks.
         *
         * @param duration the duration of the swing animation, in ticks
         * @see JavaSwingAnimation#duration()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder duration(@Positive int duration);

        /**
         * Creates the swing animation component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaSwingAnimation build();
    }
}
