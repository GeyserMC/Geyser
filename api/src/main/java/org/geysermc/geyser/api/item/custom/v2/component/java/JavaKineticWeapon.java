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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

/**
 * The kinetic weapon component is used to specify a spear-like attack when the item is in use.
 * @since 2.9.3
 */
public interface JavaKineticWeapon {

    /**
     * The minimum use time, in ticks, required for the weapon to be active. Defaults to 0.
     *
     * @return the minimum use time, in ticks, required for the weapon to be active
     * @since 2.9.3
     */
    @NonNegative int delayTicks();

    /**
     * The condition that has to meet for the attacker to dismount the target.
     *
     * @return the condition to dismount the target
     * @since 2.9.3
     */
    @Nullable Condition dismountConditions();

    /**
     * Creates a builder for the kinetic weapon component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    /**
     * Creates a new {@link Condition}.
     *
     * @param maxDurationTicks the time in ticks after which the condition is no longer checked
     * @see Condition
     * @see Condition#maxDurationTicks()
     * @return the new {@link Condition}
     * @since 2.9.3
     */
    static @NonNull Condition condition(@NonNegative int maxDurationTicks) {
        return condition(maxDurationTicks, 0.0F, 0.0F);
    }

    /**
     * Creates a new {@link Condition}.
     *
     * @param maxDurationTicks the time in ticks after which the condition is no longer checked
     * @param minSpeed the minimum speed of the attacker, in blocks per second
     * @param minRelativeSpeed the minimum relative speed between the attacker and the target, in blocks per second
     * @see Condition
     * @see Condition#maxDurationTicks()
     * @see Condition#minSpeed()
     * @see Condition#minRelativeSpeed()
     * @return the new {@link Condition}
     * @since 2.9.3
     */
    static @NonNull Condition condition(@NonNegative int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
        return Condition.builder(maxDurationTicks)
            .minSpeed(minSpeed)
            .minRelativeSpeed(minRelativeSpeed)
            .build();
    }

    /**
     * Builder for the kinetic weapon component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaKineticWeapon> {

        /**
         * Sets the minimum use time, in ticks, required for the weapon to be active.
         *
         * @param delayTicks the minimum use time, in ticks, required for the weapon to be active
         * @see JavaKineticWeapon#delayTicks()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder delayTicks(@NonNegative int delayTicks);

        /**
         * Shorthand for {@link Builder#dismountConditions(Condition)}.
         * @since 2.9.3
         */
        @This
        default Builder dismountConditions(Condition.@NonNull Builder dismountConditions) {
            return dismountConditions(dismountConditions.build());
        }

        /**
         * Sets the condition to dismount the target.
         *
         * @param dismountConditions the condition to dismount the target
         * @see JavaKineticWeapon#dismountConditions()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder dismountConditions(@Nullable Condition dismountConditions);

        /**
         * Creates the kinetic weapon component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaKineticWeapon build();
    }

    /**
     * A condition used during the attack of a {@link JavaKineticWeapon}.
     * @since 2.9.3
     */
    interface Condition {

        /**
         * The time in ticks after which the condition is no longer checked (and thus always fails), starting once {@link JavaKineticWeapon#delayTicks()} has passed.
         *
         * @return the time in ticks after which the condition is no longer checked
         * @since 2.9.3
         */
        @NonNegative int maxDurationTicks();

        /**
         * The minimum speed of the attacker, in blocks per second, required for the condition to pass. Defaults to 0.
         *
         * @return the minimum speed of the attacker, in blocks per second
         * @since 2.9.3
         */
        float minSpeed();

        /**
         * The minimum relative speed between the attacker and the target, in blocks per second, required for the condition to pass. Defaults to 0.
         *
         * @return the minimum relative speed between the attacker and the target, in blocks per second
         * @since 2.9.3
         */
        float minRelativeSpeed();

        /**
         * Creates a builder for a {@link Condition}.
         *
         * @param maxDurationTicks the time in ticks after which the condition is no longer checked
         * @see Condition
         * @see Condition#maxDurationTicks()
         * @return a new builder
         * @since 2.9.3
         */
        static @NonNull Builder builder(@NonNegative int maxDurationTicks) {
            return GeyserApi.api().provider(Builder.class, maxDurationTicks);
        }

        /**
         * Builder for a {@link Condition}.
         * @since 2.9.3
         */
        interface Builder extends GenericBuilder<Condition> {

            /**
             * Sets the minimum speed of the attacker, in blocks per second.
             *
             * @param minSpeed the minimum speed of the attacker, in blocks per second
             * @see Condition#minSpeed()
             * @return this builder
             * @since 2.9.3
             */
            @This
            Builder minSpeed(float minSpeed);

            /**
             * Sets the minimum relative speed between the attacker and the target, in blocks per second.
             *
             * @param minRelativeSpeed the minimum relative speed between the attacker and the target, in blocks per second
             * @see Condition#minRelativeSpeed()
             * @return this builder
             * @since 2.9.3
             */
            @This
            Builder minRelativeSpeed(float minRelativeSpeed);

            /**
             * Creates the {@link Condition}.
             *
             * @return the new {@link Condition}
             * @since 2.9.3
             */
            @Override
            Condition build();
        }
    }
}
