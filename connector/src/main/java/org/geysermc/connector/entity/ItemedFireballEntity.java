/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity;

import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class ItemedFireballEntity extends ThrowableEntity {
    private final Vector3f acceleration;

    /**
     * The number of ticks to advance movement before sending to Bedrock
     */
    protected int futureTicks = 3;

    public ItemedFireballEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, Vector3f.ZERO, rotation);

        float magnitude = motion.length();
        if (magnitude != 0) {
            acceleration = motion.div(magnitude).mul(0.1f);
        } else {
            acceleration = Vector3f.ZERO;
        }
    }

    private Vector3f tickMovement(GeyserSession session, Vector3f position) {
        position = position.add(motion);
        float drag = getDrag(session);
        motion = motion.add(acceleration).mul(drag);
        return position;
    }

    @Override
    protected void moveAbsoluteImmediate(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        // Advance the position by a few ticks before sending it to Bedrock
        Vector3f lastMotion = motion;
        Vector3f newPosition = position;
        for (int i = 0; i < futureTicks; i++) {
            newPosition = tickMovement(session, newPosition);
        }
        super.moveAbsoluteImmediate(session, newPosition, rotation, isOnGround, teleported);
        this.position = position;
        this.motion = lastMotion;
    }

    @Override
    public void tick(GeyserSession session) {
        moveAbsoluteImmediate(session, tickMovement(session, position), rotation, false, false);
    }
}
