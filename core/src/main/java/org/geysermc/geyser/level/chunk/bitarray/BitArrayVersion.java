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

import org.geysermc.geyser.util.MathUtils;

public enum BitArrayVersion {
    V16(16, 2, null),
    V8(8, 4, V16),
    V6(6, 5, V8), // 2 bit padding
    V5(5, 6, V6), // 2 bit padding
    V4(4, 8, V5),
    V3(3, 10, V4), // 2 bit padding
    V2(2, 16, V3),
    V1(1, 32, V2),
    V0(0, 0, V1);

    private static final BitArrayVersion[] VALUES = values();

    final byte bits;
    final byte entriesPerWord;
    final int maxEntryValue;
    final BitArrayVersion next;

    BitArrayVersion(int bits, int entriesPerWord, BitArrayVersion next) {
        this.bits = (byte) bits;
        this.entriesPerWord = (byte) entriesPerWord;
        this.maxEntryValue = (1 << this.bits) - 1;
        this.next = next;
    }

    public static BitArrayVersion get(int version, boolean read) {
        for (BitArrayVersion ver : values()) {
            if ((!read && ver.entriesPerWord <= version) || (read && ver.bits == version)) {
                return ver;
            }
        }
        throw new IllegalArgumentException("Invalid palette version: " + version);
    }

    public static BitArrayVersion forBitsCeil(int bits) {
        for (int i = VALUES.length - 1; i >= 0; i--)  {
            BitArrayVersion version = VALUES[i];
            if (version.bits >= bits)   {
                return version;
            }
        }
        return null;
    }

    public byte getId() {
        return bits;
    }

    public int getMaxEntryValue() {
        return maxEntryValue;
    }

    public int getWordsForSize(int size) {
        return MathUtils.ceil((float) size / entriesPerWord);
    }

    public BitArrayVersion next() {
        return next;
    }

    public BitArray createArray(int size) {
        return this.createArray(size, new int[MathUtils.ceil((float) size / entriesPerWord)]);
    }

    public BitArray createArray(int size, int[] words) {
        if (this == V3 || this == V5 || this == V6) {
            // Padded palettes aren't able to use bitwise operations due to their padding.
            return new PaddedBitArray(this, size, words);
        } else if (this == V0) {
            return new SingletonBitArray();
        } else {
            return new Pow2BitArray(this, size, words);
        }
    }
}
