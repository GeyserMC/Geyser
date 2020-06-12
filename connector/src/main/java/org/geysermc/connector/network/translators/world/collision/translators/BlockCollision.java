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

package org.geysermc.connector.network.translators.world.collision.translators;

import com.nukkitx.math.vector.Vector3d;
import lombok.EqualsAndHashCode;
import org.geysermc.connector.utils.BoundingBox;
import org.geysermc.connector.utils.MathUtils;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BlockCollision {
    @EqualsAndHashCode.Include
    BoundingBox[] boundingBoxes;

    @EqualsAndHashCode.Include
    int x;
    @EqualsAndHashCode.Include
    int y;
    @EqualsAndHashCode.Include
    int z;

    // This is used for the step up logic.
    // Usually, the player can only step up a block if they are on the same Y level as its bottom face or higher
    // For snow layers, due to its beforeCorrectPosition method the player can be slightly below (0.125 blocks) and still need to step up
    // Currently only used for snow layers
    double pushUpTolerance = 0;

    public void setPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Overridden in classes like SnowCollision when correction code needs to be run before the main correction
    public void beforeCorrectPosition(BoundingBox playerCollision) {}

    public void correctPosition(BoundingBox playerCollision) {
        double playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
        for (BoundingBox b: this.boundingBoxes) {
            double boxMinY = (b.getMiddleY() + y) - (b.getSizeY() / 2);
            double boxMaxY = (b.getMiddleY() + y) + (b.getSizeY() / 2);
            if (b.checkIntersection(x, y, z, playerCollision) && (playerMinY + pushUpTolerance) >= boxMinY) {
                // Max steppable distance in Minecraft is 0.5 blocks
                if (boxMaxY - playerMinY <= 0.5) {
                    playerCollision.translate(0, boxMaxY - playerMinY, 0);
                    // System.out.println("2: Moved by " + (boxMaxY - playerMinY));
                    // Update player Y for next collision box
                    playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
                }
           }

            // playerCollision.translate(0, 0.1, 0); // Hack to not check y
            // If the player still intersects the block, then push them out
            /* if (b.checkIntersection(x, y, z, playerCollision)) {
                /* double relativeX = playerCollision.getMiddleX() - (x + 0.5);
                double relativeY = playerCollision.getMiddleY() - (y + 0.5);
                double relativeZ = playerCollision.getMiddleZ() - (z + 0.5);
                if (relativeZ > 0 ? relativeX > relativeZ : relativeZ > (- relativeX)) {
                    // Collision with facing west - push the player east
                    System.out.println("Collision facing towards negative X");
                    // System.out.println("Moved by " + (playerCollision.getMiddleX() - (b.getMiddleX() + (b.getSizeX() / 2) + x))); TODO
                    playerCollision.translate(playerCollision.getMiddleX() - (b.getMiddleX() + (b.getSizeX() / 2) + x), 0, 0);
                } */
                /* while (b.checkIntersection(x, y, z, playerCollision)) {
                    playerCollision.translate(0, 0.01, 0);
                    System.out.println("Up");
                } *//*
                Vector3d relativePlayerPosition = Vector3d.from(playerCollision.getMiddleX() - x,
                        playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2) - y,
                        playerCollision.getMiddleZ() - z);

                Vector3d topFacePos = Vector3d.from(b.getMiddleX(),
                        b.getMiddleY() + (b.getSizeY() / 2),
                        b.getMiddleZ());
                double topFaceDistance = MathUtils.taxicabDistance(topFacePos,  relativePlayerPosition);

                Vector3d bottomFacePos = Vector3d.from(b.getMiddleX(),
                        b.getMiddleY() - (b.getSizeY() / 2),
                        b.getMiddleZ());
                double bottomFaceDistance = MathUtils.taxicabDistance(bottomFacePos,  relativePlayerPosition);

                Vector3d northFacePos = Vector3d.from(b.getMiddleX(),
                        b.getMiddleY(),
                        b.getMiddleZ() - (b.getSizeZ() / 2));
                double northFaceDistance = MathUtils.taxicabDistance(northFacePos, relativePlayerPosition);

                Vector3d southFacePos = Vector3d.from(b.getMiddleX(),
                        b.getMiddleY(),
                        b.getMiddleZ() + (b.getSizeZ() / 2));
                double southFaceDistance = MathUtils.taxicabDistance(southFacePos, relativePlayerPosition);

                Vector3d eastFacePos = Vector3d.from(b.getMiddleX()  + (b.getSizeX() / 2),
                        b.getMiddleY(),
                        b.getMiddleZ());
                double eastFaceDistance = MathUtils.taxicabDistance(eastFacePos, relativePlayerPosition);

                Vector3d westFacePos = Vector3d.from(b.getMiddleX()  - (b.getSizeX() / 2),
                        b.getMiddleY(),
                        b.getMiddleZ());
                double westFaceDistance = MathUtils.taxicabDistance(westFacePos, relativePlayerPosition);

                System.out.println(topFacePos.getY() - relativePlayerPosition.getY());
                // Unless the player is very close to the top, don't snap them there
                if // (!((topFacePos.getY() - relativePlayerPosition.getY() > 0) && // Positive number (player is below top)
                ((!   (topFacePos.getY() - relativePlayerPosition.getY() < 0.00001))) { // Player is close to top
                    // This should never be the lowest number
                    topFaceDistance = 9999;
                }

                double closestDistance = Math.min(topFaceDistance,
                                         // Math.min(bottomFaceDistance,
                                         Math.min(northFaceDistance,
                                         Math.min(southFaceDistance,
                                         Math.min(eastFaceDistance,
                                                  westFaceDistance)))); // );

                System.out.println("Distance: " + closestDistance);
                if (closestDistance == topFaceDistance) {
                    playerCollision.translate(0, (topFacePos.getY() - relativePlayerPosition.getY()), 0);
                    System.out.println("Snapped to top");
                }/* else if (closestDistance == bottomFaceDistance) {
                    playerCollision.translate(0, (bottomFacePos.getY() - relativePlayerPosition.getY()) + 0.9, 0);
                    System.out.println("Snapped to bottom");
                }*//* else if (closestDistance == northFaceDistance) {
                    playerCollision.translate(0, 0, northFacePos.getZ() - relativePlayerPosition.getZ() - (playerCollision.getSizeZ() / 2));
                    System.out.println("Snapped to north");
                } else if (closestDistance == southFaceDistance) {
                    playerCollision.translate(0, 0, southFacePos.getZ() - relativePlayerPosition.getZ() + (playerCollision.getSizeZ() / 2));
                    System.out.println("Snapped to south");
                } else if (closestDistance == eastFaceDistance) {
                    playerCollision.translate(eastFacePos.getX() - relativePlayerPosition.getX() + (playerCollision.getSizeX() / 2), 0, 0);
                    System.out.println("Snapped to east");
                } else if (closestDistance == westFaceDistance) {
                    playerCollision.translate(westFacePos.getX() - relativePlayerPosition.getX() - (playerCollision.getSizeX() / 2), 0, 0);
                    System.out.println("Snapped to west");
                }
            } */
            // playerCollision.translate(0, -0.1, 0); // Hack to not check y
        }

        // Solid checking for NoCheatPlus etc.
        // TODO: Better checking
        /* playerCollision.translate(0, 0.1, 0); // Hack to not check y
        if (checkIntersection(playerCollision)) {
            if (playerCollision.getMiddleX() > (x + 0.5)) {
                playerCollision.translate(0.1, 0, 0);
            } else {
                playerCollision.translate(-0.1, 0, 0);
            }

            if (playerCollision.getMiddleZ() > (z + 0.5)) {
                playerCollision.translate(0, 0, 0.1);
            } else {
                playerCollision.translate(0, 0, -0.1);
            }

            /* if (playerCollision.getMiddleY() > (y + 0.5)) {
                playerCollision.translate(0, 0.1, 0);
            } else {
                playerCollision.translate(0, -0.1, 0);
            } *//*
        }
        playerCollision.translate(0, -0.1, 0); // Hack to not check y */
    }

    // USED NOW! CHANGE THIS COMMENT Or not! Currently never used, but will probably be useful in the future
    public boolean checkIntersection(BoundingBox playerCollision) {
        for (BoundingBox b: boundingBoxes) {
            if (b.checkIntersection(x, y, z, playerCollision)) {
                return true;
            }
        }
        return false;
    }
}