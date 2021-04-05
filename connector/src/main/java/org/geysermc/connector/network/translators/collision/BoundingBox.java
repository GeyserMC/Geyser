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

package org.geysermc.connector.network.translators.collision;

import com.nukkitx.math.vector.Vector3d;
import lombok.*;
import org.geysermc.connector.utils.Axis;

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

    private boolean checkOverlapInAxis(double offsetX, double offsetY, double offsetZ, BoundingBox otherBox, Axis axis) {
        switch (axis) {
            case X:
                return Math.abs((middleX + offsetX) - otherBox.getMiddleX()) * 2 < (sizeX + otherBox.getSizeX());
            case Y:
                return Math.abs((middleY + offsetY) - otherBox.getMiddleY()) * 2 < (sizeY + otherBox.getSizeY());
            case Z:
                return Math.abs((middleZ + offsetZ) - otherBox.getMiddleZ()) * 2 < (sizeZ + otherBox.getSizeZ());
        }
        return false;
    }

    /**
     * Find the maximum offset of another bounding box in an axis, so that it won't collide with this bounding box
     *
     * @param offsetX The offset of this bounding box in the X axis
     * @param offsetY The offset of this bounding box in the Y axis
     * @param offsetZ The offset of this bounding box in the Z axis
     * @param otherBoundingBox The bounding box that is moving
     * @param axis The axis of movement
     * @param offset The current max offset
     * @return The new max offset
     */
    public double getMaxOffset(double offsetX, double offsetY, double offsetZ, BoundingBox otherBoundingBox, Axis axis, double offset) {
        // Make sure that the bounding box overlaps in the other axes
        switch (axis) {
            case X:
                if (!checkOverlapInAxis(offsetX, offsetY, offsetZ, otherBoundingBox, Axis.Y) ||
                        !checkOverlapInAxis(offsetX, offsetY, offsetZ, otherBoundingBox, Axis.Z)) {
                    return offset;
                }
                break;
            case Y:
                if (!checkOverlapInAxis(offsetX, offsetY, offsetZ, otherBoundingBox, Axis.X) ||
                        !checkOverlapInAxis(offsetX, offsetY, offsetZ, otherBoundingBox, Axis.Z)) {
                    return offset;
                }
                break;
            case Z:
                if (!checkOverlapInAxis(offsetX, offsetY, offsetZ, otherBoundingBox, Axis.X) ||
                        !checkOverlapInAxis(offsetX, offsetY, offsetZ, otherBoundingBox, Axis.Y)) {
                    return offset;
                }
                break;
        }
        if (offset > 0) {
            double min = axis.choose(this.getMin().add(Vector3d.from(offsetX, offsetY, offsetZ)));
            double max = axis.choose(otherBoundingBox.getMax());
            if ((min - max) >= -2.0 * CollisionManager.COLLISION_TOLERANCE) {
                offset = Math.min(min - max, offset);
            }
        } else if (offset < 0) {
            double min = axis.choose(otherBoundingBox.getMin());
            double max = axis.choose(this.getMax().add(Vector3d.from(offsetX, offsetY, offsetZ)));
            if ((min - max) >= -2.0 * CollisionManager.COLLISION_TOLERANCE) {
                offset = Math.max(max - min, offset);
            }
        }
        return offset;
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
