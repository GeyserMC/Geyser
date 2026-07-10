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

package org.geysermc.geyser.api.entity.data.types;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents an entity hitbox, with min/max representing absolute coordinates.
 *
 * @since 2.11.0
 */
@ApiStatus.Experimental
public interface Hitbox {

    /**
     * The min "corner" of the hitbox as a position in the world.
     *
     * @return the vector of the corner
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Vector3f min();

    /**
     * The max "corner" of the hitbox as a position in the world.
     *
     * @return the vector of the corner
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Vector3f max();

    /**
     * The pivot of the hitbox
     *
     * @return the pivot
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Vector3f pivot();

    /**
     * Creates a new builder for a hitbox.
     *
     * @return a new builder
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    static Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    /**
     * The builder for an entity hitbox.
     *
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    interface Builder {

        /**
         * Sets the min corner of the hitbox
         *
         * @param min the vector of the corner
         * @return this builder
         * @since 2.11.0
         */
        @ApiStatus.Experimental
        @This Builder min(Vector3f min);

        /**
         * Sets the max corner of the hitbox.
         *
         * @param max the vector of the corner
         * @return this builder
         * @since 2.11.0
         */
        @ApiStatus.Experimental
        @This Builder max(Vector3f max);

        /**
         * Sets the pivot of the hitbox.
         *
         * @param pivot the pivot vector
         * @return this builder
         * @since 2.11.0
         */
        @ApiStatus.Experimental
        @This Builder pivot(Vector3f pivot);

        /**
         * Builds this hitbox, defaulting to {@code Vector3f.ZERO} if
         * any one vector was not provided.
         *
         * @return a new hitbox
         * @since 2.11.0
         */
        @ApiStatus.Experimental
        Hitbox build();
    }
}
