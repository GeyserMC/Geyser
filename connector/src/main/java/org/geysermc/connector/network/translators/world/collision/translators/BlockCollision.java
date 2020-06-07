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

import lombok.EqualsAndHashCode;
import org.geysermc.connector.utils.BoundingBox;

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

    public void setPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void correctPosition(BoundingBox playerCollision) {
        double playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
        for (BoundingBox b: this.boundingBoxes) {
            double boxMinY = (b.getMiddleY() + y) - (b.getSizeY() / 2);
            double boxMaxY = (b.getMiddleY() + y) + (b.getSizeY() / 2);
            if (b.checkIntersection(x, y, z, playerCollision) && playerMinY >= boxMinY) {
                // Max steppable distance in Minecraft is 0.6 (not 0.5) blocks
                // You can step up 6-high snow layers - this isn't a bug
                if (boxMaxY - playerMinY <= 0.6) {
                    playerCollision.translate(0, boxMaxY - playerMinY, 0);
                    // Update player Y for next collision box
                    playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
                }
            }

            playerCollision.translate(0, 0.1, 0); // Hack to not check y
            // If the player still intersects the block, then push them out
            if (b.checkIntersection(x, y, z, playerCollision)) {
                double relativeX = playerCollision.getMiddleX() - (x + 0.5);
                double relativeY = playerCollision.getMiddleY() - (y + 0.5);
                double relativeZ = playerCollision.getMiddleZ() - (z + 0.5);
                if (relativeZ > 0 ? relativeX > relativeZ : relativeZ > (- relativeX)) {
                    // Collision with facing west - push the player east
                    // System.out.println("Collision facing towards negative X");
                    // System.out.println("Moved by " + (playerCollision.getMiddleX() - (b.getMiddleX() + (b.getSizeX() / 2) + x))); TODO
                    playerCollision.translate(playerCollision.getMiddleX() - (b.getMiddleX() + (b.getSizeX() / 2) + x), 0, 0);
                }
            }
            playerCollision.translate(0, -0.1, 0); // Hack to not check y
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

    // USED NOW! CHANGE THIS Or not! Currently never used, but will probably be useful in the future
    public boolean checkIntersection(BoundingBox playerCollision) {
        for (BoundingBox b: boundingBoxes) {
            if (b.checkIntersection(x, y, z, playerCollision)) {
                return true;
            }
        }
        return false;
    }
}