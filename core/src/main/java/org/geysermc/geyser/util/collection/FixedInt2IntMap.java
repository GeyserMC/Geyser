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

package org.geysermc.geyser.util.collection;

import it.unimi.dsi.fastutil.ints.AbstractInt2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.Serial;

public class FixedInt2IntMap extends AbstractInt2IntMap {
    protected int[] value;
    protected int start = -1;

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public int size() {
        return value.length;
    }

    @Override
    public ObjectSet<Int2IntMap.Entry> int2IntEntrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int get(int i) {
        return getOrDefault(i, defRetValue);
    }

    @Override
    public int getOrDefault(int key, int defaultValue) {
        int offset = key - start;
        if (offset < 0 || offset >= value.length) {
            return defaultValue;
        }

        return value[offset];
    }

    @Override
    public int put(int key, int value) {
        if (start == -1) {
            start = key;
            this.value = new int[] {value};
        } else {
            int offset = key - start;
            if (offset >= 0 && offset < this.value.length) {
                int curr = this.value[offset];
                this.value[offset] = value;
                return curr;
            } else if (offset != this.value.length) {
                throw new IndexOutOfBoundsException("Expected: " + (this.value.length + start) + ", got " + key);
            }

            int[] newValue = new int[offset + 1];
            System.arraycopy(this.value, 0, newValue, 0, this.value.length);
            this.value = newValue;
            this.value[offset] = value;
        }

        return this.defRetValue;
    }

    @Override
    public boolean containsKey(int k) {
        int offset = k - start;
        return offset >= 0 && offset < value.length;
    }

    @Override
    public boolean containsValue(int v) {
        for (int i : value) {
            if (i == v) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        int index = start;
        for (int i : value) {
            builder.append(index++).append("=>").append(i);
            if (index < value.length + start) {
                // Add commas while there are still more entries in the list
                builder.append(", ");
            }
        }
        return builder.append('}').toString();
    }
}
