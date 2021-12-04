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

import com.nukkitx.math.vector.Vector3d;
import lombok.*;

@Data
@AllArgsConstructor
public class BoundingBox implements Cloneable {
    private double middleX;
    private double middleY;
    private double middleZ;

    private double sizeX;
    private double sizeY;
    private double sizeZ;

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

    public void extend(Vector3d extend) {
        extend(extend.getX(), extend.getY(), extend.getZ());
    }

    public boolean checkIntersection(double offsetX, double offsetY, double offsetZ, BoundingBox otherBox) {
        return (Math.abs((middleX + offsetX) - otherBox.getMiddleX()) * 2 < (sizeX + otherBox.getSizeX())) &&
                (Math.abs((middleY + offsetY) - otherBox.getMiddleY()) * 2 < (sizeY + otherBox.getSizeY())) &&
                (Math.abs((middleZ + offsetZ) - otherBox.getMiddleZ()) * 2 < (sizeZ + otherBox.getSizeZ()));
    }

    public boolean checkIntersection(Vector3d offset, BoundingBox otherBox) {
        return checkIntersection(offset.getX(), offset.getY(), offset.getZ(), otherBox);
    }

    public Vector3d getMin() {
        double x = middleX - sizeX / 2;
        double y = middleY - sizeY / 2;
        double z = middleZ - sizeZ / 2;
        return Vector3d.from(x, y, z);
    }

    public Vector3d getMax() {
        double x = middleX + sizeX / 2;
        double y = middleY + sizeY / 2;
        double z = middleZ + sizeZ / 2;
        return Vector3d.from(x, y, z);
    }

    public Vector3d getBottomCenter() {
        return Vector3d.from(middleX, middleY - sizeY / 2, middleZ);
    }

    private boolean checkOverlapInAxis(double xOffset, double yOffset, double zOffset, BoundingBox otherBox, Axis axis) {
        return switch (axis) {
            case X -> Math.abs((middleX + xOffset) - otherBox.getMiddleX()) * 2 < (sizeX + otherBox.getSizeX());
            case Y -> Math.abs((middleY + yOffset) - otherBox.getMiddleY()) * 2 < (sizeY + otherBox.getSizeY());
            case Z -> Math.abs((middleZ + zOffset) - otherBox.getMiddleZ()) * 2 < (sizeZ + otherBox.getSizeZ());
        };
    }

    /**
     * Find the maximum offset of another bounding box in an axis that will not collide with this bounding box
     *
     * @param xOffset The x offset of this bounding box
     * @param yOffset The y offset of this bounding box
     * @param zOffset The z offset of this bounding box
     * @param otherBoundingBox The bounding box that is moving
     * @param axis The axis of movement
     * @param offset The current max offset
     * @return The new max offset
     */
    public double getMaxOffset(double xOffset, double yOffset, double zOffset, BoundingBox otherBoundingBox, Axis axis, double offset) {
        // Make sure that the bounding box overlaps in the other axes
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

    /**
     * Get the distance required to move this bounding box to one of otherBoundingBox's sides
     *
     * @param otherBoundingBox The stationary bounding box
     * @param side The side of otherBoundingBox to snap this bounding box to
     * @return The distance to move in the direction of {@code side}
     */
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

    @SneakyThrows(CloneNotSupportedException.class)
    @Override
    public BoundingBox clone() {
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
