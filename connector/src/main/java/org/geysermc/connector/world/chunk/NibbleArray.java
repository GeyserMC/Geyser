/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * This code in this file is derived from NukkitX and permission has
 * been granted to us allowing the usage of it in Geyser.
 *
 * Copyright (C) 2020 The NukkitX Project
 * https://github.com/NukkitX/Nukkit
 */

package org.geysermc.connector.world.chunk;

import com.nukkitx.network.util.Preconditions;

public class NibbleArray implements Cloneable {

    private final byte[] data;

    public NibbleArray(int length) {
        data = new byte[length / 2];
    }

    public NibbleArray(byte[] array) {
        data = array;
    }

    public byte get(int index) {
        Preconditions.checkElementIndex(index, data.length * 2);
        byte val = data[index / 2];
        if ((index & 1) == 0) {
            return (byte) (val & 0x0f);
        } else {
            return (byte) ((val & 0xf0) >>> 4);
        }
    }

    public void set(int index, byte value) {
        Preconditions.checkArgument(value >= 0 && value < 16, "Nibbles must have a value between 0 and 15.");
        Preconditions.checkElementIndex(index, data.length * 2);
        value &= 0xf;
        int half = index / 2;
        byte previous = data[half];
        if ((index & 1) == 0) {
            data[half] = (byte) (previous & 0xf0 | value);
        } else {
            data[half] = (byte) (previous & 0x0f | value << 4);
        }
    }

    public void fill(byte value) {
        Preconditions.checkArgument(value >= 0 && value < 16, "Nibbles must have a value between 0 and 15.");
        value &= 0xf;
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ((value << 4) | value);
        }
    }

    public void copyFrom(byte[] bytes) {
        Preconditions.checkNotNull(bytes, "bytes");
        Preconditions.checkArgument(bytes.length == data.length, "length of provided byte array is %s but expected %s", bytes.length,
                data.length);
        System.arraycopy(bytes, 0, data, 0, data.length);
    }

    public void copyFrom(NibbleArray array) {
        Preconditions.checkNotNull(array, "array");
        copyFrom(array.data);
    }

    public byte[] getData() {
        return data;
    }

    public NibbleArray copy() {
        return new NibbleArray(getData().clone());
    }
}
