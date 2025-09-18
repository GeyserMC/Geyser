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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.IntEntityProperty;
import org.geysermc.geyser.GeyserImpl;

public record IntProperty(
    String name,
    int max,
    int min,
    Integer defaultValue
) implements PropertyType<Integer, IntEntityProperty> {

    public IntProperty {
        if (min > max) {
            throw new IllegalArgumentException("Cannot create int entity property (%s) with a minimum value (%s) greater than maximum (%s)!"
                .formatted(name, min, max));
        }
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Cannot create int entity property (%s) with a default value (%s) outside of the range (%s - %s)!"
                .formatted(name, defaultValue, min, max));
        }
        if (min < -1000000 || max > 1000000) {
            // https://learn.microsoft.com/en-us/minecraft/creator/documents/introductiontoentityproperties?view=minecraft-bedrock-stable#a-note-on-large-integer-entity-property-values
            GeyserImpl.getInstance().getLogger().warning("Using int entity properties with min / max values larger than +- 1 million is not recommended!");
        }
    }

    @Override
    public NbtMap nbtMap() {
        return NbtMap.builder()
                .putString("name", name)
                .putInt("max", max)
                .putInt("min", min)
                .putInt("type", 0)
                .build();
    }

    @Override
    public Class<Integer> typeClass() {
        return Integer.class;
    }

    @Override
    public IntEntityProperty defaultValue(int index) {
        return createValue(index, defaultValue == null ? 0 : defaultValue);
    }

    @Override
    public IntEntityProperty createValue(int index, @NonNull Integer value) {
        return new IntEntityProperty(index, value);
    }
}
