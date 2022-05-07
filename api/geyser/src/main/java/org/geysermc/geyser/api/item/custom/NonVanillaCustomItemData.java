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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;

import java.util.OptionalInt;
import java.util.Set;

/**
 * This is used to store data for a custom item.
 */
public interface NonVanillaCustomItemData extends CustomItemData {
    /**
     * Gets the java identifier for this item.
     *
     * @return The java identifier for this item.
     */
    @NonNull String identifier();

    /**
     * Gets the java item id of the item.
     *
     * @return the java item id of the item
     */
    int javaId();

    /**
     * Gets the stack size of the item.
     *
     * @return the stack size of the item
     */
    int stackSize();

    /**
     * Gets the max damage of the item.
     *
     * @return the max damage of the item
     */
    int maxDamage();

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
     * Gets the armor type of the item.
     *
     * @return the armor type of the item
     */
    @Nullable String armorType();

    /**
     * Gets the armor tier of the item.
     *
     * @return the armor tier of the item
     */
    @Nullable String armorTier();

    /**
     * Gets the armor protection value of the item.
     *
     * @return the armor protection value of the item
     */
    int protectionValue();

    /**
     * Gets the item's translation string.
     *
     * @return the item's translation string
     */
    @Nullable String translationString();

    /**
     * Gets the repair materials of the item.
     *
     * @return the repair materials of the item
     */
    @Nullable Set<String> repairMaterials();

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
     * Gets if the item is a hat. This is used to determine if the item should be rendered on the player's head, and
     * normally allow the player to equip it. This is not meant for armor.
     *
     * @return if the item is a hat
     */
    boolean isHat();

    /**
     * Gets if the item is a tool. This is used to set the render type of the item, if the item is handheld.
     *
     * @return if the item is a tool
     */
    boolean isTool();

    static NonVanillaCustomItemData.Builder builder() {
        return GeyserApi.api().providerManager().builderProvider().provideBuilder(NonVanillaCustomItemData.Builder.class);
    }

    interface Builder extends CustomItemData.Builder {
        Builder name(@NonNull String name);

        Builder identifier(@NonNull String identifier);

        Builder javaId(int javaId);

        Builder stackSize(int stackSize);

        Builder maxDamage(int maxDamage);

        Builder toolType(@Nullable String toolType);

        Builder toolTier(@Nullable String toolTier);

        Builder armorType(@Nullable String armorType);

        Builder armorTier(@Nullable String armorTier);

        Builder protectionValue(int protectionValue);

        Builder translationString(@Nullable String translationString);

        Builder repairMaterials(@Nullable Set<String> repairMaterials);

        Builder creativeCategory(int creativeCategory);

        Builder creativeGroup(@Nullable String creativeGroup);

        Builder isHat(boolean isHat);

        Builder isTool(boolean isTool);

        @Override
        Builder displayName(@NonNull String displayName);

        @Override
        Builder allowOffhand(boolean allowOffhand);

        @Override
        Builder textureSize(int textureSize);

        @Override
        Builder renderOffsets(@Nullable CustomRenderOffsets renderOffsets);

        NonVanillaCustomItemData build();
    }
}
