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

package org.geysermc.geyser.translator.collision;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.session.GeyserSession;

@EqualsAndHashCode
public class BlockCollision {

    @Getter
    protected final BoundingBox[] boundingBoxes;

    /**
     * This is used for the step up logic.
     */
    protected double pushUpTolerance = 1;

    /**
     * This is used to control the maximum distance a face of a bounding box can push the player away
     */
    protected final double pushAwayTolerance = CollisionManager.COLLISION_TOLERANCE * 1.1;

    // Max steppable distance (beds)
    private static final double MAX_STEP = 0.5625;

    protected BlockCollision(BoundingBox[] boxes) {
        this.boundingBoxes = boxes;
    }

    /**
     * Overridden in classes like GrassPathCollision when correction code needs to be run before the
     * main correction
     */
    public void beforeCorrectPosition(int x, int y, int z, BoundingBox playerCollision) {}

    /**
     * Returns false if the movement is invalid, and in this case it shouldn't be sent to the server and should be
     * cancelled
     * While the Java server should do this, it could result in false flags by anticheat
     * This functionality is currently only used in 6 or 7 layer snow
     */
    public boolean correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        double playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() * 0.5);

        // Cache original sizes to restore exactly (not hardcoded 0.6)
        final double originalSizeX = playerCollision.getSizeX();
        final double originalSizeZ = playerCollision.getSizeZ();
        final double expandEpsilon = CollisionManager.COLLISION_TOLERANCE * 2.0;

        for (int i = 0; i < this.boundingBoxes.length; i++) {
            BoundingBox b = this.boundingBoxes[i];

            // Precompute halves for performance/readability
            final double halfBx = b.getSizeX() * 0.5;
            final double halfBy = b.getSizeY() * 0.5;
            final double halfBz = b.getSizeZ() * 0.5;

            // Absolute Y min/max of the block AABB in world coords
            final double boxMinY = (b.getMiddleY() + y) - halfBy;
            final double boxMaxY = (b.getMiddleY() + y) + halfBy;

            // 1) Step-up logic with "normal" player collision
            if (b.checkIntersection(x, y, z, playerCollision) && (playerMinY + pushUpTolerance) >= boxMinY) {
                final double step = boxMaxY - playerMinY;
                if (step <= MAX_STEP) {
                    playerCollision.translate(0, step, 0);
                    // Update for next collision box
                    playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() * 0.5);
                }
            }

            // 2) Passable fix: slightly enlarge X/Z to catch near-intersections
            playerCollision.setSizeX(originalSizeX + expandEpsilon);
            playerCollision.setSizeZ(originalSizeZ + expandEpsilon);

            // If the player still intersects the block, then push them out
            // This fixes NoCheatPlus's Passable check
            // This check doesn't allow players right up against the block, so they must be pushed slightly away
            if (b.checkIntersection(x, y, z, playerCollision)) {
                // ULP bounds for floating point safety
                final double absXMax = Math.max(Math.abs(playerCollision.getMiddleX()) + playerCollision.getSizeX() * 0.5, Math.abs(x) + 1.0);
                final double absZMax = Math.max(Math.abs(playerCollision.getMiddleZ()) + playerCollision.getSizeZ() * 0.5, Math.abs(z) + 1.0);

                final double xPushAwayTolerance = Math.max(pushAwayTolerance, Math.ulp((float) absXMax));
                final double zPushAwayTolerance = Math.max(pushAwayTolerance, Math.ulp((float) absZMax));

                // Faces of the block (relative to block origin)
                final double northFaceZ = b.getMiddleZ() - halfBz;
                final double southFaceZ = b.getMiddleZ() + halfBz;
                final double eastFaceX  = b.getMiddleX() + halfBx;
                final double westFaceX  = b.getMiddleX() - halfBx;
                final double bottomFaceY = b.getMiddleY() - halfBy;

                final double playerHalfX = playerCollision.getSizeX() * 0.5;
                final double playerHalfY = playerCollision.getSizeY() * 0.5;
                final double playerHalfZ = playerCollision.getSizeZ() * 0.5;

                // Z axis - north
                double relZ = playerCollision.getMiddleZ() - z;
                double translateDistance = northFaceZ - relZ - playerHalfZ;
                if (Math.abs(translateDistance) < zPushAwayTolerance) {
                    playerCollision.translate(0, 0, translateDistance);
                }

                // Z axis - south
                relZ = playerCollision.getMiddleZ() - z;
                translateDistance = southFaceZ - relZ + playerHalfZ;
                if (Math.abs(translateDistance) < zPushAwayTolerance) {
                    playerCollision.translate(0, 0, translateDistance);
                }

                // X axis - east
                double relX = playerCollision.getMiddleX() - x;
                translateDistance = eastFaceX - relX + playerHalfX;
                if (Math.abs(translateDistance) < xPushAwayTolerance) {
                    playerCollision.translate(translateDistance, 0, 0);
                }

                // X axis - west
                relX = playerCollision.getMiddleX() - x;
                translateDistance = westFaceX - relX - playerHalfX;
                if (Math.abs(translateDistance) < xPushAwayTolerance) {
                    playerCollision.translate(translateDistance, 0, 0);
                }

                // Y axis - bottom
                double relY = playerCollision.getMiddleY() - y;
                translateDistance = bottomFaceY - relY - playerHalfY;
                if (Math.abs(translateDistance) < pushAwayTolerance) {
                    playerCollision.translate(0, translateDistance, 0);
                }
            }

            // Restore original player collision size exactly (don't break possible special sizes)
            playerCollision.setSizeX(originalSizeX);
            playerCollision.setSizeZ(originalSizeZ);
        }

        return true;
    }

    public boolean checkIntersection(double x, double y, double z, BoundingBox playerCollision) {
        for (BoundingBox b : boundingBoxes) {
            if (b.checkIntersection(x, y, z, playerCollision)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIntersection(Vector3i position, BoundingBox playerCollision) {
        return checkIntersection(position.getX(), position.getY(), position.getZ(), playerCollision);
    }

    public double computeCollisionOffset(double x, double y, double z, BoundingBox boundingBox, Axis axis, double offset) {
        for (BoundingBox b : boundingBoxes) {
            offset = b.getMaxOffset(x, y, z, boundingBox, axis, offset);
            if (Math.abs(offset) < CollisionManager.COLLISION_TOLERANCE) {
                return 0;
            }
        }
        return offset;
    }

    /**
     * Checks if this block collision is below the given bounding box.
     *
     * @param blockY the y position of the block in the world
     * @param boundingBox the bounding box to compare
     * @return true if this block collision is below the bounding box
     */
    public boolean isBelow(int blockY, BoundingBox boundingBox) {
        double minY = boundingBox.getMiddleY() - boundingBox.getSizeY() / 2;
        for (BoundingBox b : boundingBoxes) {
            double offset = blockY + b.getMiddleY() + b.getSizeY() / 2 - minY;
            if (offset > CollisionManager.COLLISION_TOLERANCE) {
                return false;
            }
        }
        return true;
    }
}
