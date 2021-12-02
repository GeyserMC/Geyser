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
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^dirt_path$", passDefaultBoxes = true)
public class DirtPathCollision extends BlockCollision {
    public DirtPathCollision(String params, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);
    }

    // Needs to run before the main correction code or it can move the player into blocks
    // This is counteracted by the main collision code pushing them out
    @Override
    public void beforeCorrectPosition(int x, int y, int z, BoundingBox playerCollision) {
        // In Bedrock, dirt paths are solid blocks, so the player must be pushed down.
        double playerMinY = playerCollision.getMiddleY() - (playerCollision.getSizeY() / 2);
        double blockMaxY = y + 1;
        if (Math.abs(blockMaxY - playerMinY) <= CollisionManager.COLLISION_TOLERANCE) {
            playerCollision.translate(0, -0.0625, 0);
        }
    }
}
