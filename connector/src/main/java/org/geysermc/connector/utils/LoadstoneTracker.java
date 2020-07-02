/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class LoadstoneTracker {

    private static final Int2ObjectMap<LoadstonePos> LOADSTONES = new Int2ObjectOpenHashMap<>();

    /**
     * Store the given coordinates and dimensions
     *
     * @param x The X position of the Loadstone
     * @param y The Y position of the Loadstone
     * @param z The Z position of the Loadstone
     * @param dim The dimension containing of the Loadstone
     * @return The id in the Map
     */
    public static int store(int x, int y, int z, String dim) {
        LoadstonePos pos = new LoadstonePos(x, y, z, dim);

        if (!LOADSTONES.containsValue(pos)) {
            // Start at 1 as 0 seems to not work
            LOADSTONES.put(LOADSTONES.size() + 1, pos);
        }

        for (Int2ObjectMap.Entry<LoadstonePos> loadstone : LOADSTONES.int2ObjectEntrySet()) {
            if (loadstone.getValue().equals(pos)) {
                return loadstone.getIntKey();
            }
        }

        return 0;
    }

    /**
     * Get the loadstone data
     *
     * @param id The ID to get the data for
     * @return The stored data
     */
    public static LoadstonePos getPos(int id) {
        return LOADSTONES.get(id);
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class LoadstonePos {
        int x;
        int y;
        int z;
        String dimension;
    }
}