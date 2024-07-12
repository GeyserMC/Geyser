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

package org.geysermc.geyser.api.block.custom;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;
import org.geysermc.geyser.api.util.CreativeCategory;

import java.util.List;
import java.util.Map;

/**
 * This class is used to store data for a custom block.
 */
public interface CustomBlockData {
    /**
     * Gets the name of the custom block
     *
     * @return The name of the custom block.
     */
    @NonNull String name();

    /**
     * Gets the identifier of the custom block
     *
     * @return The identifier of the custom block.
     */
    @NonNull String identifier();

    /**
     * Gets if the custom block is included in the creative inventory
     * 
     * @return If the custom block is included in the creative inventory.
     */
    boolean includedInCreativeInventory();

    /**
     * Gets the block's creative category, or tab id.
     *
     * @return the block's creative category
     */
    @Nullable CreativeCategory creativeCategory();

    /**
     * Gets the block's creative group.
     *
     * @return the block's creative group
     */
    @Nullable String creativeGroup();

    /**
     * Gets the components of the custom block
     *
     * @return The components of the custom block.
     */
    @Nullable CustomBlockComponents components();

    /**
     * Gets the custom block's map of block property names to CustomBlockProperty
     * objects
     *
     * @return The custom block's map of block property names to CustomBlockProperty objects.
     */
    @NonNull Map<String, CustomBlockProperty<?>> properties();

    /**
     * Gets the list of the custom block's permutations
     *
     * @return The permutations of the custom block.
     */
    @NonNull List<CustomBlockPermutation> permutations();

    /**
     * Gets the custom block's default block state
     *
     * @return The default block state of the custom block.
     */
    @NonNull CustomBlockState defaultBlockState();

    /**
     * Gets a builder for a custom block state
     *
     * @return The builder for a custom block state.
     */
    CustomBlockState.@NonNull Builder blockStateBuilder();

    /**
     * Create a Builder for CustomBlockData
     *
     * @return A CustomBlockData Builder
     */
    static CustomBlockData.Builder builder() {
        return GeyserApi.api().provider(CustomBlockData.Builder.class);
    }

    interface Builder {
        Builder name(@NonNull String name);

        Builder includedInCreativeInventory(boolean includedInCreativeInventory);

        Builder creativeCategory(@Nullable CreativeCategory creativeCategory);

        Builder creativeGroup(@Nullable String creativeGroup);

        Builder components(@NonNull CustomBlockComponents components);

        Builder booleanProperty(@NonNull String propertyName);

        Builder intProperty(@NonNull String propertyName, List<Integer> values);

        Builder stringProperty(@NonNull String propertyName, List<String> values);

        Builder permutations(@NonNull List<CustomBlockPermutation> permutations);

        CustomBlockData build();
    }
}
