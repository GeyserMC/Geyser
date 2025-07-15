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
 */
public interface Consumable {

    /**
     * The seconds it takes to consume the item.
     * This it the amount of time the animation will play for. Defaults to {@code 1.6}.
     *
     * @return the consume duration, in seconds
     */
    @Positive float consumeSeconds();

    /**
     * The animation that should play when consuming the item. Defaults to {@link Animation#EAT}.
     * 
     * @return the animation to play
     */
    @NonNull Animation animation();

    /**
     * Creates a builder for the consumable component.
     *
     * @return a new builder
     */
    static Builder builder() {
        return GeyserApi.api().provider(Consumable.Builder.class);
    }

    /**
     * Creates a consumable component.
     *
     * @param consumeSeconds the consume duration, in seconds
     * @param animation the animation to play when consuming
     * @return the consumable component
     */
    static Consumable of(float consumeSeconds, Animation animation) {
        return Consumable.builder().consumeSeconds(consumeSeconds).animation(animation).build();
    }

    /**
     * Not all animations work perfectly on bedrock. Bedrock behaviour is noted per animation. The {@code toot_horn} animation does not exist on bedrock, and is therefore not listed here.
     *
     * <p>Bedrock behaviour is accurate as of version 1.21.94.</p>
     */
    enum Animation {
        /**
         * Does nothing in 1st person, appears as drinking in 3rd person.
         */
        NONE,
        /**
         * Appears to look correctly.
         */
        EAT,
        /**
         * Appears to look correctly (same as eating, but without consume particles).
         */
        DRINK,
        /**
         * Does nothing in 1st person, drinking in 3rd person.
         */
        BLOCK,
        /**
         * Does nothing in 1st person, drinking in 3rd person.
         */
        BOW,
        /**
         * Does nothing in 1st person, but looks like spear in 3rd person.
         */
        SPEAR,
        /**
         * Does nothing in 1st person, drinking in 3rd person.
         */
        CROSSBOW,
        /**
         * Does nothing in 1st person, but looks like spyglass in 3rd person.
         */
        SPYGLASS,
        /**
         * Brush in 1st and 3rd person. Will look weird when not displayed handheld.
         */
        BRUSH;
    }

    /**
     * Builder for the consumable component.
     */
    interface Builder extends GenericBuilder<Consumable> {
        /**
         * Sets the time in seconds that consumption takes. This also
         * determines the animation length.
         *
         * @param consumeSeconds the seconds it takes to consume the item
         * @see Consumable#consumeSeconds()
         * @return this builder
         */
        @This
        Builder consumeSeconds(@Positive float consumeSeconds);

        /**
         * Sets the animation to play when consuming the item.
         * See {@link Animation} for more details - some animations
         * do not work correctly.
         *
         * @param animation the animation to play
         * @see Consumable#animation()
         * @return this builder
         */
        @This
        Builder animation(@NonNull Animation animation);

        /**
         * Creates the consumable component.
         *
         * @return the new component
         */
        @Override
        Consumable build();
    }
}
