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

package org.geysermc.geyser.api.item.custom.v2.component.geyser;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Identifier;

/**
 * Allows modifying items so these can place blocks or take on the
 * icon of the block they place.
 *
 * @since 2.9.3
 */
public interface GeyserBlockPlacer {

    /**
     * The block placed by the item, used by the
     * Bedrock client to predict block placing.
     * This is a Bedrock edition block identifier.
     *
     * @return the identifier of the block to place
     * @since 2.9.3
     */
    @NonNull Identifier block();

    /**
     * Whether to use the block's rendering
     * as the icon for the item. Defaults to {@code false}.
     *
     * @return whether to use the 3d block rendering for the
     *      item icon
     * @since 2.9.3
     */
    boolean useBlockIcon();

    /**
     * Creates a builder for the block placer component.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(GeyserBlockPlacer.Builder.class);
    }

    /**
     * Creates a block placer component.
     *
     * @param block the identifier of the block to place
     * @param useBlockIcon whether to use the 3d block rendering for the item icon
     * @return the block placer component
     * @since 2.9.3
     */
    static @NonNull GeyserBlockPlacer of(@NonNull Identifier block, boolean useBlockIcon) {
        return GeyserBlockPlacer.builder().block(block).useBlockIcon(useBlockIcon).build();
    }

    /**
     * Builder for the block placer component.
     * @since 2.9.3
     */
    interface Builder extends GenericBuilder<GeyserBlockPlacer> {

        /**
         * The identifier of the block to place.
         * This should be the block identifier as it is
         * known to the Bedrock client.
         *
         * @param block the identifier of the block
         * @see GeyserBlockPlacer#block()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder block(@NonNull Identifier block);

        /**
         * Whether to use the block's icon over the item icon.
         * Block items have a 3d-generated block icon.
         *
         * @param useBlockIcon whether to use the block icon
         * @see GeyserBlockPlacer#useBlockIcon()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder useBlockIcon(boolean useBlockIcon);

        /**
         * Creates the block placer component.
         *
         * @return the new component
         * @since 2.9.3
         */
        @Override
        GeyserBlockPlacer build();
    }
}
