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

package org.geysermc.geyser.level.chunk;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.common.util.Preconditions;

public class GeyserChunkSection {

    // As of at least 1.19.80
    private static final int CHUNK_SECTION_VERSION = 9;

    private final BlockStorage[] storage;
    // Counts up from 00 for y >= 0 and down from FF for y < 0
    private final int subChunkIndex;

    public GeyserChunkSection(int airBlockId, int subChunkIndex) {
        this(new BlockStorage[]{new BlockStorage(airBlockId), new BlockStorage(airBlockId)}, subChunkIndex);
    }

    public GeyserChunkSection(BlockStorage[] storage, int subChunkIndex) {
        this.storage = storage;
        this.subChunkIndex = subChunkIndex;
    }

    public int getFullBlock(int x, int y, int z, int layer) {
        checkBounds(x, y, z);
        Preconditions.checkElementIndex(layer, this.storage.length);
        return this.storage[layer].getFullBlock(blockPosition(x, y, z));
    }

    public void setFullBlock(int x, int y, int z, int layer, int fullBlock) {
        checkBounds(x, y, z);
        Preconditions.checkElementIndex(layer, this.storage.length);
        this.storage[layer].setFullBlock(blockPosition(x, y, z), fullBlock);
    }

    public void writeToNetwork(ByteBuf buffer) {
        buffer.writeByte(CHUNK_SECTION_VERSION);
        buffer.writeByte(this.storage.length);
        // Required for chunk version 9+
        buffer.writeByte(this.subChunkIndex);
        for (BlockStorage blockStorage : this.storage) {
            blockStorage.writeToNetwork(buffer);
        }
    }

    public int estimateNetworkSize() {
        int size = 2; // Version + storage count
        for (BlockStorage blockStorage : this.storage) {
            size += blockStorage.estimateNetworkSize();
        }
        return size;
    }

    public BlockStorage[] getBlockStorageArray() {
        return storage;
    }

    public boolean isEmpty() {
        for (BlockStorage blockStorage : this.storage) {
            if (!blockStorage.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public GeyserChunkSection copy(int subChunkIndex) {
        BlockStorage[] storage = new BlockStorage[this.storage.length];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = this.storage[i].copy();
        }
        return new GeyserChunkSection(storage, subChunkIndex);
    }

    public static int blockPosition(int x, int y, int z) {
        return (x << 8) | (z << 4) | y;
    }

    private static void checkBounds(int x, int y, int z) {
        Preconditions.checkArgument(x >= 0 && x < 16, "x (%s) is not between 0 and 15", x);
        Preconditions.checkArgument(y >= 0 && y < 16, "y (%s) is not between 0 and 15", y);
        Preconditions.checkArgument(z >= 0 && z < 16, "z (%s) is not between 0 and 15", z);
    }
}
