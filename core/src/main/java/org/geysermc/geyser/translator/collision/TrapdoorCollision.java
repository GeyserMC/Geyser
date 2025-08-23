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
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "_trapdoor$", usesParams = true, passDefaultBoxes = true)
public class TrapdoorCollision extends BlockCollision {
    private final Direction facing;

    public TrapdoorCollision(BlockState state, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);
        if (state.getValue(Properties.OPEN)) {
            facing = state.getValue(Properties.HORIZONTAL_FACING);
        } else {
            if (state.getValue(Properties.HALF).equals("bottom")) {
                facing = Direction.UP;
            } else {
                facing = Direction.DOWN;
            }
        }
    }

    @Override
    public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        super.correctPosition(session, x, y, z, playerCollision);

        final Vector3d relativePlayerPosition = Vector3d.from(playerCollision.getMiddleX() - x, playerCollision.getMiddleY() - y, playerCollision.getMiddleZ() - z);

        for (BoundingBox boundingBox : this.boundingBoxes) {
            if (!boundingBox.checkIntersection(x, y, z, playerCollision)) {
                continue;
            }

            // Check for door bug (doors are 0.1875 blocks thick on Java but 0.1825 blocks thick on Bedrock)
            switch (facing) {
                case NORTH -> playerCollision.setMiddleZ(z + 0.5125);
                case EAST -> playerCollision.setMiddleX(x + 0.5125);
                case SOUTH -> playerCollision.setMiddleZ(z + 0.4875);
                case WEST -> playerCollision.setMiddleX(x + 0.4875);
                case UP -> {
                    double distance = boundingBox.getMax(Axis.Y) - relativePlayerPosition.getY() + playerCollision.getSizeY() / 2;
                    if (Math.abs(distance) < 0.005 + CollisionManager.COLLISION_TOLERANCE * 1.01F) {
                        playerCollision.translate(0, distance, 0);
                    }
                }
                case DOWN -> {
                    double distance = boundingBox.getMin(Axis.Y) - relativePlayerPosition.getY() - playerCollision.getSizeY() / 2;
                    if (Math.abs(distance) < 0.005 + CollisionManager.COLLISION_TOLERANCE * 1.01F) {
                        playerCollision.translate(0, distance, 0); // Bottom
                    }
                }
            }
        }


    }
}
