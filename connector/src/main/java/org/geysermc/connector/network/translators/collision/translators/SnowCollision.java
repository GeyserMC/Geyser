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

package org.geysermc.connector.network.translators.collision.translators;

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionRemapper;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@CollisionRemapper(regex = "^snow$", usesParams = true)
public class SnowCollision extends BlockCollision {
    private final int layers;

    public SnowCollision(String params) {
        super();
        Pattern layersPattern = Pattern.compile("layers=([0-8])");
        Matcher matcher = layersPattern.matcher(params);
        //noinspection ResultOfMethodCallIgnored
        matcher.find();

        // Hitbox is 1 layer less (you sink in 1 layer)
        layers = Integer.parseInt(matcher.group(1));

        if (layers > 1) {
            boundingBoxes = new BoundingBox[] {
                    // Take away 1 because you can go 1 layer into snow layers
                    new BoundingBox(0.5, ((layers - 1) * 0.125) / 2, 0.5,
                            1, (layers - 1) * 0.125, 1)
            };
        } else {
            // Single layers have no collision
            boundingBoxes = new BoundingBox[0];
        }

        pushUpTolerance = 0.125;
    }

    // Needs to run before the main correction code or it can move the player into blocks
    // This is counteracted by the main collision code pushing them out
    @Override
    public void beforeCorrectPosition(BoundingBox playerCollision) {
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
    public boolean correctPosition(GeyserSession session, BoundingBox playerCollision) {
        // Hack to prevent false positives
        playerCollision.setSizeX(playerCollision.getSizeX() - 0.0001);
        playerCollision.setSizeY(playerCollision.getSizeY() - 0.0001);
        playerCollision.setSizeZ(playerCollision.getSizeZ() - 0.0001);

        if (this.checkIntersection(playerCollision)) {
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
        return super.correctPosition(session, playerCollision);
    }
}
