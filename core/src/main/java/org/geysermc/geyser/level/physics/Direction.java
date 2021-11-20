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

package org.geysermc.geyser.level.physics;

import com.github.steveice10.mc.protocol.data.game.level.block.value.PistonValue;
import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;

import javax.annotation.Nonnull;

public enum Direction {
    DOWN(1, Vector3i.from(0, -1, 0), Axis.Y, PistonValue.DOWN),
    UP(0, Vector3i.UNIT_Y, Axis.Y, PistonValue.UP),
    NORTH(3, Vector3i.from(0, 0, -1), Axis.Z, PistonValue.NORTH),
    SOUTH(2, Vector3i.UNIT_Z, Axis.Z, PistonValue.SOUTH),
    WEST(5, Vector3i.from(-1, 0, 0), Axis.X, PistonValue.WEST),
    EAST(4, Vector3i.UNIT_X, Axis.X, PistonValue.EAST);

    public static final Direction[] VALUES = values();

    private final int reversedId;
    @Getter
    private final Vector3i unitVector;
    @Getter
    private final Axis axis;
    @Getter
    private final PistonValue pistonValue;

    Direction(int reversedId, Vector3i unitVector, Axis axis, PistonValue pistonValue) {
        this.reversedId = reversedId;
        this.unitVector = unitVector;
        this.axis = axis;
        this.pistonValue = pistonValue;
    }

    public Direction reversed() {
        return VALUES[reversedId];
    }

    public boolean isVertical() {
        return axis == Axis.Y;
    }

    public boolean isHorizontal() {
        return axis == Axis.X || axis == Axis.Z;
    }

    @Nonnull
    public static Direction fromPistonValue(PistonValue pistonValue) {
        for (Direction direction : VALUES) {
            if (direction.pistonValue == pistonValue) {
                return direction;
            }
        }
        throw new IllegalStateException();
    }
}
