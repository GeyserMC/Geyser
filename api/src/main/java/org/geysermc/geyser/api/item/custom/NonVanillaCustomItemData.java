/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import java.util.Set;

/**
 * Represents a completely custom item that is not based on an existing vanilla Minecraft item.
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
    @NonNegative int javaId();

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
     * Gets if the item is chargable, like a bow.
     *
     * @return if the item should act like a chargable item
     */
    boolean isChargeable();

    /**
     * Gets the block the item places.
     *
     * @return the block the item places
     */
    String block();

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

    static NonVanillaCustomItemData.Builder builder() {
        return GeyserApi.api().provider(NonVanillaCustomItemData.Builder.class);
    }

    interface Builder extends CustomItemData.Builder {
        @Override
        Builder name(@NonNull String name);

        @Override
        Builder customItemOptions(@NonNull CustomItemOptions customItemOptions);

        @Override
        Builder displayName(@NonNull String displayName);

        @Override
        Builder icon(@NonNull String icon);

        @Override
        Builder allowOffhand(boolean allowOffhand);

        @Override
        Builder displayHandheld(boolean displayHandheld);

        @Override
        Builder creativeCategory(int creativeCategory);

        @Override
        Builder creativeGroup(@Nullable String creativeGroup);

        @Override
        Builder textureSize(int textureSize);

        @Override
        Builder renderOffsets(@Nullable CustomRenderOffsets renderOffsets);

        @Override
        Builder tags(@Nullable Set<String> tags);

        @Override
        Builder stackSize(@NonNegative int stackSize);

        @Override
        Builder maxDamage(int maxDamage);

        @Override
        Builder attackDamage(int attackDamage);

        @Override
        Builder toolType(@Nullable String toolType);

        @Override
        Builder toolTier(@Nullable String toolTier);

        @Override
        Builder armorType(@Nullable String armorType);

        @Override
        Builder protectionValue(int protectionValue);

        @Override
        Builder hat(boolean isHat);

        @Override
        Builder foil(boolean isFoil);

        @Override
        Builder edible(boolean isEdible);

        @Override
        Builder canAlwaysEat(boolean canAlwaysEat);

        /**
         * @deprecated Use {@link #displayHandheld(boolean)} instead.
         */
        @Deprecated
        default Builder tool(boolean isTool) {
            return displayHandheld(isTool);
        }

        Builder identifier(@NonNull String identifier);

        Builder javaId(@NonNegative int javaId);

        Builder translationString(@Nullable String translationString);

        Builder repairMaterials(@Nullable Set<String> repairMaterials);

        Builder chargeable(boolean isChargeable);

        Builder block(String block);

        NonVanillaCustomItemData build();
    }
}
