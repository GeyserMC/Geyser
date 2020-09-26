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
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.BoundingBox;
import org.geysermc.connector.utils.MathUtils;

import javax.xml.transform.sax.SAXSource;

@EqualsAndHashCode
public class BlockCollision {

    protected BoundingBox[] boundingBoxes;

    protected int x, y, z;

    // This is used for the step up logic.
    // Usually, the player can only step up a block if they are on the same Y level as its bottom face or higher
    // For snow layers, due to its beforeCorrectPosition method the player can be slightly below (0.125 blocks) and still need to step up
    // Currently only used for snow layers
    @EqualsAndHashCode.Exclude
    protected double pushUpTolerance = 0;

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
                // Max steppable distance in Minecraft as far as we know is 0.5625 blocks (for beds)
                if (boxMaxY - playerMinY <= 0.5625) {
                    playerCollision.translate(0, boxMaxY - playerMinY, 0);
                    // Update player Y for next collision box
                    playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
                }
           }

            playerCollision.setSizeX(playerCollision.getSizeX() + GeyserSession.COLLISION_TOLERANCE * 2);
            playerCollision.setSizeZ(playerCollision.getSizeZ() + GeyserSession.COLLISION_TOLERANCE * 2);

            // If the player still intersects the block, then push them out
            if (b.checkIntersection(x, y, z, playerCollision)) {

                Vector3d oldPlayerPosition = Vector3d.from(playerCollision.getMiddleX(),
                        playerCollision.getMiddleY(),
                        playerCollision.getMiddleZ());

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

                double translateDistance = northFacePos.getZ() - relativePlayerPosition.getZ() - (playerCollision.getSizeZ() / 2);
                if (Math.abs(translateDistance) <  GeyserSession.COLLISION_TOLERANCE * 1.1) {
                    playerCollision.translate(0, 0, translateDistance);
                }
                
                translateDistance = southFacePos.getZ() - relativePlayerPosition.getZ() + (playerCollision.getSizeZ() / 2);
                if (Math.abs(translateDistance) <  GeyserSession.COLLISION_TOLERANCE * 1.1) {
                    playerCollision.translate(0, 0, translateDistance);
                }

                translateDistance = eastFacePos.getX() - relativePlayerPosition.getX() + (playerCollision.getSizeX() / 2);
                if (Math.abs(translateDistance) <  GeyserSession.COLLISION_TOLERANCE * 1.1) {
                    playerCollision.translate(translateDistance, 0, 0);
                }

                translateDistance = westFacePos.getX() - relativePlayerPosition.getX() - (playerCollision.getSizeX() / 2);
                if (Math.abs(translateDistance) <  GeyserSession.COLLISION_TOLERANCE * 1.1) {
                    playerCollision.translate(translateDistance, 0, 0);
                }

                Vector3d newPlayerPosition = Vector3d.from(playerCollision.getMiddleX(), // TODO: Use next possible double?
                        playerCollision.getMiddleY(),
                        playerCollision.getMiddleZ());

                if (MathUtils.taxicabDistance(oldPlayerPosition, newPlayerPosition) > GeyserSession.COLLISION_TOLERANCE + 0.1) {
                    playerCollision.setMiddleX(oldPlayerPosition.getX());
                    playerCollision.setMiddleY(oldPlayerPosition.getY());
                    playerCollision.setMiddleZ(oldPlayerPosition.getZ());
                    System.out.println("Cancelled");
                }
            }

            playerCollision.setSizeX(0.6);
            playerCollision.setSizeZ(0.6);
        }

    }

    public boolean checkIntersection(BoundingBox playerCollision) {
        for (BoundingBox b: boundingBoxes) {
            if (b.checkIntersection(x, y, z, playerCollision)) {
                return true;
            }
        }
        return false;
    }
}