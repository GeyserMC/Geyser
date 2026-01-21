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
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.CollisionRemapper;

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^chest$", passDefaultBoxes = true)
public class ChestCollision extends BlockCollision {
    public ChestCollision(BlockState state, BoundingBox[] boxes) {
        super(boxes);
    }

    @Override
    public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        super.correctPosition(session, x, y, z, playerCollision);

        final SessionPlayerEntity player = session.getPlayerEntity();

        // Player didn't fall on the blocks yet, no need to move them down.
        if (!player.isCollidingVertically()) {
            return;
        }

        final double collisionExpansion = CollisionManager.COLLISION_TOLERANCE * 2;
        // Slightly expand the collision so we can see if the player is actually colliding with the block or not.
        playerCollision.setSizeY(playerCollision.getSizeY() + collisionExpansion);

        double beforeYVelocity = player.getLastTickEndVelocity().getY();
        // If the player is already colliding with the block or player velocity y is larger than 0, then the player likely does not need a correction
        if (beforeYVelocity > 0 || playerCollision.getMin(Axis.Y) - player.position().getY() > 0 || this.checkIntersection(x, y, z, playerCollision)) {
            playerCollision.setSizeY(playerCollision.getSizeX() - collisionExpansion);
            return;
        }
        playerCollision.setSizeY(playerCollision.getSizeX() - collisionExpansion);

        BoundingBox previous = playerCollision.clone();
        previous.setMiddleX(player.getPosition().getX());
        previous.setMiddleY(player.position().getY() + (playerCollision.getSizeY() / 2));
        previous.setMiddleZ(player.getPosition().getZ());

        // Check for chest bug (chest are 0.875 blocks thick on Java but 0.95 blocks thick on Bedrock)
        // We grab the player velocity from last tick then apply collision on it, if the player can still fall then we correct
        // their position to fall down. If not then just use the player current position. Also do the same when player is jumping, if player
        // haven't collided yet, then correct them. Resolve #3277 and #4955
        double yVelocity = Math.max(beforeYVelocity, this.computeCollisionOffset(x, y, z, previous, Axis.Y, beforeYVelocity));
        // Player velocity is close enough, no need to correct, avoid moving player position silently if possible. Also don't move the player upwards.
        if (Math.abs(beforeYVelocity - yVelocity) <= CollisionManager.COLLISION_TOLERANCE || yVelocity > CollisionManager.COLLISION_TOLERANCE) {
            return;
        }

        previous.translate(0, yVelocity, 0);
        playerCollision.setMiddleY(previous.getMiddleY());
    }
}
