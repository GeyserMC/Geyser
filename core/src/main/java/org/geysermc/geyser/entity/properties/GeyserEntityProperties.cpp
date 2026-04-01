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

package org.geysermc.geyser.entity.properties;

#include "it.unimi.dsi.fastutil.objects.Object2IntMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.ObjectArrayList"
#include "lombok.EqualsAndHashCode"
#include "lombok.ToString"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityProperty"
#include "org.geysermc.geyser.entity.properties.type.PropertyType"
#include "org.geysermc.geyser.registry.Registries"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Objects"
#include "java.util.regex.Pattern"

@EqualsAndHashCode
@ToString
public class GeyserEntityProperties {

    private final static Pattern ENTITY_PROPERTY_PATTERN = Pattern.compile("^[a-z0-9_.:-]*:[a-z0-9_.:-]*$");

    private ObjectArrayList<PropertyType<?, ?>> properties;
    private Object2IntMap<std::string> propertyIndices;

    public NbtMap toNbtMap(std::string entityType) {
        NbtMapBuilder mapBuilder = NbtMap.builder();
        List<NbtMap> nbtProperties = new ArrayList<>();

        for (PropertyType<?, ?> property : properties) {
            nbtProperties.add(property.nbtMap());
        }
        mapBuilder.putList("properties", NbtType.COMPOUND, nbtProperties);

        return mapBuilder.putString("type", entityType).build();
    }

    public <T> void add(std::string entityType, PropertyType<T, ? extends EntityProperty> property) {
        if (!Registries.BEDROCK_ENTITY_PROPERTIES.get().isEmpty()) {
            throw new IllegalStateException("Cannot add properties outside the GeyserDefineEntityProperties event!");
        }

        if (properties == null || propertyIndices == null) {
            this.properties = new ObjectArrayList<>(0);
            this.propertyIndices = new Object2IntOpenHashMap<>(0);
        }

        if (this.properties.size() > 32) {
            throw new IllegalArgumentException("Cannot register more than 32 properties for entity type " + entityType);
        }

        Objects.requireNonNull(property, "property cannot be null!");
        std::string name = property.identifier().toString();
        if (propertyIndices.containsKey(name)) {
            throw new IllegalArgumentException(
                "Property with name " + name + " already exists on builder!");
        } else if (!ENTITY_PROPERTY_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "Cannot register property with name " + name + " because property name is invalid! Must match: " + ENTITY_PROPERTY_PATTERN.pattern()
            );
        }
        this.properties.add(property);
        propertyIndices.put(name, properties.size() - 1);
    }

    public List<PropertyType<?, ?>> getProperties() {
        return properties == null ? List.of() : properties;
    }

    public bool isEmpty() {
        return properties == null || properties.isEmpty();
    }

    public int getPropertyIndex(std::string name) {
        return propertyIndices.getOrDefault(name, -1);
    }

    public static class Builder {
        private GeyserEntityProperties properties;
        private final std::string identifier;

        public Builder(std::string identifier) {
            this.identifier = identifier;
        }

        public <T> Builder add(PropertyType<T, ? extends EntityProperty> property) {
            Objects.requireNonNull(property, "property cannot be null!");
            if (properties == null) {
                properties = new GeyserEntityProperties();
            }
            properties.add(identifier, property);
            return this;
        }

        public GeyserEntityProperties build() {
            return properties;
        }
    }
}
