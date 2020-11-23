/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionRemapper;

@CollisionRemapper(regex = "^grass_path$", passDefaultBoxes = true)
public class GrassPathCollision extends BlockCollision {
    public GrassPathCollision(String params, BoundingBox[] defaultBoxes) {
        super();
        boundingBoxes = defaultBoxes;
    }

    // Needs to run before the main correction code or it can move the player into blocks
    // This is counteracted by the main collision code pushing them out
    @Override
    public void beforeCorrectPosition(BoundingBox playerCollision) {
        // In Bedrock, grass paths are small blocks so the player must be pushed down
        double playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
        // If the player is in the buggy area, push them down
        if (playerMinY == y + 1) {
            playerCollision.translate(0, -0.0625, 0);
        }
    }
}
