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
import org.geysermc.geyser.api.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Record-like interface grouping a custom item's icons on Bedrock edition. As of right now, this only consists of the item's default icon,
 * and optionally, the item's dyed icon.
 */
public interface CustomItemIcons {

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
     * Gets the item's default icon.
     *
     * @return the item's default icon
     * @since 2.12.0
     */
    @Nullable String defaultIcon();

    /**
     * Gets the item's dyed icon.
     *
     * @return the item's dyed icon
     * @since 2.12.0
     */
    @Nullable String dyedIcon();

    /**
     * Resolves this instance's icons for the given item's Bedrock identifier.
     *
     * <p>If no default icon is set, then the Bedrock identifier is used,
     * the namespace separator ({@code :}) replaced with {@code .} and the path separators ({@code /}) replaced with {@code _}. For example:</p>
     *
     * <p>{@code my_datapack:my_custom_item} => {@code my_datapack.my_custom_item}</p>
     * <p>{@code my_datapack:cool_items/cool_item_1} => {@code my_datapack.cool_items_cool_item_1}</p>
     *
     * <p>If no dyed icon is set, the computed default icon is used.</p>
     *
     * @param bedrockIdentifier the item's Bedrock identifier
     * @return this instance's icons, resolved for the given item's Bedrock identifier
     */
    default Resolved resolve(Identifier bedrockIdentifier) {
        String defaultIcon = defaultIcon() == null ? bedrockIdentifier.toString().replace(":", ".").replace("/", "_") : defaultIcon();
        String dyedIcon = dyedIcon() == null ? defaultIcon : dyedIcon();

        // For IDEA warnings
        assert defaultIcon != null;
        assert dyedIcon != null;
        return new Resolved() {
            @Override
            public String defaultIcon() {
                return defaultIcon;
            }

            @Override
            public String dyedIcon() {
                return dyedIcon;
            }
        };
    }

    /**
     * Interface extending {@link CustomItemIcons} and marking that the icons are <em>resolved</em> using the item's Bedrock identifier:
     * this indicates that {@link CustomItemIcons#defaultIcon()} and {@link CustomItemIcons#dyedIcon()} will never return null.
     */
    interface Resolved extends CustomItemIcons {

        /**
         * {@inheritDoc}
         */
        @Override
        String defaultIcon();

        /**
         * {@inheritDoc}
         */
        @Override
        String dyedIcon();

        /**
         * @throws UnsupportedOperationException always, as you cannot resolve an already resolved {@link CustomItemIcons} instance
         */
        @Override
        default Resolved resolve(Identifier bedrockIdentifier) {
            throw new UnsupportedOperationException("Cannot resolve an already resolved CustomItemIcons instance");
        }
    }

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
     * @since 2.9.3
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
