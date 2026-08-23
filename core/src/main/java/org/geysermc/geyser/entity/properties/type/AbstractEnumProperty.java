/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.entity.IntEntityProperty;
import org.geysermc.geyser.api.util.Identifier;

import java.util.List;
import java.util.regex.Pattern;

public interface AbstractEnumProperty<T> extends PropertyType<T, IntEntityProperty> {

    Pattern VALUE_VALIDATION_REGEX = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,31}$");

    @Override
    default NbtMap nbtMap() {
        return NbtMap.builder()
            .putString("name", identifier().toString())
            .putList("enum", NbtType.STRING, allBedrockValues())
            .putInt("type", 3)
            .build();
    }

    default void validateAllValues(Identifier name, List<String> values) {
        if (values.size() > 16) {
            throw new IllegalArgumentException("Cannot register enum property with name " + name + " because it has more than 16 values!");
        }

        for (String value : values) {
            if (!VALUE_VALIDATION_REGEX.matcher(value).matches()) {
                throw new IllegalArgumentException(
                    "Cannot register enum property with name " + name + " and value " + value +
                        " because enum values can only contain alphanumeric characters and underscores."
                );
            }
        }
    }

    List<String> allBedrockValues();

    @Override
    default IntEntityProperty defaultValue(int index) {
        return new IntEntityProperty(index, defaultIndex());
    }

    @Override
    default IntEntityProperty createValue(int index, @Nullable T value) {
        if (value == null) {
            return defaultValue(index);
        }

        int valueIndex = indexOf(value);
        if (valueIndex == -1) {
            throw new IllegalArgumentException("Enum value " + value + " is not a valid enum value!");
        }
        return new IntEntityProperty(index, valueIndex);
    }

    int indexOf(T value);

    int defaultIndex();
}
