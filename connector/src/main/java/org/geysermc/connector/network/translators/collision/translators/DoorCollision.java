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

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionRemapper;

@CollisionRemapper(regex = "_door$", usesParams = true, passDefaultBoxes = true)
public class DoorCollision extends BlockCollision {
    /**
     * 1 = north
     * 2 = east
     * 3 = south
     * 4 = west
     */
    private int facing;

    public DoorCollision(String params, BoundingBox[] defaultBoxes) {
        super();
        boundingBoxes = defaultBoxes;
        if (params.contains("facing=north")) {
            facing = 1;
        } else if (params.contains("facing=east")) {
            facing = 2;
        } else if (params.contains("facing=south")) {
            facing = 3;
        } else if (params.contains("facing=west")) {
            facing = 4;
        }

        // If the door is open it changes direction
        if (params.contains("open=true")) {
            facing = facing % 2 + 1;
        }
    }

    @Override
    public boolean correctPosition(GeyserSession session, BoundingBox playerCollision) {
        boolean result = super.correctPosition(session, playerCollision);
        // Hack to prevent false positives
        playerCollision.setSizeX(playerCollision.getSizeX() - 0.0001);
        playerCollision.setSizeY(playerCollision.getSizeY() - 0.0001);
        playerCollision.setSizeZ(playerCollision.getSizeZ() - 0.0001);

        // Check for door bug (doors are 0.1875 blocks thick on Java but 0.1825 blocks thick on Bedrock)
        if (this.checkIntersection(playerCollision)) {
            switch (facing) {
                case 1: // North
                    playerCollision.setMiddleZ(Math.floor(playerCollision.getMiddleZ()) + 0.5125);
                    break;
                case 2: // East
                    playerCollision.setMiddleX(Math.floor(playerCollision.getMiddleX()) + 0.5125);
                    break;
                case 3: // South
                    playerCollision.setMiddleZ(Math.floor(playerCollision.getMiddleZ()) + 0.4875);
                    break;
                case 4: // West
                    playerCollision.setMiddleX(Math.floor(playerCollision.getMiddleX()) + 0.4875);
                    break;
            }
        }

        playerCollision.setSizeX(playerCollision.getSizeX() + 0.0001);
        playerCollision.setSizeY(playerCollision.getSizeY() + 0.0001);
        playerCollision.setSizeZ(playerCollision.getSizeZ() + 0.0001);
        return result;
    }
}
