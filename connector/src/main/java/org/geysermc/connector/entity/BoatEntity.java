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
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityEventType;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.TimeUnit;

public class BoatEntity extends Entity {

    private boolean is_paddling_left;
    private float paddle_time_left;
    private boolean is_paddling_right;
    private float paddle_time_right;

    // Looks too fast and too choppy with 0.1f, which is how I believe the Microsoftian client handles it
    private final float ROWING_SPEED = 0.05f;

    public BoatEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position.add(0d, entityType.getOffset(), 0d), motion, rotation);
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround) {
        super.moveAbsolute(session, position.add(0d, this.entityType.getOffset(), 0d), rotation, isOnGround);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 10) {
            metadata.put(EntityData.VARIANT, (int) entityMetadata.getValue());
        } else if (entityMetadata.getId() == 11) {
            is_paddling_left = (boolean) entityMetadata.getValue();
            if (!is_paddling_left) {
                metadata.put(EntityData.PADDLE_TIME_LEFT, 0f);
            }
            else {
                // Java sends simply "true" and "false" (is_paddling_left), Bedrock keeps sending packets as you're rowing
                // This is an asynchronous method that emulates Bedrock rowing until "false" is sent.
                paddle_time_left = 0f;
                session.getConnector().getGeneralThreadPool().schedule(() ->
                                updateLeftPaddle(session, entityMetadata),
                        100,
                        TimeUnit.MILLISECONDS
                );
            }
        }
        else if (entityMetadata.getId() == 12) {
            is_paddling_right = (boolean) entityMetadata.getValue();
            if (!is_paddling_right) {
                metadata.put(EntityData.PADDLE_TIME_RIGHT, 0f);
            }
            else {
                paddle_time_right = 0f;
                session.getConnector().getGeneralThreadPool().schedule(() ->
                                updateRightPaddle(session, entityMetadata),
                        100,
                        TimeUnit.MILLISECONDS
                );
            }
        } else if (entityMetadata.getId() == 8) {
            // TODO: Called only on login, do we need this?
            System.out.println("Forward: " + entityMetadata.getValue());
        } else if (entityMetadata.getId() == 13) {
            // Possibly
            metadata.put(EntityData.BOAT_BUBBLE_TIME, entityMetadata.getValue());
        }
        else if (entityMetadata.getId() > 6){
            // TODO: Also called at login - see if we can fill these out
            System.out.println("New metadata ID: " + entityMetadata.getId());
            System.out.println("Value: " + entityMetadata.getValue());
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    public void updateLeftPaddle(GeyserSession session, EntityMetadata entityMetadata) {
        while (is_paddling_left) {
            try {
                paddle_time_left += ROWING_SPEED;
                metadata.put(EntityData.PADDLE_TIME_LEFT, paddle_time_left);
                super.updateBedrockMetadata(entityMetadata, session);
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }

    public void updateRightPaddle(GeyserSession session, EntityMetadata entityMetadata) {
        while (is_paddling_right) {
            try {
                paddle_time_right += ROWING_SPEED;
                metadata.put(EntityData.PADDLE_TIME_RIGHT, paddle_time_right);
                super.updateBedrockMetadata(entityMetadata, session);
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }
}
