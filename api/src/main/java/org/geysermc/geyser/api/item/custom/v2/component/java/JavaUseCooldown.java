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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Identifier;

/**
 * The use cooldown component is used to add an item use cooldown to items.
 * @since 2.9.3
 */
public interface JavaUseCooldown {

    /**
     * The duration of time in seconds items with a matching category will
     * spend cooling down before being usable again.
     *
     * @return the cooldown duration
     * @since 2.9.3
     */
    @Positive float seconds();

    /**
     * The cooldown type of the item. Other items in this group
     * will also have the cooldown applied when any item of this
     * group is used. {@code null} will result in the item identifier
     * being used.
     *
     * @return the cooldown identifier
     * @since 2.9.3
     */
    @Nullable Identifier cooldownGroup();

    /**
     * Creates a builder for the use cooldown component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(JavaUseCooldown.Builder.class);
    }

    /**
     * Builder for the use cooldown component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaUseCooldown> {

        /**
         * Sets the duration in seconds in which the item
         * cannot be used again.
         *
         * @param seconds the cooldown time
         * @see JavaUseCooldown#seconds()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder seconds(@Positive float seconds);

        /**
         * Sets the cooldown group that this cooldown belongs to.
         * When any item in this group is used, all items in the group
         * are not usable for the amount of time specified in {@link Builder#seconds()}
         * {@code null} results in the item identifier being specified instead.
         *
         * @param cooldownGroup the cooldown group identifier
         * @see JavaUseCooldown#cooldownGroup()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder cooldownGroup(@Nullable Identifier cooldownGroup);

        /**
         * Creates the use cooldown component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaUseCooldown build();
    }
}
