/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.api.util.CreativeCategory;

import java.util.List;

/**
 * Represents a completely custom block that is not based on an existing vanilla Minecraft block.
 */
public interface NonVanillaCustomBlockData extends CustomBlockData {
    /**
     * Gets the namespace of the custom block
     *
     * @return The namespace of the custom block.
     */
    @NonNull String namespace();


    /**
     * Create a Builder for NonVanillaCustomBlockData
     *
     * @return A NonVanillaCustomBlockData Builder
     */
    static NonVanillaCustomBlockData.Builder builder() {
        return GeyserApi.api().provider(NonVanillaCustomBlockData.Builder.class);
    }

    interface Builder extends CustomBlockData.Builder {

        Builder namespace(@NonNull String namespace);

        @Override
        Builder name(@NonNull String name);

        @Override
        Builder includedInCreativeInventory(boolean includedInCreativeInventory);

        @Override
        Builder creativeCategory(@Nullable CreativeCategory creativeCategory);

        @Override
        Builder creativeGroup(@Nullable String creativeGroup);

        @Override
        Builder components(@NonNull CustomBlockComponents components);

        @Override
        Builder booleanProperty(@NonNull String propertyName);

        @Override
        Builder intProperty(@NonNull String propertyName, List<Integer> values);

        @Override
        Builder stringProperty(@NonNull String propertyName, List<String> values);

        @Override
        Builder permutations(@NonNull List<CustomBlockPermutation> permutations);

        @Override
        NonVanillaCustomBlockData build();
    }
}
