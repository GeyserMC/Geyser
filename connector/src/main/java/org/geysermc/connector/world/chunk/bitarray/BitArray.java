package org.geysermc.connector.world.chunk.bitarray;

/**
 * Adapted from NukkitX: https://github.com/NukkitX/Nukkit
 */
public interface BitArray {

    void set(int index, int value);

    int get(int index);

    int size();

    int[] getWords();

    BitArrayVersion getVersion();

    BitArray copy();
}