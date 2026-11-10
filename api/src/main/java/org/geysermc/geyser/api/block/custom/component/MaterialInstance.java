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

/**
 * This class is used to store data for a material instance.
 */
public interface MaterialInstance {

    /**
     * Gets the texture of the block.
     *
     * @return the texture of the block
     * @since 2.2.0
     */
    @Nullable String texture();

    /**
     * Gets the render method of the block.
     *
     * @return the render method of the block
     * @since 2.2.0
     */
    @Nullable String renderMethod();

    /**
     * Gets the tint method of the block.
     *
     * @return the tint method of the block
     * @since 2.9.1
     */
    @Nullable String tintMethod();

    /**
     * Gets if the block should be dimmed on certain faces.
     *
     * @return if the block should be dimmed on certain faces
     * @since 2.2.0
     */
    boolean faceDimming();

    /**
     * Gets if the block should have ambient occlusion.
     *
     * <p>This method is deprecated for removal and will be removed in Geyser 2.13.0.</p>
     *
     * @return if the block should have ambient occlusion
     * @since 2.2.0
     * @deprecated since 2.11.3: use {@link #ambientOcclusionExponent()} instead
     */
    @Deprecated(since = "2.11.3", forRemoval = true)
    boolean ambientOcclusion();

    /**
     * Gets the exponent applied to the ambient occlusion value after lighting.
     * Bedrock accepts values between {@code 0.0} and {@code 10.0}, where {@code 0.0}
     * disables ambient occlusion and {@code 1.0} is the default strength.
     *
     * @return the ambient occlusion exponent
     * @since 2.11.3
     */
    float ambientOcclusionExponent();

    /**
     * Gets if the block is isotropic, meaning the UVs of its faces are randomly rotated based on the block's position.
     *
     * @return if the block is isotropic
     * @since 2.9.1
     */
    boolean isotropic();

    /**
     * Gets if the block's texture can use texture variations.
     *
     * @return if the block's texture can use texture variations
     * @since 2.12.2
     */
    boolean textureVariation();

    /**
     * Gets if the block's tint is masked by its texture's alpha.
     *
     * @return if the block's tint is masked by its texture's alpha
     * @since 2.12.2
     */
    boolean alphaMaskedTint();

    /**
     * Creates a builder for MaterialInstance.
     *
     * @return a builder for MaterialInstance
     */
    static MaterialInstance.Builder builder() {
        return GeyserApi.api().provider(MaterialInstance.Builder.class);
    }

    /**
     * A builder for MaterialInstance
     *
     * @since 2.2.0
     */
    interface Builder {
        /**
         * Sets the texture of the block.
         * @see MaterialInstance#texture()
         *
         * @param texture the texture of the block
         * @return this builder
         * @since 2.2.0
         */
        @This
        Builder texture(@Nullable String texture);

        /**
         * Sets the render method of the block.
         * @see MaterialInstance#renderMethod()
         *
         * @param renderMethod the render method of the block
         * @return this builder
         * @since 2.2.0
         */
        @This
        Builder renderMethod(@Nullable String renderMethod);

        /**
         * Sets the tint method of the block.
         * @see MaterialInstance#tintMethod()
         *
         * @param tintMethod the tint method of the block
         * @return this builder
         * @since 2.9.1
         */
        @This
        Builder tintMethod(@Nullable String tintMethod);

        /**
         * Sets if the block should be dimmed on certain faces.
         * @see MaterialInstance#faceDimming()
         *
         * @param faceDimming if the block should be dimmed on certain faces
         * @return this builder
         * @since 2.2.0
         */
        @This
        Builder faceDimming(boolean faceDimming);

        /**
         * Sets if the block should have ambient occlusion.
         *
         * <p>This method is deprecated for removal and will be removed in Geyser 2.13.0.</p>
         *
         * @param ambientOcclusion if the block should have ambient occlusion
         * @return this builder
         * @since 2.2.0
         * @deprecated since 2.11.3: Use {@link #ambientOcclusionExponent(float)} instead
         */
        @This
        @Deprecated(since = "2.11.3", forRemoval = true)
        Builder ambientOcclusion(boolean ambientOcclusion);

        /**
         * Sets the exponent applied to the ambient occlusion value after lighting,
         * between {@code 0.0} and {@code 10.0} inclusive.
         * @see MaterialInstance#ambientOcclusionExponent()
         *
         * @param ambientOcclusionExponent the ambient occlusion exponent
         * @return this builder
         * @since 2.11.3
         */
        @This
        Builder ambientOcclusionExponent(float ambientOcclusionExponent);

        /**
         * Sets if the block is isotropic, meaning the UVs of its faces are randomly rotated based on the block's position.
         *
         * @see MaterialInstance#isotropic()
         * @param isotropic if the block is isotropic
         * @return this builder
         * @since 2.9.1
         */
        @This
        Builder isotropic(boolean isotropic);

        /**
         * Sets if the block's texture can use texture variations.
         *
         * @see MaterialInstance#textureVariation()         *
         * @param textureVariation if the block's texture can use texture variations
         * @return this builder
         * @since 2.12.2
         */
        @This
        Builder textureVariation(boolean textureVariation);

        /**
         * Sets if the block's tint is masked by its texture's alpha.
         *
         * @see MaterialInstance#alphaMaskedTint()
         * @param alphaMaskedTint if the block's tint is masked by its texture's alpha
         * @return this builder
         * @since 2.12.2
         */
        @This
        Builder alphaMaskedTint(boolean alphaMaskedTint);

        /**
         * Creates the MaterialInstance.
         *
         * @return the MaterialInstance
         * @since 2.2.0
         */
        MaterialInstance build();
    }
}
