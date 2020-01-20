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

public interface BitArray {

    void set(int index, int value);

    int get(int index);

    int size();

    int[] getWords();

    BitArrayVersion getVersion();

    BitArray copy();
}