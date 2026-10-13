/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.item.custom.v2;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.jspecify.annotations.Nullable;

/**
 * Record-like interface grouping a custom item's icons on Bedrock edition. As of right now, this only consists of the item's default icon,
 * and optionally, the item's dyed icon.
 *
 * @since 2.12.0
 */
public interface CustomItemIcons {

    /**
     * Implementation of {@link CustomItemIcons} that always returns {@code null} for all icons.
     *
     * @since 2.12.0
     */
    CustomItemIcons EMPTY = new CustomItemIcons() {
        @Override
        public @Nullable String defaultIcon() {
            return null;
        }

        @Override
        public @Nullable String dyedIcon() {
            return null;
        }
    };

    /**
     * Gets the item's default icon, if it was manually set.
     *
     * @return the item's default icon
     * @since 2.12.0
     */
    @Nullable String defaultIcon();

    /**
     * Gets the item's dyed icon, if it was manually set.
     *
     * @return the item's dyed icon
     * @since 2.12.0
     */
    @Nullable String dyedIcon();

    /**
     * Creates a new builder for a {@link CustomItemIcons} instance.
     *
     * @return a new builder
     * @since 2.12.0
     */
    static CustomItemIcons.Builder builder() {
        return GeyserApi.api().provider(CustomItemIcons.Builder.class);
    }

    /**
     * Builder for {@link CustomItemIcons}.
     *
     * @since 2.12.0
     */
    interface Builder {

        /**
         * Sets the item's default icon.
         *
         * @param icon the item's default icon
         * @see CustomItemIcons#defaultIcon()
         * @return this builder
         * @since 2.12.0
         */
        @This
        Builder defaultIcon(@Nullable String icon);

        /**
         * Sets the item's dyed icon.
         *
         * @param dyedIcon the item's dyed icon
         * @see CustomItemIcons#dyedIcon()
         * @return this builder
         * @since 2.12.0
         */
        @This
        Builder dyedIcon(@Nullable String dyedIcon);

        /**
         * Creates the {@link CustomItemIcons} instance.
         *
         * @return the new {@link CustomItemIcons} instance
         * @since 2.12.0
         */
        CustomItemIcons build();
    }
}
