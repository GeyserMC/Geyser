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
     * Returns 0 if not set. When not set (or 0), it defaults to the stack count of the Java item when based of a vanilla item, or 64 when registering a non-vanilla item.
     *
     * @return the stack size of the item
     */
    @NonNegative
    int stackSize();

    /**
     * Gets the max damage of the item.
     *
     * Returns -1 if not set. When not set (or below 0), it defaults to the maximum damage of the Java item when based of a vanilla item, or uses 0 when registering a non-vanilla item.
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
     * Gets the armor type of the item.
     *
     * This can be "boots", "leggings", "chestplate", or "helmet", and makes the item able to be equipped into its respective equipment slot.
     * This should only be set if the Java vanilla/non-vanilla item is able to fit into the specified equipment slot.
     *
     * @return the armor type of the item
     */
    @Nullable String armorType();

    /**
     * Gets the armor protection value of the item.
     *
     * Only has a function when {@link CustomItemData#armorType} is set.
     *
     * @return the armor protection value of the item
     */
    int protectionValue();

    /**
     * Gets if the item is a hat. This is used to determine if the item should be rendered on the player's head, and
     * normally allow the player to equip it. This is not meant for armor.
     *
     * @return if the item is a hat
     */
    boolean isHat();

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

        Builder armorType(@Nullable String armorType);

        Builder protectionValue(int protectionValue);

        Builder hat(boolean isHat);

        Builder foil(boolean isFoil);

        Builder edible(boolean isEdible);

        Builder canAlwaysEat(boolean canAlwaysEat);

        CustomItemData build();
    }
}
