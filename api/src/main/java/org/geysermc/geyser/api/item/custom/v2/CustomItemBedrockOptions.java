/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;

import java.util.Set;

/**
 * This is used to store options for a custom item definition that can't be described using item components.
 * @since 2.9.3
 */
public interface CustomItemBedrockOptions {

    /**
     * Gets the item's icon. When not present, the item's Bedrock identifier is used.
     *
     * @return the item's icon
     * @see CustomItemDefinition#icon()
     * @since 2.9.3
     */
    @Nullable
    String icon();

    /**
     * If the item is allowed to be put into the offhand. Defaults to true.
     *
     * @return true if the item is allowed to be used in the offhand, false otherwise
     * @since 2.9.3
     */
    boolean allowOffhand();

    /**
     * If the item should be displayed as handheld, like a tool. Defaults to false.
     *
     * @return true if the item should be displayed as handheld, false otherwise
     * @since 2.9.3
     */
    boolean displayHandheld();

    /**
     * Since Bedrock doesn't properly support setting item armor values over attributes, this value
     * determines how many armor points should be shown when this item is worn. This is purely visual.
     *
     * <p>Only has an effect when the item is equippable, and defaults to 0.</p>
     *
     * @return the item's visually shown protection value
     * @since 2.9.3
     */
    int protectionValue();

    /**
     * The item's creative category. Defaults to {@code NONE}.
     *
     * @return the item's creative category
     * @since 2.9.3
     */
    @NonNull
    CreativeCategory creativeCategory();

    /**
     * The item's creative group.
     *
     * <p>A list of creative groups available in vanilla can be found <a href="https://wiki.bedrock.dev/documentation/menu-categories#list-of-vanilla-groups">here</a>.</p>
     *
     * @return the item's creative group
     * @since 2.9.3
     */
    @Nullable
    String creativeGroup();

    /**
     * Gets the item's set of bedrock tags that can be used in Molang.
     * Equivalent to "tag:some_tag"
     *
     * @return the item's set of bedrock tags, can be empty
     * @since 2.9.3
     */
    @NonNull
    Set<Identifier> tags();

    /**
     * Creates a new builder for custom item bedrock options.
     *
     * @return a new builder
     * @since 2.9.3
     */
    static Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    /**
     * Builder for custom item bedrock options.
     * @since 2.9.3
     */
    interface Builder {

        /**
         * Sets the item's icon.
         *
         * @param icon the item's icon
         * @see CustomItemBedrockOptions#icon()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder icon(@Nullable String icon);

        /**
         * Sets if the item is allowed to be put into the offhand.
         *
         * @param allowOffhand if the item is allowed to be put into the offhand slot
         * @see CustomItemBedrockOptions#allowOffhand()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder allowOffhand(boolean allowOffhand);

        /**
         * Sets if the item should be displayed as handheld, like a tool.
         *
         * @param displayHandheld if the item should be displayed as handheld
         * @see CustomItemBedrockOptions#displayHandheld()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder displayHandheld(boolean displayHandheld);

        /**
         * Sets the item's protection value.
         *
         * @param protectionValue the item's protection value
         * @see CustomItemBedrockOptions#protectionValue()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder protectionValue(int protectionValue);

        /**
         * Sets the item's creative category.
         *
         * @param creativeCategory the item's creative category
         * @see CustomItemBedrockOptions#creativeCategory()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder creativeCategory(@NonNull CreativeCategory creativeCategory);

        /**
         * Sets the item's creative group.
         *
         * @param creativeGroup the item's creative group
         * @see CustomItemBedrockOptions#creativeGroup()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder creativeGroup(@Nullable String creativeGroup);

        /**
         * Adds a tag to the set of bedrock tags this item has, for use in Molang.
         *
         * @param tag the tag to be added
         * @see CustomItemBedrockOptions#tags()
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder tag(@NonNull Identifier tag);

        /**
         * Sets the item's set of bedrock tags, for use in Molang. Pass {@code null} to clear all tags.
         *
         * @param tags the tags to be set, or {@code null} to clear all tags
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder tags(@Nullable Set<Identifier> tags);

        /**
         * Creates the custom item bedrock options.
         *
         * @return the new instance of bedrock options
         * @since 2.9.3
         */
        CustomItemBedrockOptions build();
    }
}
