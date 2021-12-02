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

package org.geysermc.geyser.translator.collision;

import lombok.EqualsAndHashCode;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.physics.BoundingBox;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^snow$", passDefaultBoxes = true, usesParams = true)
public class SnowCollision extends BlockCollision {
    private final int layers;

    public SnowCollision(String params, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);
        int layerCharIndex = params.indexOf("=") + 1;
        layers = Integer.parseInt(params.substring(layerCharIndex, layerCharIndex + 1));

        pushUpTolerance = 0.125;
    }

    // Needs to run before the main correction code or it can move the player into blocks
    // This is counteracted by the main collision code pushing them out
    @Override
    public void beforeCorrectPosition(int x, int y, int z, BoundingBox playerCollision) {
        // In Bedrock, snow layers round down to half blocks but you can't sink into them at all
        // This means the collision each half block reaches above where it should be on Java so the player has to be
        // pushed down
        if (layers == 4 || layers == 8) {
            double playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
            double boxMaxY = (boundingBoxes[0].getMiddleY() + y) + (boundingBoxes[0].getSizeY() / 2);
            // If the player is in the buggy area, push them down
            if (playerMinY > boxMaxY &&
                    playerMinY <= (boxMaxY + 0.125)) {
                playerCollision.translate(0, boxMaxY - playerMinY, 0);
            }
        }
    }

    @Override
    public boolean correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        if (layers == 1) {
            // 1 layer of snow does not have collision
            return true;
        }
        // Hack to prevent false positives
        playerCollision.setSizeX(playerCollision.getSizeX() - 0.0001);
        playerCollision.setSizeY(playerCollision.getSizeY() - 0.0001);
        playerCollision.setSizeZ(playerCollision.getSizeZ() - 0.0001);

        if (this.checkIntersection(x, y, z, playerCollision)) {
            double playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
            double boxMaxY = (boundingBoxes[0].getMiddleY() + y) + (boundingBoxes[0].getSizeY() / 2);
            // If the player actually can't step onto it (they can step onto it from other snow layers)
            if ((boxMaxY - playerMinY) > 0.5) {
                // Cancel the movement
                return false;
            }
        }

        playerCollision.setSizeX(playerCollision.getSizeX() + 0.0001);
        playerCollision.setSizeY(playerCollision.getSizeY() + 0.0001);
        playerCollision.setSizeZ(playerCollision.getSizeZ() + 0.0001);
        return super.correctPosition(session, x, y, z, playerCollision);
    }
}
