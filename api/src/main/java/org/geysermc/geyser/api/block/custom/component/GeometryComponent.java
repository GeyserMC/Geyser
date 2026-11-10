/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.block.custom.component;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * This class is used to store data for a geometry component.
 */
public interface GeometryComponent {

    /**
     * Gets the identifier of the geometry.
     *
     * @return the identifier of the geometry
     * @since 2.2.0
     */
    String identifier();

    /**
     * Gets the bone visibility of the geometry.
     *
     * @return the bone visibility of the geometry
     * @since 2.2.0
     */
    @Nullable Map<String, String> boneVisibility();

    /**
     * Gets the identifier of the block culling rules, defined in a resource pack's {@code block_culling} folder.
     *
     * @return the identifier of the block culling rules
     * @since 2.12.2
     */
    @Nullable String culling();

    /**
     * Gets the culling layer, used by block culling rules with the {@code same_culling_layer} condition.
     *
     * @return the culling layer
     * @since 2.12.2
     */
    @Nullable String cullingLayer();

    /**
     * Creates a builder for GeometryComponent.
     *
     * @return a builder for GeometryComponent
     */
    static GeometryComponent.Builder builder() {
        return GeyserApi.api().provider(GeometryComponent.Builder.class);
    }

    /**
     * A builder for GeometryComponent
     *
     * @since 2.2.0
     */
    interface Builder {
        /**
         * Sets the identifier of the geometry.
         *
         * @see GeometryComponent#identifier()
         * @param identifier the identifier of the geometry
         * @return this builder
         * @since 2.2.0
         */
        @This
        Builder identifier(String identifier);

        /**
         * Sets the bone visibility of the geometry.
         *
         * @see GeometryComponent#boneVisibility()
         * @param boneVisibility the bone visibility of the geometry
         * @return this builder
         * @since 2.2.0
         */
        @This
        Builder boneVisibility(@Nullable Map<String, String> boneVisibility);

        /**
         * Sets the identifier of the block culling rules, defined in a resource pack's {@code block_culling} folder.
         *
         * @see GeometryComponent#culling()
         * @param culling the identifier of the block culling rules
         * @return this builder
         * @since 2.12.2
         */
        @This
        Builder culling(@Nullable String culling);

        /**
         * Sets the culling layer, used by block culling rules with the {@code same_culling_layer} condition.
         *
         * @see GeometryComponent#cullingLayer()
         * @param cullingLayer the culling layer
         * @return this builder
         * @since 2.12.2
         */
        @This
        Builder cullingLayer(@Nullable String cullingLayer);

        /**
         * Creates the GeometryComponent.
         *
         * @return the GeometryComponent
         * @since 2.2.0
         */
        GeometryComponent build();
    }
}
