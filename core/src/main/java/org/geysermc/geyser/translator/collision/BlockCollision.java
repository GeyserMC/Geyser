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
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;

@EqualsAndHashCode
public class BlockCollision {

    @Getter
    protected final BoundingBox[] boundingBoxes;

    /**
     * This is used to control the maximum distance a face of a bounding box can push the player away
     */
    protected final double pushAwayTolerance = CollisionManager.COLLISION_TOLERANCE * 1.1;

    protected BlockCollision(BoundingBox[] boxes) {
        this.boundingBoxes = boxes;
    }

    /**
     * Silently move player bounding box/position out of block when needed to.
     */
    public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        final double collisionExpansion = CollisionManager.COLLISION_TOLERANCE * 2;
        // Make player collision slightly bigger to pick up on blocks that could cause problems with NoCheatPlus's Passable check.
        playerCollision.expand(collisionExpansion, 0, collisionExpansion);

        // Due to floating points errors, or because of block collision difference, player could be slightly clipping into the block.
        // So we check if the player is intersecting the block, if they do then push them out. This fixes NoCheatPlus's Passable check and other anticheat checks.
        // This check doesn't allow players right up against the block, so they must be pushed slightly away. However, we should only do it if the
        // push distance is smaller than "pushAwayTolerance", we don't want to push player out when they're actually inside a block.
        for (BoundingBox boundingBox : this.boundingBoxes) {
            if (!boundingBox.checkIntersection(x, y, z, playerCollision)) {
                continue;
            }

            boundingBox = boundingBox.clone();
            boundingBox.translate(x, y, z);

            // The ULP should give an upper bound on the floating point error
            double xULP = Math.ulp((float) Math.max(Math.abs(playerCollision.getMiddleX()) + playerCollision.getSizeX() / 2.0, Math.abs(x) + 1));
            double zULP = Math.ulp((float) Math.max(Math.abs(playerCollision.getMiddleZ()) + playerCollision.getSizeZ() / 2.0, Math.abs(z) + 1));
            double xPushAwayTolerance = Math.max(pushAwayTolerance, xULP), zPushAwayTolerance = Math.max(pushAwayTolerance, zULP);

            boundingBox.pushOutOfBoundingBox(playerCollision, Direction.NORTH, zPushAwayTolerance);
            boundingBox.pushOutOfBoundingBox(playerCollision, Direction.SOUTH, zPushAwayTolerance);
            boundingBox.pushOutOfBoundingBox(playerCollision, Direction.EAST, xPushAwayTolerance);
            boundingBox.pushOutOfBoundingBox(playerCollision, Direction.WEST, xPushAwayTolerance);
            boundingBox.pushOutOfBoundingBox(playerCollision, Direction.UP, pushAwayTolerance);
            boundingBox.pushOutOfBoundingBox(playerCollision, Direction.DOWN, pushAwayTolerance);

            correctPosition(session, x, y, z, boundingBox, playerCollision);
        }

        // Revert back to the old collision size.
        playerCollision.expand(-collisionExpansion, 0, -collisionExpansion);
    }

    protected void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox blockCollision, BoundingBox playerCollision) {
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
