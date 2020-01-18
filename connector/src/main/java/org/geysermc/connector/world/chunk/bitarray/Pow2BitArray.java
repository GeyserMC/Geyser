/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * This code in this file is derived from NukkitX and permission has
 * been granted to us allowing the usage of it in Geyser.
 *
 * Copyright (C) 2020 The NukkitX Project
 * https://github.com/NukkitX/Nukkit
 */

package org.geysermc.connector.world.chunk.bitarray;

import com.nukkitx.network.util.Preconditions;
import org.geysermc.connector.utils.MathUtils;

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