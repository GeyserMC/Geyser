package org.geysermc.connector.world.chunk.palette;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public interface Palette {

    void set(int index, int value);

    int get(int index);

    int size();

    int[] getWords();

    PaletteVersion getVersion();

    Palette copy();
}