/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.chunk.bitarray;

import com.nukkitx.network.util.Preconditions;
import org.geysermc.geyser.util.MathUtils;

import java.util.Arrays;

public class Pow2BitArray implements BitArray {

    /**
     * Array used to store data
     */
    private final int[] words;

    /**
     * Palette version information
     */
    private final BitArrayVersion version;

    /**
     * Number of entries in this palette (<b>not</b> the length of the words array that internally backs this palette)
     */
    private final int size;

    Pow2BitArray(BitArrayVersion version, int size, int[] words) {
        this.size = size;
        this.version = version;
        this.words = words;
        int expectedWordsLength = MathUtils.ceil((float) size / version.entriesPerWord);
        if (words.length != expectedWordsLength) {
            throw new IllegalArgumentException("Invalid length given for storage, got: " + words.length +
                    " but expected: " + expectedWordsLength);
        }
    }

    /**
     * Sets the entry at the given location to the given value
     */
    public void set(int index, int value) {
        Preconditions.checkElementIndex(index, this.size);
        Preconditions.checkArgument(value >= 0 && value <= this.version.maxEntryValue, "Invalid value %s", value);
        int bitIndex = index * this.version.bits;
        int arrayIndex = bitIndex >> 5;
        int offset = bitIndex & 31;
        this.words[arrayIndex] = this.words[arrayIndex] & ~(this.version.maxEntryValue << offset) | (value & this.version.maxEntryValue) << offset;
    }

    /**
     * Gets the entry at the given index
     */
    public int get(int index) {
        Preconditions.checkElementIndex(index, this.size);
        int bitIndex = index * this.version.bits;
        int arrayIndex = bitIndex >> 5;
        int wordOffset = bitIndex & 31;
        return this.words[arrayIndex] >>> wordOffset & this.version.maxEntryValue;
    }

    /**
     * Gets the long array that is used to store the data in this BitArray. This is useful for sending packet data.
     */
    public int size() {
        return this.size;
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int[] getWords() {
        return this.words;
    }

    public BitArrayVersion getVersion() {
        return version;
    }

    @Override
    public BitArray copy() {
        return new Pow2BitArray(this.version, this.size, Arrays.copyOf(this.words, this.words.length));
    }
}