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

import java.util.List;

/**
 * Represents enums we don't need classes for in Geyser.
 */
public final class BasicEnumProperty extends Property<String> {
    private final List<String> values;

    private BasicEnumProperty(String name, List<String> values) {
        super(name);
        this.values = values;
    }

    @Override
    public int valuesCount() {
        return this.values.size();
    }

    @Override
    public int indexOf(String value) {
        int index = this.values.indexOf(value);
        if (index == -1) {
            throw new IllegalArgumentException("Property " + this + " does not have value " + value);
        }
        return index;
    }

    @SuppressWarnings("unchecked")
    public <T> T values() {
        return (T) this.values;
    }

    public static BasicEnumProperty create(String name, String... values) {
        return new BasicEnumProperty(name, List.of(values));
    }
}
