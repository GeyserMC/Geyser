package org.geysermc.connector.world;

import org.geysermc.connector.utils.BitArray256;
import org.geysermc.connector.utils.IntPalette;
import org.geysermc.connector.utils.MathHelper;
import org.geysermc.connector.utils.ThreadCache;

import java.util.Arrays;

/**
 * This file property the NukkitX project
 * https://github.com/NukkitX/Nukkit
 */
public class BiomePalette {
    private int biome;
    private BitArray256 encodedData;
    private IntPalette palette;

    private BiomePalette(BiomePalette clone) {
        this.biome = clone.biome;
        if (clone.encodedData != null) {
            this.encodedData = clone.encodedData.clone();
            this.palette = clone.palette.clone();
        }
    }

    public BiomePalette(int[] biomeColors) {
        for (int i = 0; i < 256; i++) {
            set(i, biomeColors[i]);
        }
    }

    public BiomePalette() {
        this.biome = Integer.MIN_VALUE;
    }

    public int get(int x, int z) {
        return get(getIndex(x, z));
    }

    public synchronized int get(int index) {
        if (encodedData == null) return biome;
        return palette.getKey(encodedData.getAt(index));
    }

    public void set(int x, int z, int value) {
        set(getIndex(x, z), value);
    }

    public synchronized void set(int index, int value) {
        if (encodedData == null) {
            if (value == biome) return;
            if (biome == Integer.MIN_VALUE) {
                biome = value;
                return;
            }
            synchronized (this) {
                palette = new IntPalette();
                palette.add(biome);
                palette.add(value);
                encodedData = new BitArray256(1);
                if (value < biome) {
                    Arrays.fill(encodedData.data, -1);
                    encodedData.setAt(index, 0);
                } else {
                    encodedData.setAt(index, 1);
                }
                return;
            }
        }

        int encodedValue = palette.getValue(value);
        if (encodedValue != Integer.MIN_VALUE) {
            encodedData.setAt(index, encodedValue);
        } else {
            synchronized (this) {
                int[] raw = encodedData.toRaw(ThreadCache.intCache256.get());

                // TODO skip remapping of raw data and use grow instead if `remap`
                // boolean remap = value < palette.getValue(palette.length() - 1);

                for (int i = 0; i < 256; i++) {
                    raw[i] = palette.getKey(raw[i]);
                }

                int oldRaw = raw[4];

                raw[index] = value;

                palette.add(value);

                int oldBits = MathHelper.log2(palette.length() - 2);
                int newBits = MathHelper.log2(palette.length() - 1);
                if (oldBits != newBits) {
                    encodedData = new BitArray256(newBits);
                }

                for (int i = 0; i < raw.length; i++) {
                    raw[i] = palette.getValue(raw[i]);
                }

                encodedData.fromRaw(raw);
            }
        }

    }

    public synchronized int[] toRaw() {
        int[] buffer = ThreadCache.intCache256.get();
        if (encodedData == null) {
            Arrays.fill(buffer, biome);
        } else {
            synchronized (this) {
                buffer = encodedData.toRaw(buffer);
                for (int i = 0; i < 256; i++) {
                    buffer[i] = palette.getKey(buffer[i]);
                }
            }
        }
        return buffer;
    }

    public int getIndex(int x, int z) {
        return (z << 4) | x;
    }

    public synchronized BiomePalette clone() {
        return new BiomePalette(this);
    }
}
