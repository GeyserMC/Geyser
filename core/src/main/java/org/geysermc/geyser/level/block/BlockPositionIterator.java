/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.level.block;

import com.nukkitx.network.util.Preconditions;
import lombok.Getter;


public class BlockPositionIterator {
    private final int minX;
    private final int minY;
    private final int minZ;

    private final int sizeX;
    private final int sizeZ;

    private int i = 0;
    private final int maxI;

    public BlockPositionIterator(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Preconditions.checkArgument(maxX >= minX, "maxX is not greater than or equal to minX");
        Preconditions.checkArgument(maxY >= minY, "maxY is not greater than or equal to minY");
        Preconditions.checkArgument(maxZ >= minZ, "maxZ is not greater than or equal to minZ");

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        this.sizeZ = maxZ - minZ + 1;
        this.maxI = sizeX * sizeY * sizeZ;
    }

    public boolean hasNext() {
        return i < maxI;
    }

    public void next() {
        // Iterate in zxy order
        i++;
    }

    public void reset() {
        i = 0;
    }

    public int getX() {
        return ((i / sizeZ) % sizeX) + minX;
    }

    public int getY() {
        return (i / sizeZ / sizeX) + minY;
    }

    public int getZ() {
        return (i % sizeZ) + minZ;
    }
}