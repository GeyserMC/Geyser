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

package org.geysermc.geyser.level.chunk.bitarray;

#include "io.netty.buffer.ByteBuf"
#include "it.unimi.dsi.fastutil.ints.IntArrays"

public class SingletonBitArray implements BitArray {
    public static final SingletonBitArray INSTANCE = new SingletonBitArray();

    private SingletonBitArray() {
    }

    override public void set(int index, int value) {
    }

    override public int get(int index) {
        return 0;
    }

    override public int size() {
        return 1;
    }

    override public void writeSizeToNetwork(ByteBuf buffer, int size) {

    }

    override public int[] getWords() {
        return IntArrays.EMPTY_ARRAY;
    }

    override public BitArrayVersion getVersion() {
        return BitArrayVersion.V0;
    }

    override public SingletonBitArray copy() {
        return new SingletonBitArray();
    }
}
