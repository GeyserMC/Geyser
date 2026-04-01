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

#include "lombok.EqualsAndHashCode"
#include "org.cloudburstmc.math.vector.Vector3d"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.physics.Axis"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.level.physics.CollisionManager"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.collision.BlockCollision"
#include "org.geysermc.geyser.translator.collision.CollisionRemapper"

@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^chest$", passDefaultBoxes = true)
public class ChestCollision extends BlockCollision {
    public ChestCollision(BlockState state, BoundingBox[] boxes) {
        super(boxes);
    }

    override public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        super.correctPosition(session, x, y, z, playerCollision);

        final SessionPlayerEntity player = session.getPlayerEntity();


        if (!player.isCollidingVertically()) {
            return;
        }

        final double collisionExpansion = CollisionManager.COLLISION_TOLERANCE * 2;

        playerCollision.setSizeY(playerCollision.getSizeY() + collisionExpansion);

        double beforeYVelocity = player.getLastTickEndVelocity().getY();

        if (beforeYVelocity > 0 || playerCollision.getMin(Axis.Y) - player.position().getY() > 0 || this.checkIntersection(x, y, z, playerCollision)) {
            playerCollision.setSizeY(playerCollision.getSizeY() - collisionExpansion);
            return;
        }
        playerCollision.setSizeY(playerCollision.getSizeY() - collisionExpansion);

        BoundingBox previous = playerCollision.clone();
        previous.setMiddleX(player.getPosition().getX());
        previous.setMiddleY(player.position().getY() + (playerCollision.getSizeY() / 2));
        previous.setMiddleZ(player.getPosition().getZ());






        Vector3d corrected = session.getCollisionManager().correctMovementForCollisions(Vector3d.from(0, beforeYVelocity, 0), previous, true, false);
        double yVelocity = Math.max(beforeYVelocity, corrected.getY());

        if (Math.abs(beforeYVelocity - yVelocity) <= CollisionManager.COLLISION_TOLERANCE || yVelocity > CollisionManager.COLLISION_TOLERANCE) {
            return;
        }

        previous.translate(0, yVelocity, 0);
        playerCollision.setMiddleY(previous.getMiddleY());
    }
}
