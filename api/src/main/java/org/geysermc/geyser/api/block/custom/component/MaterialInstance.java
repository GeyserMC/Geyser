/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;

/**
 * This class is used to store data for a material instance.
 */
public interface MaterialInstance {
    /**
     * Gets the texture of the block
     *
     * @return The texture of the block.
     */
    @Nullable String texture();

    /**
     * Gets the render method of the block
     *
     * @return The render method of the block.
     */
    @Nullable String renderMethod();

    /**
     * Gets if the block should be dimmed on certain faces
     * 
     * @return If the block should be dimmed on certain faces.
     */
    boolean faceDimming();

    /**
     * Gets if the block should have ambient occlusion
     * 
     * @return If the block should have ambient occlusion.
     */
    boolean ambientOcclusion();

    /**
     * Creates a builder for MaterialInstance.
     *
     * @return a builder for MaterialInstance
     */
    static MaterialInstance.Builder builder() {
        return GeyserApi.api().provider(MaterialInstance.Builder.class);
    }

    interface Builder {
        Builder texture(@Nullable String texture);

        Builder renderMethod(@Nullable String renderMethod);

        Builder faceDimming(boolean faceDimming);

        Builder ambientOcclusion(boolean ambientOcclusion);

        MaterialInstance build();
    }
}
