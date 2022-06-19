/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event.world;

import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.event.Cancellable;
import org.geysermc.geyser.api.event.Event;

public class GeyserConvertSkullEvent implements Event, Cancellable {
    private final int x;
    private final int y;
    private final int z;
    
    private final boolean onFloor;
    
    private final WallDirection wallDirection;
    private final int floorDirection;
    
    private final String skinHash;

    private boolean cancelled;

    private CustomBlockState newBlockState;

    public GeyserConvertSkullEvent(int x, int y, int z, boolean onFloor, WallDirection wallDirection, int floorDirection, String skinHash) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.onFloor = onFloor;
        this.wallDirection = wallDirection;
        this.floorDirection = floorDirection;

        if (onFloor && (wallDirection != WallDirection.INVALID || floorDirection == -1)) {
            throw new IllegalArgumentException("Skull can't be on the floor and wall at the same time");
        } else if (!onFloor && (wallDirection == WallDirection.INVALID || floorDirection != -1)) {
            throw new IllegalArgumentException("Skull can't be on the floor and wall at the same time");
        }

        this.skinHash = skinHash;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public boolean onFloor() {
        return onFloor;
    }

    public WallDirection wallDirection() {
        return wallDirection;
    }

    public int floorDirection() {
        return floorDirection;
    }

    public String skinHash() {
        return skinHash;
    }

    public void replaceWithBlock(CustomBlockState blockState) {
        this.newBlockState = blockState;
    }

    public CustomBlockState getNewBlockState() {
        return newBlockState;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    public enum WallDirection {
        NORTH,
        EAST,
        SOUTH,
        WEST,
        INVALID

    }
}
