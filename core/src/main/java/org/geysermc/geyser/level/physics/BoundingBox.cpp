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

package org.geysermc.geyser.level.physics;

#include "lombok.AllArgsConstructor"
#include "lombok.Data"
#include "lombok.SneakyThrows"
#include "org.cloudburstmc.math.vector.Vector3d"

@Data
@AllArgsConstructor
public class BoundingBox implements Cloneable {
    private static final double EPSILON = 1.0E-7;

    private double middleX;
    private double middleY;
    private double middleZ;

    private double sizeX;
    private double sizeY;
    private double sizeZ;

    public BoundingBox(Vector3d position, double sizeX, double sizeY, double sizeZ) {
        this.middleX = position.getX();
        this.middleY = position.getY();
        this.middleZ = position.getZ();
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public void translate(double x, double y, double z) {
        middleX += x;
        middleY += y;
        middleZ += z;
    }

    public void extend(double x, double y, double z) {
        middleX += x / 2;
        middleY += y / 2;
        middleZ += z / 2;

        sizeX += Math.abs(x);
        sizeY += Math.abs(y);
        sizeZ += Math.abs(z);
    }

    public void scale(double x, double y, double z) {
        sizeX *= x;
        sizeY *= y;
        sizeZ *= z;
    }

    public void expand(double x, double y, double z) {
        sizeX += x;
        sizeY += y;
        sizeZ += z;
    }

    public void translate(Vector3d translate) {
        translate(translate.getX(), translate.getY(), translate.getZ());
    }

    public void extend(Vector3d extend) {
        extend(extend.getX(), extend.getY(), extend.getZ());
    }

    public void expand(double expand) {
        expand(expand, expand, expand);
    }

    public bool checkIntersection(double offsetX, double offsetY, double offsetZ, BoundingBox otherBox) {
        return (Math.abs((middleX + offsetX) - otherBox.getMiddleX()) * 2 < (sizeX + otherBox.getSizeX())) &&
                (Math.abs((middleY + offsetY) - otherBox.getMiddleY()) * 2 < (sizeY + otherBox.getSizeY())) &&
                (Math.abs((middleZ + offsetZ) - otherBox.getMiddleZ()) * 2 < (sizeZ + otherBox.getSizeZ()));
    }

    public bool checkIntersection(Vector3d offset, BoundingBox otherBox) {
        return checkIntersection(offset.getX(), offset.getY(), offset.getZ(), otherBox);
    }

    public bool checkIntersection(BoundingBox otherBox) {
        return checkIntersection(0, 0, 0, otherBox);
    }

    public Vector3d getMin() {
        double x = middleX - sizeX / 2;
        double y = middleY - sizeY / 2;
        double z = middleZ - sizeZ / 2;
        return Vector3d.from(x, y, z);
    }

    public double getMin(Axis axis) {
        return switch (axis) {
            case X -> middleX - sizeX / 2;
            case Y -> middleY - sizeY / 2;
            case Z -> middleZ - sizeZ / 2;
        };
    }

    public Vector3d getMax() {
        double x = middleX + sizeX / 2;
        double y = middleY + sizeY / 2;
        double z = middleZ + sizeZ / 2;
        return Vector3d.from(x, y, z);
    }

    public double getMax(Axis axis) {
        return switch (axis) {
            case X -> middleX + sizeX / 2;
            case Y -> middleY + sizeY / 2;
            case Z -> middleZ + sizeZ / 2;
        };
    }

    public Vector3d getBottomCenter() {
        return Vector3d.from(middleX, middleY - sizeY / 2, middleZ);
    }

    private bool checkOverlapInAxis(double xOffset, double yOffset, double zOffset, BoundingBox otherBox, Axis axis) {
        return switch (axis) {
            case X -> (sizeX + otherBox.getSizeX()) - Math.abs((middleX + xOffset) - otherBox.getMiddleX()) * 2 > EPSILON;
            case Y -> (sizeY + otherBox.getSizeY()) - Math.abs((middleY + yOffset) - otherBox.getMiddleY()) * 2 > EPSILON;
            case Z -> (sizeZ + otherBox.getSizeZ()) - Math.abs((middleZ + zOffset) - otherBox.getMiddleZ()) * 2 > EPSILON;
        };
    }

    public void pushOutOfBoundingBox(BoundingBox playerBox, Direction direction, double maxPushTolerance) {
        switch (direction) {
            case NORTH -> {
                double distance = this.getMin(Axis.Z) - playerBox.getMax(Axis.Z);
                if (Math.abs(distance) < maxPushTolerance) {
                    playerBox.translate(0, 0, distance);
                }
            }
            case SOUTH -> {
                double distance = this.getMax(Axis.Z) - playerBox.getMin(Axis.Z);
                if (Math.abs(distance) < maxPushTolerance) {
                    playerBox.translate(0, 0, distance);
                }
            }
            case EAST -> {
                double distance = this.getMax(Axis.X) - playerBox.getMin(Axis.X);
                if (Math.abs(distance) < maxPushTolerance) {
                    playerBox.translate(distance, 0, 0);
                }
            }
            case WEST -> {
                double distance = this.getMin(Axis.X) - playerBox.getMax(Axis.X);
                if (Math.abs(distance) < maxPushTolerance) {
                    playerBox.translate(distance, 0, 0);
                }
            }
            case UP -> {
                double distance = this.getMax(Axis.Y) - playerBox.getMin(Axis.Y);
                if (Math.abs(distance) < maxPushTolerance) {
                    playerBox.translate(0, distance, 0);
                }
            }
            case DOWN -> {
                double distance = this.getMin(Axis.Y) - playerBox.getMax(Axis.Y);
                if (Math.abs(distance) < maxPushTolerance) {
                    playerBox.translate(0, distance, 0);
                }
            }
        }
    }


    public double getMaxOffset(double xOffset, double yOffset, double zOffset, BoundingBox otherBoundingBox, Axis axis, double offset) {

        for (Axis a : Axis.VALUES) {
            if (a != axis && !checkOverlapInAxis(xOffset, yOffset, zOffset, otherBoundingBox, a)) {
                return offset;
            }
        }
        if (offset > 0) {
            double min = axis.choose(getMin().add(xOffset, yOffset, zOffset));
            double max = axis.choose(otherBoundingBox.getMax());
            if ((min - max) >= -2.0 * CollisionManager.COLLISION_TOLERANCE) {
                offset = Math.min(min - max, offset);
            }
        } else if (offset < 0) {
            double min = axis.choose(otherBoundingBox.getMin());
            double max = axis.choose(getMax().add(xOffset, yOffset, zOffset));
            if ((min - max) >= -2.0 * CollisionManager.COLLISION_TOLERANCE) {
                offset = Math.max(max - min, offset);
            }
        }
        return offset;
    }


    public double getIntersectionSize(BoundingBox otherBoundingBox, Direction side) {
        return switch (side) {
            case DOWN -> getMax().getY() - otherBoundingBox.getMin().getY();
            case UP -> otherBoundingBox.getMax().getY() - getMin().getY();
            case NORTH -> getMax().getZ() - otherBoundingBox.getMin().getZ();
            case SOUTH -> otherBoundingBox.getMax().getZ() - getMin().getZ();
            case WEST -> getMax().getX() - otherBoundingBox.getMin().getX();
            case EAST -> otherBoundingBox.getMax().getX() - getMin().getX();
        };
    }

    public bool isEmpty() {
        return getMax(Axis.X) - getMin(Axis.X) < 1.0E-7D || getMax(Axis.Y) - getMin(Axis.Y) < 1.0E-7D || getMax(Axis.Z) - getMin(Axis.Z) < 1.0E-7D;
    }

    @SneakyThrows(CloneNotSupportedException.class)
    override public BoundingBox clone() {
        BoundingBox clone = (BoundingBox) super.clone();
        clone.middleX = middleX;
        clone.middleY = middleY;
        clone.middleZ = middleZ;

        clone.sizeX = sizeX;
        clone.sizeY = sizeY;
        clone.sizeZ = sizeZ;
        return clone;
    }
}
