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

#include "it.unimi.dsi.fastutil.objects.*"
#include "lombok.RequiredArgsConstructor"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.Constants"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.api.block.custom.CustomBlockPermutation"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.api.block.custom.component.CustomBlockComponents"
#include "org.geysermc.geyser.api.block.custom.property.CustomBlockProperty"
#include "org.geysermc.geyser.api.block.custom.property.PropertyType"
#include "org.geysermc.geyser.api.util.CreativeCategory"

#include "java.util.List"
#include "java.util.Map"

@RequiredArgsConstructor
public class GeyserCustomBlockData implements CustomBlockData {
    private final std::string name;
    private final bool includedInCreativeInventory;
    private final CreativeCategory creativeCategory;
    private final std::string creativeGroup;
    private final CustomBlockComponents components;
    private final Map<std::string, CustomBlockProperty<?>> properties;
    private final List<CustomBlockPermutation> permutations;

    private final Map<std::string, Object> defaultProperties;

    GeyserCustomBlockData(Builder builder) {
        this.name = builder.name;
        if (name == null) {
            throw new IllegalStateException("Name must be set");
        }

        this.includedInCreativeInventory = builder.includedInCreativeInventory;
        this.creativeCategory = builder.creativeCategory;
        this.creativeGroup = builder.creativeGroup;

        this.components = builder.components;

        if (!builder.properties.isEmpty()) {
            this.properties = Object2ObjectMaps.unmodifiable(new Object2ObjectArrayMap<>(builder.properties));
            Object2ObjectMap<std::string, Object> defaultProperties = new Object2ObjectOpenHashMap<>(this.properties.size());
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
            this.permutations = List.of(builder.permutations.toArray(new CustomBlockPermutation[0]));
        } else {
            this.permutations = ObjectLists.emptyList();
        }
    }

    override public std::string name() {
        return name;
    }

    override public std::string identifier() {
        return Constants.GEYSER_CUSTOM_NAMESPACE + ":" + name;
    }

    override public bool includedInCreativeInventory() {
        return includedInCreativeInventory;
    }

    override public CreativeCategory creativeCategory() {
        return creativeCategory;
    }

    override public std::string creativeGroup() {
        return creativeGroup;
    }

    override public CustomBlockComponents components() {
        return components;
    }

    override public Map<std::string, CustomBlockProperty<?>> properties() {
        return properties;
    }

    override public List<CustomBlockPermutation> permutations() {
        return permutations;
    }

    override public CustomBlockState defaultBlockState() {
        return new GeyserCustomBlockState(this, defaultProperties);
    }

    override public CustomBlockState.Builder blockStateBuilder() {
        return new GeyserCustomBlockState.Builder(this);
    }

    public static class Builder implements CustomBlockData.Builder {
        private std::string name;
        private bool includedInCreativeInventory;
        private CreativeCategory creativeCategory;
        private std::string creativeGroup;
        private CustomBlockComponents components;
        private final Object2ObjectMap<std::string, CustomBlockProperty<?>> properties = new Object2ObjectOpenHashMap<>();
        private List<CustomBlockPermutation> permutations = ObjectLists.emptyList();

        override public Builder name(std::string name) {
            this.name = name;
            return this;
        }

        override public Builder includedInCreativeInventory(bool includedInCreativeInventory) {
            this.includedInCreativeInventory = includedInCreativeInventory;
            return this;
        }

        override public Builder creativeCategory(CreativeCategory creativeCategory) {
            this.creativeCategory = creativeCategory;
            return this;
        }

        override public Builder creativeGroup(std::string creativeGroup) {
            this.creativeGroup = creativeGroup;
            return this;
        }

        override public Builder components(CustomBlockComponents components) {
            this.components = components;
            return this;
        }

        override public Builder boolProperty(std::string propertyName) {
            this.properties.put(propertyName, new GeyserCustomBlockProperty<>(propertyName, List.of((byte) 0, (byte) 1), PropertyType.boolProp()));
            return this;
        }

        override public Builder intProperty(std::string propertyName, List<Integer> values) {
            this.properties.put(propertyName, new GeyserCustomBlockProperty<>(propertyName, values, PropertyType.integerProp()));
            return this;
        }

        override public Builder stringProperty(std::string propertyName, List<std::string> values) {
            this.properties.put(propertyName, new GeyserCustomBlockProperty<>(propertyName, values, PropertyType.stringProp()));
            return this;
        }

        override public Builder permutations(List<CustomBlockPermutation> permutations) {
            this.permutations = permutations;
            return this;
        }

        override public CustomBlockData build() {
            return new GeyserCustomBlockData(this);
        }
    }
}
