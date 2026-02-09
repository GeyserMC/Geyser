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
 * The consumable component is used to mark
 * an item as consumable. Further, it allows specifying
 * the consume duration and animation to play when consuming.
 * @since 2.9.3
 */
public interface JavaConsumable {

    /**
     * The seconds it takes to consume the item.
     * This it the amount of time the animation will play for. Defaults to {@code 1.6}.
     *
     * @return the consume duration, in seconds
     * @since 2.9.3
     */
    @Positive float consumeSeconds();

    /**
     * The animation that should play when consuming the item. Defaults to {@link Animation#EAT}.
     * 
     * @return the animation to play
     * @since 2.9.3
     */
    @NonNull Animation animation();

    /**
     * Creates a builder for the consumable component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(JavaConsumable.Builder.class);
    }

    /**
     * Creates a consumable component.
     *
     * @param consumeSeconds the consume duration, in seconds
     * @param animation the animation to play when consuming
     * @return the consumable component
     * @since 2.9.3
     */
    static @NonNull JavaConsumable of(float consumeSeconds, Animation animation) {
        return JavaConsumable.builder().consumeSeconds(consumeSeconds).animation(animation).build();
    }

    /**
     * Not all animations work perfectly on bedrock. Bedrock behavior is noted per animation. The {@code toot_horn} animation does not exist on bedrock, and is therefore not listed here.
     *
     * <p>Bedrock behavior is accurate as of version 1.21.94.</p>
     * @since 2.9.3
     */
    enum Animation {
        /**
         * Does nothing in 1st person, appears as drinking in 3rd person.
         * @since 2.9.3
         */
        NONE,
        /**
         * Appears to look correctly.
         * @since 2.9.3
         */
        EAT,
        /**
         * Appears to look correctly (same as eating, but without consume particles).
         * @since 2.9.3
         */
        DRINK,
        /**
         * Does nothing in 1st person, drinking in 3rd person.
         * @since 2.9.3
         */
        BLOCK,
        /**
         * Does nothing in 1st person, drinking in 3rd person.
         * @since 2.9.3
         */
        BOW,
        /**
         * Does nothing in 1st person, but looks like spear in 3rd person.
         * @since 2.9.3
         */
        SPEAR,
        /**
         * Does nothing in 1st person, drinking in 3rd person.
         * @since 2.9.3
         */
        CROSSBOW,
        /**
         * Does nothing in 1st person, but looks like spyglass in 3rd person.
         * @since 2.9.3
         */
        SPYGLASS,
        /**
         * Brush in 1st and 3rd person. Will look weird when not displayed handheld.
         * @since 2.9.3
         */
        BRUSH;
    }

    /**
     * Builder for the consumable component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaConsumable> {
        /**
         * Sets the time in seconds that consumption takes. This also
         * determines the animation length.
         *
         * @param consumeSeconds the seconds it takes to consume the item
         * @see JavaConsumable#consumeSeconds()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder consumeSeconds(@Positive float consumeSeconds);

        /**
         * Sets the animation to play when consuming the item.
         * See {@link Animation} for more details - some animations
         * do not work correctly.
         *
         * @param animation the animation to play
         * @see JavaConsumable#animation()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder animation(@NonNull Animation animation);

        /**
         * Creates the consumable component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaConsumable build();
    }
}
