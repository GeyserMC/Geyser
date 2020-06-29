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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.TimeUnit;

public class BoatEntity extends Entity {

    private boolean isPaddlingLeft;
    private float paddleTimeLeft;
    private boolean isPaddlingRight;
    private float paddleTimeRight;

    // Looks too fast and too choppy with 0.1f, which is how I believe the Microsoftian client handles it
    private final float ROWING_SPEED = 0.05f;

    public BoatEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position.add(0d, entityType.getOffset(), 0d), motion, rotation.add(90, 0, 90));
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        // We don't include the rotation (y) as it causes the boat to appear sideways
        super.moveAbsolute(session, position.add(0d, this.entityType.getOffset(), 0d), Vector3f.from(rotation.getX() + 90, 0, rotation.getX() + 90), isOnGround, teleported);
    }

    @Override
    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        super.moveRelative(session, relX, relY, relZ, Vector3f.from(rotation.getX(), 0, rotation.getX()), isOnGround);
    }

    @Override
    public void updatePositionAndRotation(GeyserSession session, double moveX, double moveY, double moveZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(session, moveX, moveY, moveZ, yaw + 90, pitch, isOnGround);
    }

    @Override
    public void updateRotation(GeyserSession session, float yaw, float pitch, boolean isOnGround) {
        moveRelative(session, 0, 0, 0, Vector3f.from(yaw + 90, 0, 0), isOnGround);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {

        // Time since last hit
        if (entityMetadata.getId() == 7) {
            metadata.put(EntityData.HURT_TIME, entityMetadata.getValue());
        }

        // Rocking direction
        if (entityMetadata.getId() == 8) {
            metadata.put(EntityData.HURT_DIRECTION, entityMetadata.getValue());
        }

        // 'Health' in Bedrock, damage taken in Java
        if (entityMetadata.getId() == 9) {
            // Not exactly health but it makes motion in Bedrock
            metadata.put(EntityData.HEALTH, 40 - ((int) (float) entityMetadata.getValue()));
        }

        if (entityMetadata.getId() == 10) {
            metadata.put(EntityData.VARIANT, entityMetadata.getValue());
        } else if (entityMetadata.getId() == 11) {
            isPaddlingLeft = (boolean) entityMetadata.getValue();
            if (!isPaddlingLeft) {
                metadata.put(EntityData.ROW_TIME_LEFT, 0f);
            }
            else {
                // Java sends simply "true" and "false" (is_paddling_left), Bedrock keeps sending packets as you're rowing
                // This is an asynchronous method that emulates Bedrock rowing until "false" is sent.
                paddleTimeLeft = 0f;
                session.getConnector().getGeneralThreadPool().execute(() ->
                        updateLeftPaddle(session, entityMetadata)
                );
            }
        }
        else if (entityMetadata.getId() == 12) {
            isPaddlingRight = (boolean) entityMetadata.getValue();
            if (!isPaddlingRight) {
                metadata.put(EntityData.ROW_TIME_RIGHT, 0f);
            } else {
                paddleTimeRight = 0f;
                session.getConnector().getGeneralThreadPool().execute(() ->
                        updateRightPaddle(session, entityMetadata)
                );
            }
        } else if (entityMetadata.getId() == 13) {
            // Possibly - I don't think this does anything?
            metadata.put(EntityData.BOAT_BUBBLE_TIME, entityMetadata.getValue());
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    public void updateLeftPaddle(GeyserSession session, EntityMetadata entityMetadata) {
        if (isPaddlingLeft) {
            paddleTimeLeft += ROWING_SPEED;
            metadata.put(EntityData.ROW_TIME_LEFT, paddleTimeLeft);
            super.updateBedrockMetadata(entityMetadata, session);
            session.getConnector().getGeneralThreadPool().schedule(() ->
                    updateLeftPaddle(session, entityMetadata),
                    100,
                    TimeUnit.MILLISECONDS
            );
        }}

    public void updateRightPaddle(GeyserSession session, EntityMetadata entityMetadata) {
        if (isPaddlingRight) {
            paddleTimeRight += ROWING_SPEED;
            metadata.put(EntityData.ROW_TIME_RIGHT, paddleTimeRight);
            super.updateBedrockMetadata(entityMetadata, session);
            session.getConnector().getGeneralThreadPool().schedule(() ->
                            updateRightPaddle(session, entityMetadata),
                    100,
                    TimeUnit.MILLISECONDS
            );
        }}
}
