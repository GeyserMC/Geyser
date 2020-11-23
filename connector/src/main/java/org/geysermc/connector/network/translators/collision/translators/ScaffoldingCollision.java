/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.collision.translators;

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionRemapper;

/**
 * In order for scaffolding to work on Bedrock, entity flags need to be sent to the player
 */
@CollisionRemapper(regex = "^scaffolding$", usesParams = true, passDefaultBoxes = true)
public class ScaffoldingCollision extends BlockCollision {
    public ScaffoldingCollision(String params, BoundingBox[] defaultBoxes) {
        super();
        boundingBoxes = defaultBoxes;
    }

    @Override
    public boolean correctPosition(GeyserSession session, BoundingBox playerCollision) {
        // Hack to not check below the player
        playerCollision.setSizeY(playerCollision.getSizeY() - 0.001);
        playerCollision.setMiddleY(playerCollision.getMiddleY() + 0.002);

        boolean intersected = this.checkIntersection(playerCollision);

        playerCollision.setSizeY(playerCollision.getSizeY() + 0.001);
        playerCollision.setMiddleY(playerCollision.getMiddleY() - 0.002);

        if (intersected) {
            session.getCollisionManager().setTouchingScaffolding(true);
            session.getCollisionManager().setOnScaffolding(true);
        } else {
            // Hack to check slightly below the player
            playerCollision.setSizeY(playerCollision.getSizeY() + 0.001);
            playerCollision.setMiddleY(playerCollision.getMiddleY() - 0.002);

            if (this.checkIntersection(playerCollision)) {
                session.getCollisionManager().setOnScaffolding(true);
            }

            playerCollision.setSizeY(playerCollision.getSizeY() - 0.001);
            playerCollision.setMiddleY(playerCollision.getMiddleY() + 0.002);
        }

        // Normal move correction isn't really needed for scaffolding
        return true;
    }
}
