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
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.GeyserSession;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "glass_pane$|iron_bars$", usesParams = true, passDefaultBoxes = true)
public class GlassPaneAndIronBarsCollision extends BlockCollision {
    /**
     * 1 = north
     * 2 = east
     * 3 = south
     * 4 = west
     * 5 = north, east
     * 6 = east, south
     * 7 = south, west
     * 8 = west, north
     */
    private int facing;

    public GlassPaneAndIronBarsCollision(String params, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);
        //east=true,north=true,south=true,west=true
        if (params.contains("north=true") && params.contains("east=false") && params.contains("south=false") && params.contains("west=false")) {
            facing = 1;
        } else if (params.contains("north=true") && params.contains("east=true") && params.contains("south=false") && params.contains("west=false")) {
            facing = 5;
        } else if (params.contains("east=true") && params.contains("north=false") && params.contains("south=false") && params.contains("west=false")) {
            facing = 2;
        } else if (params.contains("east=true") && params.contains("north=false") && params.contains("south=true") && params.contains("west=false")) {
            facing = 6;
        } else if (params.contains("south=true") && params.contains("north=false") && params.contains("east=false") && params.contains("west=false")) {
            facing = 3;
        } else if (params.contains("south=true") && params.contains("north=false") && params.contains("east=false") && params.contains("west=true")) {
            facing = 7;
        } else if (params.contains("west=true") && params.contains("north=false") && params.contains("east=false") && params.contains("south=false")) {
            facing = 4;
        } else if (params.contains("west=true") && params.contains("north=true") && params.contains("east=false") && params.contains("south=false")) {
            facing = 8;
        }
    }

    @Override
    public boolean correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        boolean result = super.correctPosition(session, x, y, z, playerCollision);
        playerCollision.setSizeX(playerCollision.getSizeX() - 0.0001);
        playerCollision.setSizeY(playerCollision.getSizeY() - 0.0001);
        playerCollision.setSizeZ(playerCollision.getSizeZ() - 0.0001);

        if (this.checkIntersection(x, y, z, playerCollision)) {
            switch (facing) {
                case 1 -> playerCollision.setMiddleZ(z + 0.8625); // North
                case 2 -> playerCollision.setMiddleX(x + 0.1375); // East
                case 3 -> playerCollision.setMiddleZ(z + 0.1375); // South
                case 4 -> playerCollision.setMiddleX(x + 0.8625); // West
                case 5 -> { //North, East
                    playerCollision.setMiddleZ(z + 0.8625);
                    playerCollision.setMiddleX(x + 0.1375);
                }
                case 6 -> { //East, South
                    playerCollision.setMiddleX(x + 0.1375);
                    playerCollision.setMiddleZ(z + 0.1375);
                }
                case 7 -> { //South, West
                    playerCollision.setMiddleZ(z + 0.1375);
                    playerCollision.setMiddleX(x + 0.8625);
                }
                case 8 -> { //West, North
                    playerCollision.setMiddleX(x + 0.8625);
                    playerCollision.setMiddleZ(z + 0.8625);
                }
            }
        }

        playerCollision.setSizeX(playerCollision.getSizeX() + 0.0001);
        playerCollision.setSizeY(playerCollision.getSizeY() + 0.0001);
        playerCollision.setSizeZ(playerCollision.getSizeZ() + 0.0001);
        return result;
    }
}
