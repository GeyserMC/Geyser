package org.geysermc.connector.world.chunk.palette;

import org.geysermc.connector.utils.MathUtils;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public enum PaletteVersion {

    V16(16, 2, null),
    V8(8, 4, V16),
    V6(6, 5, V8), // 2 bit padding
    V5(5, 6, V6), // 2 bit padding
    V4(4, 8, V5),
    V3(3, 10, V4), // 2 bit padding
    V2(2, 16, V3),
    V1(1, 32, V2);

    final byte bits;
    final byte entriesPerWord;
    final int maxEntryValue;
    final PaletteVersion next;

    PaletteVersion(int bits, int entriesPerWord, PaletteVersion next) {
        this.bits = (byte) bits;
        this.entriesPerWord = (byte) entriesPerWord;
        this.maxEntryValue = (1 << this.bits) - 1;
        this.next = next;
    }

    public Palette createPalette(int size) {
        return this.createPalette(size, new int[MathUtils.ceil((float) size / entriesPerWord)]);
    }

    public byte getVersion() {
        return bits;
    }

    public int getMaxEntryValue() {
        return maxEntryValue;
    }

    public PaletteVersion next() {
        return next;
    }

    public Palette createPalette(int size, int[] words) {
        if (this == V3 || this == V5 || this == V6) {
            // Padded palettes aren't able to use bitwise operations due to their padding.
            return new PaddedPalette(this, size, words);
        } else {
            return new Pow2Palette(this, size, words);
        }
    }

    private static PaletteVersion getVersion(int version, boolean read) {
        for (PaletteVersion ver : values()) {
            if ( ( !read && ver.entriesPerWord <= version ) || ( read && ver.bits == version ) ) {
                return ver;
            }
        }
        throw new IllegalArgumentException("Invalid palette version: " + version);
    }
}
