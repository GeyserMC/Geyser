/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.CollisionRemapper;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^conduit$", passDefaultBoxes = true)
public class ConduitCollision extends BlockCollision {
    public ConduitCollision(BlockState state, BoundingBox[] boxes) {
        super(boxes);
    }

    @Override
    public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        super.correctPosition(session, x, y, z, playerCollision);

        final double maxPushDistance = 0.1875F + CollisionManager.COLLISION_TOLERANCE * 1.01F;

        // Check for conduit bug (conduit is lifted from the ground on Java unlike Bedrock where conduit is placed on the ground)
        for (BoundingBox boundingBox : this.boundingBoxes) {
            if (!boundingBox.checkIntersection(x, y, z, playerCollision)) {
                continue;
            }

            boundingBox = boundingBox.clone();
            boundingBox.translate(x, y, z);

            boundingBox.pushOutOfBoundingBox(playerCollision, Direction.UP, maxPushDistance);
        }
    }
}
