/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world.chunk;

import com.nukkitx.network.util.Preconditions;
import io.netty.buffer.ByteBuf;
import lombok.Synchronized;

public class ChunkSection {

    private static final int CHUNK_SECTION_VERSION = 8;
    public static final int SIZE = 4096;

    private final BlockStorage[] storage;
    private final NibbleArray blockLight;
    private final NibbleArray skyLight;

    public ChunkSection() {
        this(new BlockStorage[]{new BlockStorage(), new BlockStorage()}, new NibbleArray(SIZE),
                new NibbleArray(SIZE));
    }

    public ChunkSection(BlockStorage[] blockStorage) {
        this(blockStorage, new NibbleArray(SIZE), new NibbleArray(SIZE));
    }

    public ChunkSection(BlockStorage[] storage, byte[] blockLight, byte[] skyLight) {
        Preconditions.checkNotNull(storage, "storage");
        Preconditions.checkArgument(storage.length > 1, "Block storage length must be at least 2");
        for (BlockStorage blockStorage : storage) {
            Preconditions.checkNotNull(blockStorage, "storage");
        }

        this.storage = storage;
        this.blockLight = new NibbleArray(blockLight);
        this.skyLight = new NibbleArray(skyLight);
    }

    private ChunkSection(BlockStorage[] storage, NibbleArray blockLight, NibbleArray skyLight) {
        this.storage = storage;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
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

    @Synchronized("skyLight")
    public byte getSkyLight(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.skyLight.get(blockPosition(x, y, z));
    }

    @Synchronized("skyLight")
    public void setSkyLight(int x, int y, int z, byte val) {
        checkBounds(x, y, z);
        this.skyLight.set(blockPosition(x, y, z), val);
    }

    @Synchronized("blockLight")
    public byte getBlockLight(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.blockLight.get(blockPosition(x, y, z));
    }

    @Synchronized("blockLight")
    public void setBlockLight(int x, int y, int z, byte val) {
        checkBounds(x, y, z);
        this.blockLight.set(blockPosition(x, y, z), val);
    }

    public void writeToNetwork(ByteBuf buffer) {
        buffer.writeByte(CHUNK_SECTION_VERSION);
        buffer.writeByte(this.storage.length);
        for (BlockStorage blockStorage : this.storage) {
            blockStorage.writeToNetwork(buffer);
        }
    }

    public NibbleArray getSkyLightArray() {
        return skyLight;
    }

    public NibbleArray getBlockLightArray() {
        return blockLight;
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

    public ChunkSection copy() {
        BlockStorage[] storage = new BlockStorage[this.storage.length];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = this.storage[i].copy();
        }
        return new ChunkSection(storage, skyLight.copy(), blockLight.copy());
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
