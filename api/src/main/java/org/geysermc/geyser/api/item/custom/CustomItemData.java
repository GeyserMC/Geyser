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

package org.geysermc.geyser.api.item.custom;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;

import java.util.OptionalInt;
import java.util.Set;

/**
 * This is used to store data for a custom item.
 */
public interface CustomItemData {
    /**
     * Gets the item's name.
     *
     * @return the item's name
     */
    @NonNull String name();

    /**
     * Gets the custom item options of the item.
     *
     * @return the custom item options of the item.
     */
    CustomItemOptions customItemOptions();

    /**
     * Gets the item's display name. By default, this is the item's name.
     *
     * @return the item's display name
     */
    @NonNull String displayName();

    /**
     * Gets the item's icon. By default, this is the item's name.
     *
     * @return the item's icon
     */
    @NonNull String icon();

    /**
     * Gets if the item is allowed to be put into the offhand.
     *
     * @return true if the item is allowed to be used in the offhand, false otherwise
     */
    boolean allowOffhand();

    /**
     * Gets if the item should be displayed as handheld, like a tool.
     *
     * @return true if the item should be displayed as handheld, false otherwise
     */
    boolean displayHandheld();

    /**
     * Gets the item's creative category, or tab id.
     *
     * @return the item's creative category
     */
    @NonNull OptionalInt creativeCategory();

    /**
     * Gets the item's creative group.
     *
     * @return the item's creative group
     */
    @Nullable String creativeGroup();

    /**
     * Gets the item's texture size. This is to resize the item if the texture is not 16x16.
     *
     * @return the item's texture size
     */
    int textureSize();

    /**
     * Gets the item's render offsets. If it is null, the item will be rendered normally, with no offsets.
     *
     * @return the item's render offsets
     */
    @Nullable CustomRenderOffsets renderOffsets();

    /**
     * Gets the item's set of tags that can be used in Molang.
     * Equivalent to "tag:some_tag"
     *
     * @return the item's tags, if they exist
     */
    @NonNull Set<String> tags();

    /**
     * Gets the stack size of the item.
     *
     * Returns 0 if not set. When not set (or 0), takes the Java item stack count when based of a vanilla item, or uses 64 when porting a modded item.
     *
     * @return the stack size of the item
     */
    @NonNegative
    int stackSize();

    /**
     * Gets the max damage of the item.
     *
     * Returns -1 if not set. When not set (or below 0), takes the Java item max damage when based of a vanilla item, or uses 0 when porting a modded item.
     *
     * @return the max damage of the item
     */
    int maxDamage();

    /**
     * Gets the attack damage of the item.
     * This is purely visual, and only applied to tools
     *
     * Returns 0 if not set. When 0, takes the Java item attack damage when based of a vanilla item, or uses 0 when porting a modded item.
     *
     * @return the attack damage of the item
     */
    int attackDamage();

    /**
     * Gets the tool type of the item.
     *
     * @return the tool type of the item
     */
    @Nullable String toolType();

    /**
     * Gets the tool tier of the item.
     *
     * @return the tool tier of the item
     */
    @Nullable String toolTier();

    /**
     * Gets the item's translation string.
     *
     * @return the item's translation string
     */
    @Nullable String translationString();

    /**
     * Gets the armor protection value of the item.
     *
     * @return the armor protection value of the item
     */
    int protectionValue();

    /**
     * Gets if the item is a foil. This is used to determine if the item should be rendered with an enchantment glint effect.
     *
     * @return if the item is a foil
     */
    boolean isFoil();

    /**
     * Gets if the item is edible.
     *
     * @return if the item is edible
     */
    boolean isEdible();

    /**
     * Gets if the food item can always be eaten.
     *
     * @return if the item is allowed to be eaten all the time
     */
    boolean canAlwaysEat();

    /**
     * @deprecated Use {@link #displayHandheld()} instead.
     * Gets if the item is a tool. This is used to set the render type of the item, if the item is handheld.
     *
     * @return if the item is a tool
     */
    @Deprecated
    default boolean isTool() {
        return displayHandheld();
    }

    static CustomItemData.Builder builder() {
        return GeyserApi.api().provider(CustomItemData.Builder.class);
    }

    interface Builder {
        /**
         * Will also set the display name and icon to the provided parameter, if it is currently not set.
         */
        Builder name(@NonNull String name);

        Builder customItemOptions(@NonNull CustomItemOptions customItemOptions);

        Builder displayName(@NonNull String displayName);

        Builder icon(@NonNull String icon);

        Builder allowOffhand(boolean allowOffhand);

        Builder displayHandheld(boolean displayHandheld);

        Builder creativeCategory(int creativeCategory);

        Builder creativeGroup(@Nullable String creativeGroup);

        Builder textureSize(int textureSize);

        Builder renderOffsets(@Nullable CustomRenderOffsets renderOffsets);

        Builder tags(@Nullable Set<String> tags);

        Builder stackSize(@NonNegative int stackSize);

        Builder maxDamage(int maxDamage);

        Builder attackDamage(int attackDamage);

        Builder toolType(@Nullable String toolType);

        Builder toolTier(@Nullable String toolTier);

        Builder protectionValue(int protectionValue);

        Builder translationString(@Nullable String translationString);

        Builder foil(boolean isFoil);

        Builder edible(boolean isEdible);

        Builder canAlwaysEat(boolean canAlwaysEat);

        /**
         * @deprecated Use {@link #displayHandheld(boolean)} instead.
         */
        @Deprecated
        default Builder tool(boolean isTool) {
            return displayHandheld(isTool);
        }

        CustomItemData build();
    }
}
