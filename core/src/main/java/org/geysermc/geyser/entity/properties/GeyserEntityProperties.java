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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@EqualsAndHashCode
@ToString
public class GeyserEntityProperties {

    private final static Pattern ENTITY_PROPERTY_PATTERN = Pattern.compile("^[a-z0-9_.:-]*:[a-z0-9_.:-]*$");

    private final ObjectArrayList<PropertyType<?, ?>> properties;
    private final Object2IntMap<String> propertyIndices;

    private GeyserEntityProperties(ObjectArrayList<PropertyType<?, ?>> properties,
            Object2IntMap<String> propertyIndices) {
        this.properties = properties;
        this.propertyIndices = propertyIndices;
    }

    public NbtMap toNbtMap(String entityType) {
        NbtMapBuilder mapBuilder = NbtMap.builder();
        List<NbtMap> nbtProperties = new ArrayList<>();

        for (PropertyType<?, ?> property : properties) {
            nbtProperties.add(property.nbtMap());
        }
        mapBuilder.putList("properties", NbtType.COMPOUND, nbtProperties);

        return mapBuilder.putString("type", entityType).build();
    }

    public Builder toBuilder() {
        return new Builder(properties, propertyIndices);
    }

    public @NonNull List<PropertyType<?, ?>> getProperties() {
        return properties;
    }

    public int getPropertyIndex(String name) {
        return propertyIndices.getOrDefault(name, -1);
    }

    public static class Builder {
        private final ObjectArrayList<PropertyType<?, ?>> properties = new ObjectArrayList<>();
        private final Object2IntMap<String> propertyIndices = new Object2IntOpenHashMap<>();

        public Builder(ObjectArrayList<PropertyType<?, ?>> properties, Object2IntMap<String> propertyIndices) {
            this.properties.addAll(properties);
            this.propertyIndices.putAll(propertyIndices);
        }

        public Builder() {
        }

        public boolean isEmpty() {
            return this.properties.isEmpty();
        }

        public List<PropertyType<?, ?>> properties() {
            return properties;
        }

        public <T> Builder add(@NonNull PropertyType<T, ? extends EntityProperty> property) {
            Objects.requireNonNull(property, "property cannot be null!");
            String name = property.name();
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
            return this;
        }

        public GeyserEntityProperties build() {
            return new GeyserEntityProperties(properties, propertyIndices);
        }
    }
}
