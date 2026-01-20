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
 * The attack range component is used to specify the
 * attack ranges of an item. Because of limitations on Bedrock, these only apply to items that also have
 * a {@link JavaItemDataComponents#KINETIC_WEAPON} or a {@link JavaItemDataComponents#PIERCING_WEAPON} component.
 * @since 2.9.3
 */
public interface JavaAttackRange {

    /**
     * The minimum distance in blocks from the user to the target for the user to be able to attack that target. Defaults to 0.
     *
     * @return the minimum distance for attacks, in blocks
     * @since 2.9.3
     */
    @IntRange(from = 0, to = 64) float minReach();

    /**
     * The maximum distance in blocks from the user to the target for the user to be able to attack that target. Defaults to 3.
     *
     * @return the maximum distance for attacks, in blocks
     * @since 2.9.3
     */
    @IntRange(from = 0, to = 64) float maxReach();

    /**
     * The minimum distance in blocks from the user to the target for the user to be able to attack that target, if the user is in creative mode. Defaults to 0.
     *
     * @return the minimum distance for attacks made in creative mode, in blocks
     * @since 2.9.3
     */
    @IntRange(from = 0, to = 64) float minCreativeReach();

    /**
     * The maximum distance in blocks from the user to the target for the user to be able to attack that target, if the user is in creative mode. Defaults to 5.
     *
     * @return the maximum distance for attacks made in creative mode, in blocks
     * @since 2.9.3
     */
    @IntRange(from = 0, to = 64) float maxCreativeReach();

    /**
     * The margin applied to the target hitbox when attacking. Defaults to 0.3.
     *
     * @return the margin applied to the target hitbox when attacking
     * @since 2.9.3
     */
    @IntRange(from = 0, to = 1) float hitboxMargin();

    /**
     * Creates a builder for the attack range component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    /**
     * Builder for the attack range component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaAttackRange> {

        /**
         * Sets the minimum distance for attacks, in blocks.
         *
         * @param minReach the minimum distance for attacks, in blocks
         * @see JavaAttackRange#minReach()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder minReach(@IntRange(from = 0, to = 64) float minReach);

        /**
         * Sets the maximum distance for attacks, in blocks.
         *
         * @param maxReach the maximum distance for attacks, in blocks
         * @see JavaAttackRange#maxReach()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder maxReach(@IntRange(from = 0, to = 64) float maxReach);

        /**
         * Sets the minimum distance for attacks made in creative mode, in blocks.
         *
         * @param minCreativeReach the minimum distance for attacks made in creative mode, in blocks
         * @see JavaAttackRange#minCreativeReach()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder minCreativeReach(@IntRange(from = 0, to = 64) float minCreativeReach);

        /**
         * Sets the maximum distance for attacks made in creative mode, in blocks.
         *
         * @param maxCreativeReach the maximum distance for attacks made in creative mode, in blocks
         * @see JavaAttackRange#maxCreativeReach()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder maxCreativeReach(@IntRange(from = 0, to = 64) float maxCreativeReach);

        /**
         * Sets the margin applied to the target hitbox when attacking.
         *
         * @param hitboxMargin the margin applied to the target hitbox when attacking
         * @see JavaAttackRange#hitboxMargin()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder hitboxMargin(@IntRange(from = 0, to = 1) float hitboxMargin);

        /**
         * Creates the attack range component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaAttackRange build();
    }
}
