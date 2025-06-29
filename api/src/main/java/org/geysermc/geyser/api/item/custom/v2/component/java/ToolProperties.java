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

import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

/**
 * The tool properties component can be used to mark
 * if the item can destroy blocks when used in creative mode.
 */
public interface ToolProperties {

    /**
     * Whether this item can destroy blocks when trying to break them in
     * creative mode. Defaults to {@code true}.
     *
     * @return whether this item can destroy blocks in creative mode
     */
    boolean canDestroyBlocksInCreative();

    /**
     * Creates a builder for the tool properties component.
     *
     * @return a new builder
     */
    static Builder builder() {
        return GeyserApi.api().provider(ToolProperties.Builder.class);
    }

    /**
     * Creates a tool properties component.
     *
     * @param canDestroyBlocksInCreative determines if the item will break blocks in creative mode
     * @return a tool properties component
     */
    static ToolProperties of(boolean canDestroyBlocksInCreative) {
        return builder().canDestroyBlocksInCreative(canDestroyBlocksInCreative).build();
    }

    /**
     * Builder for the tool properties component.
     */
    interface Builder extends GenericBuilder<ToolProperties> {

        /**
         * Sets whether this item can destroy blocks when trying to break them in
         * creative mode.
         *
         * @param canDestroyBlocksInCreative determines if the item will break blocks in creative mode
         * @see ToolProperties#canDestroyBlocksInCreative()
         * @return this builder
         */
        @This
        Builder canDestroyBlocksInCreative(boolean canDestroyBlocksInCreative);

        /**
         * Creates the tool properties component.
         *
         * @return the new component
         */
        @Override
        ToolProperties build();
    }
}
