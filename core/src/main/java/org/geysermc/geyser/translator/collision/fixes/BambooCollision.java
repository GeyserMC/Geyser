/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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
import org.cloudburstmc.math.GenericMath;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.CollisionRemapper;

/**
 * On Java, bamboo's model and collision box are offset by a pseudorandom amount derived from the block's X/Z
 * position. Bedrock derives its offset differently, so the client's bamboo rarely lines up with Java's, and the
 * client will happily stand in spots that intersect the Java collision box. The Java server then rejects such
 * movement ("moved wrongly!") and teleports the player back, causing constant rubber-banding around bamboo.
 * <p>
 * The Bedrock-side offset cannot be changed, but this class computes the collision box the Java server actually
 * uses and silently nudges the position forwarded to it out of that box, so the movement is always accepted.
 */
@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^bamboo$")
public class BambooCollision extends BlockCollision {

    /**
     * Java's maximum horizontal collision offset for bamboo, see {@code BambooStalkBlock#getMaxHorizontalOffset}
     */
    private static final double MAX_HORIZONTAL_OFFSET = 0.125;

    /**
     * The maximum distance the player can be pushed out of a bamboo collision box. The offset difference between
     * the two editions can put the Bedrock player at most ~0.25 + a small margin inside the Java box; deeper than
     * that means the player is genuinely inside the block (e.g. bamboo grew into them), which Java tolerates and
     * lets them walk out of on their own.
     */
    private static final double MAX_CORRECTION = 0.3;

    public BambooCollision(BlockState state) {
        // Java's bamboo collision shape, Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5), before the offset is applied
        super(new BoundingBox[] {new BoundingBox(0.5, 0.5, 0.5, 0.1875, 1, 0.1875)});
    }

    @Override
    public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        BoundingBox blockCollision = this.boundingBoxes[0].clone();
        blockCollision.translate(x + getOffsetX(x, z), y, z + getOffsetZ(x, z));
        if (!blockCollision.checkIntersection(playerCollision)) {
            return;
        }

        // Distance needed to push the player out of the box, for each horizontal direction
        double west = playerCollision.getMax(Axis.X) - blockCollision.getMin(Axis.X);
        double east = blockCollision.getMax(Axis.X) - playerCollision.getMin(Axis.X);
        double north = playerCollision.getMax(Axis.Z) - blockCollision.getMin(Axis.Z);
        double south = blockCollision.getMax(Axis.Z) - playerCollision.getMin(Axis.Z);

        double distance = Math.min(Math.min(west, east), Math.min(north, south));
        if (distance > MAX_CORRECTION) {
            return;
        }

        double push = distance + CollisionManager.COLLISION_TOLERANCE;
        if (distance == west) {
            playerCollision.translate(-push, 0, 0);
        } else if (distance == east) {
            playerCollision.translate(push, 0, 0);
        } else if (distance == north) {
            playerCollision.translate(0, 0, -push);
        } else {
            playerCollision.translate(0, 0, push);
        }
    }

    @Override
    public boolean checkIntersection(double x, double y, double z, BoundingBox playerCollision) {
        int blockX = GenericMath.floor(x);
        int blockZ = GenericMath.floor(z);
        return this.boundingBoxes[0].checkIntersection(x + getOffsetX(blockX, blockZ), y, z + getOffsetZ(blockX, blockZ), playerCollision);
    }

    @Override
    public double computeCollisionOffset(double x, double y, double z, BoundingBox boundingBox, Axis axis, double offset) {
        int blockX = GenericMath.floor(x);
        int blockZ = GenericMath.floor(z);
        offset = this.boundingBoxes[0].getMaxOffset(x + getOffsetX(blockX, blockZ), y, z + getOffsetZ(blockX, blockZ), boundingBox, axis, offset);
        if (Math.abs(offset) < CollisionManager.COLLISION_TOLERANCE) {
            return 0;
        }
        return offset;
    }

    /**
     * @return the X offset Java applies to the collision box of a bamboo block at the given position
     */
    public static double getOffsetX(int x, int z) {
        return clampToMaxOffset(((getSeed(x, z) & 15L) / 15.0F - 0.5D) * 0.5D);
    }

    /**
     * @return the Z offset Java applies to the collision box of a bamboo block at the given position
     */
    public static double getOffsetZ(int x, int z) {
        return clampToMaxOffset(((getSeed(x, z) >> 8 & 15L) / 15.0F - 0.5D) * 0.5D);
    }

    private static double clampToMaxOffset(double value) {
        return Math.max(-MAX_HORIZONTAL_OFFSET, Math.min(MAX_HORIZONTAL_OFFSET, value));
    }

    /**
     * {@code Mth#getSeed} with y = 0, matching how Java computes the offset of blocks with an XZ offset type.
     * The int overflow in the X term is intentional and matches vanilla.
     */
    private static long getSeed(int x, int z) {
        long seed = (long) (x * 3129871) ^ (long) z * 116129781L;
        seed = seed * seed * 42317861L + seed * 11L;
        return seed >> 16;
    }
}
