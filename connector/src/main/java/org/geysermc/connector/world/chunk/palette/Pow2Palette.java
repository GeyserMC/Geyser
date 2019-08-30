package org.geysermc.connector.world.chunk.palette;

import com.nukkitx.network.util.Preconditions;
import org.geysermc.connector.utils.MathUtils;

import java.util.Arrays;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public class Pow2Palette implements Palette {

    /**
     * Array used to store data
     */
    private final int[] words;

    /**
     * Palette version information
     */
    private final PaletteVersion version;

    /**
     * Number of entries in this palette (<b>not</b> the length of the words array that internally backs this palette)
     */
    private final int size;

    Pow2Palette(PaletteVersion version, int size, int[] words) {
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
        Preconditions.checkArgument(value >= 0 && value <= this.version.maxEntryValue, "Invalid value");
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

    public PaletteVersion getVersion() {
        return version;
    }

    @Override
    public Palette copy() {
        return new Pow2Palette(this.version, this.size, Arrays.copyOf(this.words, this.words.length));
    }
}