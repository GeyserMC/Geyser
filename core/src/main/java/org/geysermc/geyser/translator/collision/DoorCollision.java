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
import org.geysermc.geyser.session.GeyserSession;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "_door$", usesParams = true, passDefaultBoxes = true)
public class DoorCollision extends BlockCollision {
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
    public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        super.correctPosition(session, x, y, z, playerCollision);
        final double maxPushDistance = 0.005 + CollisionManager.COLLISION_TOLERANCE * 1.01F;
        final Vector3d relativePlayerPosition = Vector3d.from(playerCollision.getMiddleX() - x, playerCollision.getMiddleY() - y, playerCollision.getMiddleZ() - z);

        // Check for door bug (doors are 0.1875 blocks thick on Java but 0.1825 blocks thick on Bedrock)
        for (BoundingBox boundingBox : this.boundingBoxes) {
            if (!boundingBox.checkIntersection(x, y, z, playerCollision)) {
                continue;
            }

            switch (this.facing) {
                case 1 -> {
                    double distance = boundingBox.getMin(Axis.Z) - relativePlayerPosition.getZ() - (playerCollision.getSizeZ() / 2);
                    if (Math.abs(distance) < maxPushDistance) {
                        playerCollision.translate(0, 0, distance); // North
                    }
                }
                case 2 -> {
                    double distance = boundingBox.getMax(Axis.X) - relativePlayerPosition.getX() + (playerCollision.getSizeX() / 2);
                    if (Math.abs(distance) < maxPushDistance) {
                        playerCollision.translate(distance, 0, 0); // East
                    }
                }
                case 3 -> {
                    double distance = boundingBox.getMax(Axis.Z) - relativePlayerPosition.getZ() + (playerCollision.getSizeZ() / 2);
                    if (Math.abs(distance) < maxPushDistance) {
                        playerCollision.translate(0, 0, distance); // South
                    }
                }
                case 4 -> {
                    double distance = boundingBox.getMin(Axis.X) - relativePlayerPosition.getX() - (playerCollision.getSizeX() / 2);
                    if (Math.abs(distance) < maxPushDistance) {
                        playerCollision.translate(distance, 0, 0); // West
                    }
                }
            }
        }
    }
}
