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
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.util.CreativeCategory;

import java.util.Set;

/**
 * This is used to store options for a custom item defintion that can't be described using item components.
 */
public interface CustomItemBedrockOptions {

    /**
     * Gets the item's icon. When not present, the item's Bedrock identifier is used.
     *
     * @return the item's icon
     * @see CustomItemDefinition#icon()
     */
    @Nullable
    String icon();

    /**
     * If the item is allowed to be put into the offhand. Defaults to true.
     *
     * @return true if the item is allowed to be used in the offhand, false otherwise
     */
    boolean allowOffhand();

    /**
     * If the item should be displayed as handheld, like a tool.
     *
     * @return true if the item should be displayed as handheld, false otherwise
     */
    boolean displayHandheld();

    /**
     * The item's creative category. Defaults to {@code NONE}.
     *
     * @return the item's creative category
     */
    @NonNull
    CreativeCategory creativeCategory();

    /**
     * Gets the item's creative group.
     *
     * @return the item's creative group
     */
    @Nullable
    String creativeGroup();

    /**
     * Gets the item's texture size. This is to resize the item if the texture is not 16x16.
     *
     * @deprecated resizing is done with render offsets, which are deprecated in favour of attachables.
     * @return the item's texture size
     */
    @Deprecated
    int textureSize();

    /**
     * Gets the item's render offsets. If it is null, the item will be rendered normally, with no offsets.
     *
     * @deprecated attachables are now preferred instead of using render offsets.
     * @return the item's render offsets
     */
    @Nullable
    @Deprecated
    CustomRenderOffsets renderOffsets();

    /**
     * Gets the item's set of tags that can be used in Molang.
     * Equivalent to "tag:some_tag"
     *
     * @return the item's tags, if they exist
     */
    @NonNull
    Set<String> tags();

    static Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    interface Builder {

        Builder icon(@Nullable String icon);

        Builder allowOffhand(boolean allowOffhand);

        Builder displayHandheld(boolean displayHandheld);

        Builder creativeCategory(CreativeCategory creativeCategory);

        Builder creativeGroup(@Nullable String creativeGroup);

        @Deprecated
        Builder textureSize(int textureSize);

        @Deprecated
        Builder renderOffsets(@Nullable CustomRenderOffsets renderOffsets);

        Builder tags(@Nullable Set<String> tags);

        CustomItemBedrockOptions build();
    }
}
