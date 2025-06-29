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
import org.geysermc.geyser.api.util.Identifier;

import java.util.Arrays;
import java.util.List;

/**
 * The repairable component determines which other items can be used
 * to repair the item.
 */
public interface Repairable {

    /**
     * The Bedrock identifiers of the items
     * that can be used to repair this item.
     *
     * @return the identifiers
     */
    List<@NonNull Identifier> items();

    /**
     * Creates a builder for the repairable component.
     *
     * @return a new builder
     */
    static Builder builder() {
        return GeyserApi.api().provider(Repairable.Builder.class);
    }

    /**
     * Creates a repairable component.
     *
     * @param items the identifiers of the items that
     *      can repair the item
     * @return the repairable component
     */
    static Repairable of(Identifier... items) {
        Repairable.Builder builder = builder();
        Arrays.stream(items).forEach(builder::item);
        return builder.build();
    }

    /**
     * Builder for the repairable component.
     */
    interface Builder extends GenericBuilder<Repairable> {

        /**
         * Adds an item that can be used to repair the item.
         * This will throw when trying to add an item that was already added.
         *
         * @param item the Bedrock item identifier that can be used to repair the item
         * @see Repairable#items()
         * @return this builder
         */
        @This
        Builder item(@NonNull Identifier item);

        /**
         * Creates the repairable component.
         *
         * @return the new component
         */
        @Override
        Repairable build();
    }
}
