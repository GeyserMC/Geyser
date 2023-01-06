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

import it.unimi.dsi.fastutil.objects.*;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.property.CustomBlockProperty;
import org.geysermc.geyser.api.block.custom.property.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Value
public class GeyserCustomBlockData implements CustomBlockData {

    String name;
    CustomBlockComponents components;
    Map<String, CustomBlockProperty<?>> properties;
    List<CustomBlockPermutation> permutations;

    Map<String, Object> defaultProperties;

    private GeyserCustomBlockData(CustomBlockDataBuilder builder) {
        this.name = builder.name;
        if (name == null) {
            throw new IllegalStateException("Name must be set");
        }

        this.components = builder.components;

        if (!builder.properties.isEmpty()) {
            this.properties = Object2ObjectMaps.unmodifiable(new Object2ObjectArrayMap<>(builder.properties));
            Object2ObjectMap<String, Object> defaultProperties = new Object2ObjectOpenHashMap<>(this.properties.size());
            for (CustomBlockProperty<?> property : properties.values()) {
                if (property.values().size() > 16) {
                    GeyserImpl.getInstance().getLogger().warning(property.name() + " contains more than 16 values, but BDS specifies it should not. This may break in future versions.");
                }
                if (property.values().stream().distinct().count() != property.values().size()) {
                    throw new IllegalStateException(property.name() + " has duplicate values.");
                }
                if (property.values().isEmpty()) {
                    throw new IllegalStateException(property.name() + " contains no values.");
                }
                defaultProperties.put(property.name(), property.values().get(0));
            }
            this.defaultProperties = Object2ObjectMaps.unmodifiable(defaultProperties);
        } else {
            this.properties = Object2ObjectMaps.emptyMap();
            this.defaultProperties = Object2ObjectMaps.emptyMap();
        }

        if (!builder.permutations.isEmpty()) {
            this.permutations = ObjectLists.unmodifiable(new ObjectArrayList<>(builder.permutations));
        } else {
            this.permutations = ObjectLists.emptyList();
        }
    }

    @Override
    public @NonNull String name() {
        return name;
    }

    @Override
    public @NonNull String identifier() {
        return Constants.GEYSER_NAMESPACE + name;
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
    public @NonNull CustomBlockState defaultBlockState() {
        return new GeyserCustomBlockState(this, defaultProperties);
    }

    @Override
    public CustomBlockState.@NotNull Builder blockStateBuilder() {
        return new GeyserCustomBlockState.CustomBlockStateBuilder(this);
    }

    public static class CustomBlockDataBuilder implements Builder {
        private String name;
        private CustomBlockComponents components;
        private final Object2ObjectMap<String, CustomBlockProperty<?>> properties = new Object2ObjectOpenHashMap<>();
        private List<CustomBlockPermutation> permutations = ObjectLists.emptyList();

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
        public Builder booleanProperty(@NonNull String propertyName) {
            this.properties.put(propertyName, new GeyserCustomBlockProperty<>(propertyName, List.of((byte) 0, (byte) 1), PropertyType.BOOLEAN));
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
            return new GeyserCustomBlockData(this);
        }
    }
}
