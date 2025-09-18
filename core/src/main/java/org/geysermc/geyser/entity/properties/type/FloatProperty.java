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

package org.geysermc.geyser.entity.properties.type;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.FloatEntityProperty;

public record FloatProperty(
    String name,
    float max,
    float min,
    Float defaultValue
) implements PropertyType<Float, FloatEntityProperty> {

    public FloatProperty {
        if (min > max) {
            throw new IllegalArgumentException("Cannot create float entity property (%s) with a minimum value (%s) greater than maximum (%s)!"
                .formatted(name, min, max));
        }
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Cannot create float entity property (%s) with a default value (%s) outside of the range (%s - %s)!"
                .formatted(name, defaultValue, min, max));
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public NbtMap nbtMap() {
        return NbtMap.builder()
                .putString("name", name)
                .putFloat("max", max)
                .putFloat("min", min)
                .putInt("type", 1)
                .build();
    }

    @Override
    public FloatEntityProperty defaultValue(int index) {
        return createValue(index, defaultValue == null ? 0f : defaultValue);
    }

    @Override
    public FloatEntityProperty createValue(int index, @NonNull Float value) {
        return new FloatEntityProperty(index, value);
    }

    @Override
    public Float fromObject(Object object) {
        if (object instanceof Float floatObject) {
            return floatObject;
        }
        throw new IllegalArgumentException("Cannot convert " + object + " to Float");
    }
}
