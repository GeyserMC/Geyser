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

package org.geysermc.geyser.entity.properties.type;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.entity.IntEntityProperty;
import org.geysermc.geyser.entity.properties.GeyserEntityPropertyManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record EnumProperty(
    String name,
    List<String> values,
    Object2IntMap<String> valueIndexMap,
    int defaultIndex
) implements PropertyType<String, IntEntityProperty> {

    public static final Pattern VALUE_VALIDATION_REGEX = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,31}$");

    public EnumProperty {
        if (values.size() > 16) {
            throw new IllegalArgumentException("Cannot register enum property with name " + name + " because it has more than 16 values!");
        }

        for (String value : values) {
            if (value == null) {
                throw new IllegalArgumentException(
                    "Cannot register enum property with name " + name + " because it contains a null value."
                );
            } else if (!VALUE_VALIDATION_REGEX.matcher(value).matches()) {
                throw new IllegalArgumentException(
                    "Cannot register enum property with name " + name + " and value " + value +
                        " because enum values can only contain alphanumeric characters and underscores."
                );
            }
        }

        if (defaultIndex < 0) {
            throw new IllegalArgumentException("Unable to find default value for enum property with name " + name);
        }
    }

    public EnumProperty(String name, List<String> values, @Nullable String defaultValue) {
        this(name, values, getValueIndexMap(values), defaultValue == null ? 0 : values.indexOf(defaultValue));
    }

    public <E extends Enum<E>> EnumProperty(@NonNull String name, @NonNull Class<E> enumClass, @Nullable E defaultValue) {
        this(name,
            Arrays.stream(enumClass.getEnumConstants())
                .map(entry -> entry.name().toLowerCase(Locale.ROOT))
                .toList(),
            defaultValue != null ? defaultValue.name().toLowerCase(Locale.ROOT) : null);
    }

    private static Object2IntMap<String> getValueIndexMap(List<String> values) {
        Object2IntMap<String> valueIndexMap = new Object2IntOpenHashMap<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            valueIndexMap.put(values.get(i), i);
        }
        return valueIndexMap;
    }

    @Override
    public NbtMap nbtMap() {
        return NbtMap.builder()
                .putString("name", name)
                .putList("enum", NbtType.STRING, values)
                .putInt("type", 3)
                .build();
    }

    @Override
    public Class<String> typeClass() {
        return String.class;
    }

    @Override
    public IntEntityProperty defaultValue(int index) {
        return new IntEntityProperty(index, defaultIndex);
    }

    @Override
    public IntEntityProperty createValue(int index, @NonNull String value) {
        int valueIndex = getIndex(value);
        if (valueIndex == -1) {
            throw new IllegalArgumentException("Enum value " + value + " is not a valid enum value!");
        }
        return new IntEntityProperty(index, valueIndex);
    }

    public int getIndex(String value) {
        return valueIndexMap.getOrDefault(value, -1);
    }

    public <E extends Enum<E>> void apply(GeyserEntityPropertyManager manager, E value) {
        apply(manager, value.name().toLowerCase(Locale.ROOT));
    }
}
