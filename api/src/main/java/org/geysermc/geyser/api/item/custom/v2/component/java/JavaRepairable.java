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
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Holders;

/**
 * The repairable component determines which other items can be used
 * to repair the item.
 * @since 2.9.3
 */
public interface JavaRepairable {

    /**
     * The {@link Holders} of item identifiers that can be used to repair the item.
     *
     * @return the {@link Holders} of item identifiers
     * @since 2.9.3
     */
    @NonNull Holders items();

    /**
     * Creates a builder for the repairable component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(JavaRepairable.Builder.class);
    }

    /**
     * Creates a repairable component.
     *
     * @param items the {@link Holders} of the items that
     *      can repair the item
     * @return the repairable component
     * @since 2.9.3
     */
    static @NonNull JavaRepairable of(@NonNull Holders items) {
        return JavaRepairable.builder().items(items).build();
    }

    /**
     * Builder for the repairable component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<JavaRepairable> {

        /**
         * Sets the {@link Holders} of item identifiers that can be used to repair the item.
         *
         * @param items the {@link Holders} of item identifiers that can be used to repair the item
         * @see JavaRepairable#items()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder items(@NonNull Holders items);

        /**
         * Creates the repairable component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        JavaRepairable build();
    }
}
