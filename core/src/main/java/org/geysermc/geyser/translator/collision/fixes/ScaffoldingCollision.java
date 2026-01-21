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
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.CollisionRemapper;

/**
 * In order for scaffolding to work on Bedrock, entity flags need to be sent to the player
 */
@EqualsAndHashCode(callSuper = true)
@CollisionRemapper(regex = "^scaffolding$", usesParams = true, passDefaultBoxes = true)
public class ScaffoldingCollision extends BlockCollision {
    private final boolean bottom;

    public ScaffoldingCollision(BlockState state, BoundingBox[] defaultBoxes) {
        super(defaultBoxes);

        this.bottom = state.getValue(Properties.BOTTOM) && state.getValue(Properties.STABILITY_DISTANCE) != 0;
    }

    @Override
    public void correctPosition(GeyserSession session, int x, int y, int z, BoundingBox playerCollision) {
        // Hack to not check below the player
        playerCollision.setSizeY(playerCollision.getSizeY() - 0.001);
        playerCollision.setMiddleY(playerCollision.getMiddleY() + 0.002);

        boolean intersected = this.checkIntersection(x, y, z, playerCollision);

        playerCollision.setSizeY(playerCollision.getSizeY() + 0.001);
        playerCollision.setMiddleY(playerCollision.getMiddleY() - 0.002);

        boolean canStandOn = playerCollision.getMin(Axis.Y) >= y + 1 && !session.isSneaking();

        // If these condition are met, then the scaffolding will have a bottom collision that is 0.125 block high.
        // However, this is not the case in Bedrock, so we push the player up by 0.125 blocks so the player won't get setback.
        if (!canStandOn && this.bottom && playerCollision.getMin(Axis.Y) >= y) {
            double distance = y + 0.125 - (playerCollision.getMin(Axis.Y));
            if (Math.abs(distance) < 0.125F + CollisionManager.COLLISION_TOLERANCE * 1.01F) {
                playerCollision.translate(0, distance, 0);
            }
        }

        if (intersected) {
            session.getCollisionManager().setTouchingScaffolding(true);
            session.getCollisionManager().setOnScaffolding(true);
        } else {
            // Hack to check slightly below the player
            playerCollision.setSizeY(playerCollision.getSizeY() + 0.001);
            playerCollision.setMiddleY(playerCollision.getMiddleY() - 0.002);

            if (this.checkIntersection(x, y, z, playerCollision)) {
                session.getCollisionManager().setOnScaffolding(true);
            }

            playerCollision.setSizeY(playerCollision.getSizeY() - 0.001);
            playerCollision.setMiddleY(playerCollision.getMiddleY() + 0.002);
        }
    }
}
