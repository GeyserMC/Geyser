/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block.property;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public final class EnumProperty<T extends Enum<T>> extends Property<T> {
    private final IntList ordinalValues;

    /**
     * @param values all possible values of this enum.
     */
    private EnumProperty(String name, T[] values) {
        super(name);
        this.ordinalValues = new IntArrayList(values.length);
        for (T anEnum : values) {
            this.ordinalValues.add(anEnum.ordinal());
        }
    }

    @Override
    public int valuesCount() {
        return this.ordinalValues.size();
    }

    @Override
    public int indexOf(T value) {
        return this.ordinalValues.indexOf(value.ordinal());
    }

    @SafeVarargs
    public static <T extends Enum<T>> EnumProperty<T> create(String name, T... values) {
        return new EnumProperty<>(name, values);
    }
}
