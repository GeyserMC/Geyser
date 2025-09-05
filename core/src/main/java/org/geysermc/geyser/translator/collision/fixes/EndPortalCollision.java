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
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.CollisionRemapper;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^end_portal_frame$", passDefaultBoxes = true)
public class EndPortalCollision extends BlockCollision {
    private final static double MAX_PUSH_DISTANCE = 0.1875 + CollisionManager.COLLISION_TOLERANCE * 1.01;

    private final boolean eye;

    public EndPortalCollision(BlockState state, BoundingBox[] boxes) {
        super(boxes);

        this.eye = state.getValue(Properties.EYE);
    }

    @Override
    protected void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox blockCollision, BoundingBox playerCollision) {
        if (!this.eye) {
            return;
        }

        // Check for end portal frame bug (BE don't have the eye collision when the end portal frame contain an eye unlike JE)
        blockCollision.pushOutOfBoundingBox(playerCollision, Direction.UP, MAX_PUSH_DISTANCE);
    }
}
