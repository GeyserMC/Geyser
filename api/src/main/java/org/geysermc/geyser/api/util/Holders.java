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

package org.geysermc.geyser.api.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Similar to the {@code HolderSet}s in Minecraft, a Holders object can represent either a list of identifiers, or an identifier of a Minecraft registry tag.
 * What these identifiers represent depends on the context in which Holders are used.
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface Holders {

    /**
     * Creates a Holders object consisting of a single identifier.
     *
     * @param identifier the identifier the Holders object consists of
     * @return a new Holders object
     * @since 2.9.3
     */
    static @NonNull Holders of(Identifier identifier) {
        return builder().with(identifier).build();
    }

    /**
     * Creates a Holders object consisting of a list of identifiers.
     *
     * @param identifiers the identifiers the Holders object consists of
     * @return a new Holders object
     * @since 2.9.3
     */
    static @NonNull Holders of(List<Identifier> identifiers) {
        Builder builder = builder();
        identifiers.forEach(builder::with);
        return builder.build();
    }

    /**
     * Creates a Holders object consisting of a tag
     *
     * @param tag the tag the Holders object consists of
     * @return a new Holders object
     * @since 2.9.3
     */
    static @NonNull Holders ofTag(Identifier tag) {
        return builder().tag(tag).build();
    }

    /**
     * Creates a builder for a Holders object.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(Holders.Builder.class);
    }

    /**
     * Builder for the Holders object
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<Holders> {

        /**
         * Adds a new identifier to the Holders object. This will throw when a tag has been set, since a Holders object can
         * consist of either a tag, or a list of identifiers, not both.
         *
         * @param identifier the identifier to add to the Holders object
         * @throws IllegalArgumentException when a tag has been set
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder with(@NonNull Identifier identifier);

        /**
         * Sets the tag of the Holders object. A Holders object can only consist of one tag. This will throw when at least one identifier has been
         * added, since a Holders object can consist of either a tag, or a list of identifiers, not both.
         *
         * @param tag the tag to set
         * @throws IllegalArgumentException when at least one identifier has already been added
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder tag(@NonNull Identifier tag);

        /**
         * Creates the Holders object.
         *
         * @return the new Holders object
         * @since 2.9.3
         */
        @Override
        Holders build();
    }
}
