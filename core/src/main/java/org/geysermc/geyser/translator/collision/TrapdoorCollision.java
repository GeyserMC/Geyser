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
import org.geysermc.geyser.level.physics.CollisionManager;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "_trapdoor$", usesParams = true, passDefaultBoxes = true)
public class TrapdoorCollision extends BlockCollision {
    /**
     * 1 = north
     * 2 = east
     * 3 = south
     * 4 = west
     * 5 = up
     * 6 = down
     */
    private int facing;

    public TrapdoorCollision(String params, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);
        if (params.contains("open=true")) {
            if (params.contains("facing=north")) {
                facing = 1;
            } else if (params.contains("facing=east")) {
                facing = 2;
            } else if (params.contains("facing=south")) {
                facing = 3;
            } else if (params.contains("facing=west")) {
                facing = 4;
            }
        } else {
            if (params.contains("half=bottom")) {
                // Up
                facing = 5;
            } else {
                // Down
                facing = 6;
            }
        }
    }

    @Override
    public boolean correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        boolean result = super.correctPosition(session, x, y, z, playerCollision);
        // Check for door bug (doors are 0.1875 blocks thick on Java but 0.1825 blocks thick on Bedrock)
        if (this.checkIntersection(x, y, z, playerCollision)) {
            switch (facing) {
                case 1: // North
                    playerCollision.setMiddleZ(z + 0.5125);
                    break;
                case 2: // East
                    playerCollision.setMiddleX(x + 0.5125);
                    break;
                case 3: // South
                    playerCollision.setMiddleZ(z + 0.4875);
                    break;
                case 4: // West
                    playerCollision.setMiddleX(x + 0.4875);
                    break;
                case 5:
                    // Up-facing trapdoors are handled by the step-up check
                    break;
                case 6: // Down
                    // (top y of trap door) - (trap door thickness) = top y of player
                    playerCollision.setMiddleY(y + 1 - (3.0 / 16.0) - playerCollision.getSizeY() / 2.0 - CollisionManager.COLLISION_TOLERANCE);
                    break;
            }
        }
        return result;
    }
}
