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

package org.geysermc.geyser.level.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;
import org.geysermc.geyser.api.block.custom.property.PropertyType;

import java.util.List;
import java.util.Map;

@Value
public class GeyserCustomBlockData implements CustomBlockData {
    String name;
    CustomBlockComponents components;
    Map<String, CustomBlockProperty<?>> properties;
    List<CustomBlockPermutation> permutations;

    @Override
    public @NonNull String name() {
        return name;
    }

    @Override
    public @NonNull String identifier() {
        return "geyser:" + name;
    }

    @Override
    public CustomBlockComponents components() {
        return components;
    }

    @Override
    public @NonNull Map<String, CustomBlockProperty<?>> properties() {
        return properties;
    }

    @Override
    public @NonNull List<CustomBlockPermutation> permutations() {
        return permutations;
    }

    @Override
    public CustomBlockState.Builder blockStateBuilder() {
        return new GeyserCustomBlockState.CustomBlockStateBuilder(this);
    }

    public static class CustomBlockDataBuilder implements Builder {
        private String name;
        private CustomBlockComponents components;
        private Map<String, CustomBlockProperty<?>> properties = new Object2ObjectOpenHashMap<>();
        private List<CustomBlockPermutation> permutations;

        @Override
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder components(@NonNull CustomBlockComponents components) {
            this.components = components;
            return this;
        }

        @Override
        public Builder booleanProperty(@NonNull String propertyName, List<Boolean> values) {
            this.properties.put(propertyName, new GeyserCustomBlockProperty<>(propertyName, values, PropertyType.BOOLEAN));
            return this;
        }

        @Override
        public Builder intProperty(@NonNull String propertyName, List<Integer> values) {
            this.properties.put(propertyName, new GeyserCustomBlockProperty<>(propertyName, values, PropertyType.INTEGER));
            return this;
        }

        @Override
        public Builder stringProperty(@NonNull String propertyName, List<String> values) {
            this.properties.put(propertyName, new GeyserCustomBlockProperty<>(propertyName, values, PropertyType.STRING));
            return this;
        }

        @Override
        public Builder permutations(@NonNull List<CustomBlockPermutation> permutations) {
            this.permutations = permutations;
            return this;
        }

        @Override
        public CustomBlockData build() {
            if (name == null) {
                throw new IllegalArgumentException("Name must be set");
            }
            if (properties == null) {
                properties = Object2ObjectMaps.emptyMap();
            } else {
                for (CustomBlockProperty<?> property : properties.values()) {
                    if (property.values().isEmpty() || property.values().size() > 16) {
                        throw new IllegalArgumentException(property.name() + " must contain 1 to 16 values.");
                    }
                }
            }
            if (permutations == null) {
                permutations = ObjectLists.emptyList();
            }
            return new GeyserCustomBlockData(name, components, properties, permutations);
        }
    }
}
