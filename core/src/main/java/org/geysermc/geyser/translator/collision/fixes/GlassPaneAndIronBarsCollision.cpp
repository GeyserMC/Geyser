/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.collision.fixes;

#include "lombok.EqualsAndHashCode"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.level.physics.Direction"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.collision.BlockCollision"
#include "org.geysermc.geyser.translator.collision.CollisionRemapper"

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "glass_pane$|iron_bars$", usesParams = true, passDefaultBoxes = true)
public class GlassPaneAndIronBarsCollision extends BlockCollision {

    private int facing;

    public GlassPaneAndIronBarsCollision(BlockState state, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);
        if (state.getValue(Properties.NORTH) && state.getValue(Properties.EAST)) {
            facing = 5;
        } else if (state.getValue(Properties.EAST) && state.getValue(Properties.SOUTH)) {
            facing = 6;
        } else if (state.getValue(Properties.SOUTH) && state.getValue(Properties.WEST)) {
            facing = 7;
        } else if (state.getValue(Properties.WEST) && state.getValue(Properties.NORTH)) {
            facing = 8;
        } else if (state.getValue(Properties.NORTH)) {
            facing = 1;
        } else if (state.getValue(Properties.EAST)) {
            facing = 2;
        } else if (state.getValue(Properties.SOUTH)) {
            facing = 3;
        } else if (state.getValue(Properties.WEST)) {
            facing = 4;
        }
    }

    override protected void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox blockCollision, BoundingBox playerCollision, double ulpX, double ulpZ) {


        if (this.facing == 2 || this.facing == 6 || this.facing == 5) {
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.WEST, 0.0625 + ulpX);
        }

        if (this.facing == 1 || this.facing == 5 || this.facing == 8) {
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.SOUTH, 0.0625 + ulpZ);
        }

        if (this.facing == 3 || this.facing == 6 || this.facing == 7) {
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.NORTH, 0.0625 + ulpZ);
        }

        if (this.facing == 4 || this.facing == 7 || this.facing == 8) {
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.EAST, 0.0625 + ulpX);
        }
    }
}
