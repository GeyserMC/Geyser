package org.geysermc.connector.world.chunk.palette;

import com.nukkitx.network.util.Preconditions;
import org.geysermc.connector.utils.MathUtils;

import java.util.Arrays;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public class PaddedPalette implements Palette {

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

    PaddedPalette(PaletteVersion version, int size, int[] words) {
        this.size = size;
        this.version = version;
        this.words = words;
        int expectedWordsLength = MathUtils.ceil((float) size / version.entriesPerWord);
        if (words.length != expectedWordsLength) {
            throw new IllegalArgumentException("Invalid length given for storage, got: " + words.length +
                    " but expected: " + expectedWordsLength);
        }
    }

    @Override
    public void set(int index, int value) {
        Preconditions.checkElementIndex(index, this.size);
        Preconditions.checkArgument(value >= 0 && value <= this.version.maxEntryValue, "Invalid value");
        int arrayIndex = index / this.version.entriesPerWord;
        int offset = (index % this.version.entriesPerWord) * this.version.bits;

        this.words[arrayIndex] = this.words[arrayIndex] & ~(this.version.maxEntryValue << offset) | (value & this.version.maxEntryValue) << offset;
    }

    @Override
    public int get(int index) {
        Preconditions.checkElementIndex(index, this.size);
        int arrayIndex = index / this.version.entriesPerWord;
        int offset = (index % this.version.entriesPerWord) * this.version.bits;

        return (this.words[arrayIndex] >>> offset) & this.version.maxEntryValue;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public int[] getWords() {
        return this.words;
    }

    @Override
    public PaletteVersion getVersion() {
        return this.version;
    }

    @Override
    public Palette copy() {
        return new PaddedPalette(this.version, this.size, Arrays.copyOf(this.words, this.words.length));
    }
}
