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
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode
@ToString
public class GeyserEntityProperties {
    private final ObjectArrayList<PropertyType> properties;
    private final Object2IntMap<String> propertyIndices;

    private GeyserEntityProperties(ObjectArrayList<PropertyType> properties,
            Object2IntMap<String> propertyIndices) {
        this.properties = properties;
        this.propertyIndices = propertyIndices;
    }

    public void addToBuilder(Builder builder) {
        for (Object2IntMap.Entry<String> entry : propertyIndices.object2IntEntrySet()) {
            builder.add(entry.getKey(), properties.get(entry.getIntValue()));
        }
    }

    public NbtMap toNbtMap(String entityType) {
        NbtMapBuilder mapBuilder = NbtMap.builder();
        List<NbtMap> nbtProperties = new ArrayList<>();

        for (PropertyType property : properties) {
            nbtProperties.add(property.nbtMap());
        }
        mapBuilder.putList("properties", NbtType.COMPOUND, nbtProperties);

        return mapBuilder.putString("type", entityType).build();
    }

    public @NonNull List<PropertyType> getProperties() {
        return properties;
    }

    public int getPropertyIndex(String name) {
        return propertyIndices.getOrDefault(name, -1);
    }

    public static class Builder {
        private final ObjectArrayList<PropertyType> properties = new ObjectArrayList<>();
        private final Object2IntMap<String> propertyIndices = new Object2IntOpenHashMap<>();

        public boolean isEmpty() {
            return this.properties.isEmpty();
        }

        public Builder add(@NonNull String name, PropertyType property) {
            if (propertyIndices.containsKey(name)) {
                throw new IllegalArgumentException(
                    "Property with name " + name + " already exists on builder!");
            }
            else if (name.matches(".*[A-Z].*") || name.contains(" ")) {
                throw new IllegalArgumentException(
                    "Cannot register property with name " + name + " because property names cannot contain capital letters or spaces."
                );
            }
            else if (!name.contains(":")) {
                throw new IllegalArgumentException(
                    "Property identifier must have a namespace. " + "Property with name " + name + " was not registered."
                );
            }
            this.properties.add(property);
            propertyIndices.put(name, properties.size() - 1);
            return this;
        }

        public Builder addInt(@NonNull String name, int min, int max) {
            return add(name, new IntProperty(name, min, max));
        }

        public Builder addInt(@NonNull String name) {
            return addInt(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        public Builder addFloat(@NonNull String name, float min, float max) {
            return add(name, new FloatProperty(name, min, max));
        }

        public Builder addFloat(@NonNull String name) {
            return addFloat(name, Float.MIN_VALUE, Float.MAX_VALUE);
        }

        public Builder addBoolean(@NonNull String name) {
            return add(name, new BooleanProperty(name));
        }

        public Builder addEnum(@NonNull String name, @NonNull List<String> values) {
            for (String value : values) {
                if (value == null) {
                    throw new IllegalArgumentException(
                        "Cannot register enum property with name " + name + " because it contains a null value."
                    );
                }
                else if (name.matches("^[a-zA-Z0-9_]*$") || name.contains(" ")) {
                    throw new IllegalArgumentException(
                        "Cannot register enum property with name " + name + " and value " + value +
                            " because enum values can only contain alphanumeric characters and underscores."
                    );
                }
            }
            return add(name, new EnumProperty(name, values));
        }

        public Builder addEnum(@NonNull String name, @NonNull String... values) {
            return addEnum(name, Arrays.asList(values));
        }

        public GeyserEntityProperties build() {
            return new GeyserEntityProperties(properties, propertyIndices);
        }
    }
}
