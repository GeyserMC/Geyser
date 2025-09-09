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

import lombok.EqualsAndHashCode;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.CollisionRemapper;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "glass_pane$|iron_bars$", usesParams = true, passDefaultBoxes = true)
public class GlassPaneAndIronBarsCollision extends BlockCollision {
    private final static double MAX_PUSH_DISTANCE = 0.0625 + CollisionManager.COLLISION_TOLERANCE * 1.01;

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

    @Override
    protected void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox blockCollision, BoundingBox playerCollision) {
        // Check for glass pane/iron bars bug (pane/iron bars is 0.5 blocks thick on Bedrock but 0.5625 on Java when only 1 side is connected).
        // Also, we want to flip the direction since the direction here is indicating the block side the glass is connected to.
        if (this.facing == 2 || this.facing == 6 || this.facing == 5) { // East
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.WEST, MAX_PUSH_DISTANCE);
        }

        if (this.facing == 1 || this.facing == 5 || this.facing == 8) { // North.
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.SOUTH, MAX_PUSH_DISTANCE);
        }

        if (this.facing == 3 || this.facing == 6 || this.facing == 7) { // South
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.NORTH, MAX_PUSH_DISTANCE);
        }

        if (this.facing == 4 || this.facing == 7 || this.facing == 8) { // West
            blockCollision.pushOutOfBoundingBox(playerCollision, Direction.EAST, MAX_PUSH_DISTANCE);
        }
    }
}
