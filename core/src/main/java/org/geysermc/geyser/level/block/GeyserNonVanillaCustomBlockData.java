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

package org.geysermc.geyser.level.block;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.util.CreativeCategory;

import java.util.List;

public class GeyserNonVanillaCustomBlockData extends GeyserCustomBlockData implements NonVanillaCustomBlockData {
    private final String namespace;

    GeyserNonVanillaCustomBlockData(Builder builder) {
        super(builder);

        this.namespace = builder.namespace;
        if (namespace == null) {
            throw new IllegalStateException("Identifier must be set");
        }
    }

    @Override
    public @NonNull String identifier() {
        return this.namespace + ":" + super.name();
    }

    @Override
    public @NonNull String namespace() {
        return this.namespace;
    }

    public static class Builder extends GeyserCustomBlockData.Builder implements NonVanillaCustomBlockData.Builder {
        private String namespace;

        @Override
        public Builder namespace(@NonNull String namespace) {
            this.namespace = namespace;
            return this;
        }

        @Override
        public Builder name(@NonNull String name) {
            return (Builder) super.name(name);
        }

        @Override
        public Builder includedInCreativeInventory(boolean includedInCreativeInventory) {
            return (Builder) super.includedInCreativeInventory(includedInCreativeInventory);
        }

        @Override
        public Builder creativeCategory(@Nullable CreativeCategory creativeCategories) {
            return (Builder) super.creativeCategory(creativeCategories);
        }

        @Override
        public Builder creativeGroup(@Nullable String creativeGroup) {
            return (Builder) super.creativeGroup(creativeGroup);
        }

        @Override
        public Builder components(@NonNull CustomBlockComponents components) {
            return (Builder) super.components(components);
        }

        @Override
        public Builder booleanProperty(@NonNull String propertyName) {
            return (Builder) super.booleanProperty(propertyName);
        }

        @Override
        public Builder intProperty(@NonNull String propertyName, List<Integer> values) {
            return (Builder) super.intProperty(propertyName, values);
        }

        @Override
        public Builder stringProperty(@NonNull String propertyName, List<String> values) {
            return (Builder) super.stringProperty(propertyName, values);
        }

        @Override
        public Builder permutations(@NonNull List<CustomBlockPermutation> permutations) {
            return (Builder) super.permutations(permutations);
        }

        @Override
        public NonVanillaCustomBlockData build() {
            return new GeyserNonVanillaCustomBlockData(this);
        }
    }
}
