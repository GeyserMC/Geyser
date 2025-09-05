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
@CollisionRemapper(regex = "_door$", usesParams = true, passDefaultBoxes = true)
public class DoorCollision extends BlockCollision {
    private final static double MAX_PUSH_DISTANCE = 0.005 + CollisionManager.COLLISION_TOLERANCE * 1.01;

    /**
     * 1 = north
     * 2 = east
     * 3 = south
     * 4 = west
     */
    private int facing;

    public DoorCollision(BlockState state, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);
        facing = switch (state.getValue(Properties.HORIZONTAL_FACING)) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            default -> throw new IllegalStateException();
        };

        // If the door is open it changes direction
        if (state.getValue(Properties.OPEN)) {
            facing = facing % 2 + 1;
        }
    }

    @Override
    protected void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox blockCollision, BoundingBox playerCollision) {
        // Check for door bug (doors are 0.1875 blocks thick on Java but 0.1825 blocks thick on Bedrock)
        switch (this.facing) {
            case 1 -> blockCollision.pushOutOfBoundingBox(playerCollision, Direction.NORTH, MAX_PUSH_DISTANCE);
            case 2 -> blockCollision.pushOutOfBoundingBox(playerCollision, Direction.EAST, MAX_PUSH_DISTANCE);
            case 3 -> blockCollision.pushOutOfBoundingBox(playerCollision, Direction.SOUTH, MAX_PUSH_DISTANCE);
            case 4 -> blockCollision.pushOutOfBoundingBox(playerCollision, Direction.WEST, MAX_PUSH_DISTANCE);
        }
    }
}
