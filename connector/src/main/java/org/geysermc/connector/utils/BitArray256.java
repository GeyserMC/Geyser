package org.geysermc.connector.utils;

/**
 * This file property the NukkitX project
 * https://github.com/NukkitX/Nukkit
 * @author https://github.com/boy0001/
 */
public final class BitArray256 {
    private final int bitsPerEntry;
    public final long[] data;

    public BitArray256(int bitsPerEntry) {
        this.bitsPerEntry = bitsPerEntry;
        int longLen = (this.bitsPerEntry * 256) >> 6;
        this.data = new long[longLen];
    }

    public BitArray256(BitArray256 other) {
        this.bitsPerEntry = other.bitsPerEntry;
        this.data = other.data.clone();
    }

    public final void setAt(int index, int value) {
        int bitIndexStart = index * bitsPerEntry;
        int longIndexStart = bitIndexStart >> 6;
        int localBitIndexStart = bitIndexStart & 63;
        this.data[longIndexStart] = this.data[longIndexStart] & ~((long) ((1 << bitsPerEntry) - 1) << localBitIndexStart) | ((long) value) << localBitIndexStart;

        if(localBitIndexStart > 64 - bitsPerEntry) {
            int longIndexEnd = longIndexStart + 1;
            int localShiftStart = 64 - localBitIndexStart;
            int localShiftEnd = bitsPerEntry - localShiftStart;
            this.data[longIndexEnd] = this.data[longIndexEnd] >>> localShiftEnd << localShiftEnd | (((long) value) >> localShiftStart);
        }
    }

    public final int getAt(int index) {
        int bitIndexStart = index * bitsPerEntry;

        int longIndexStart = bitIndexStart >> 6;

        int localBitIndexStart = bitIndexStart & 63;
        if(localBitIndexStart <= 64 - bitsPerEntry) {
            return (int)(this.data[longIndexStart] >>> localBitIndexStart & ((1 << bitsPerEntry) - 1));
        } else {
            int localShift = 64 - localBitIndexStart;
            return (int) ((this.data[longIndexStart] >>> localBitIndexStart | this.data[longIndexStart + 1] << localShift) & ((1 << bitsPerEntry) - 1));
        }
    }

    public final void fromRaw(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            setAt(i, arr[i]);
        }
    }

    public BitArray256 grow(int newBitsPerEntry) {
        int amtGrow = newBitsPerEntry - this.bitsPerEntry;
        if (amtGrow <= 0) return this;
        BitArray256 newBitArray = new BitArray256(newBitsPerEntry);

        int[] buffer = ThreadCache.intCache256.get();
        toRaw(buffer);
        newBitArray.fromRaw(buffer);

        return newBitArray;
    }

    public BitArray256 growSlow(int bitsPerEntry) {
        BitArray256 newBitArray = new BitArray256(bitsPerEntry);
        for (int i = 0; i < 256; i++) {
            newBitArray.setAt(i, getAt(i));
        }
        return newBitArray;
    }

    public final int[] toRaw(int[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = getAt(i);
        }
        return buffer;
    }

    public final int[] toRaw() {
        return toRaw(new int[256]);
    }

    public BitArray256 clone() {
        return new BitArray256(this);
    }
}
