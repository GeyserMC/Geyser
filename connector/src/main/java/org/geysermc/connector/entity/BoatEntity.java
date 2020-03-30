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

public class BoatEntity extends Entity {

    private boolean is_paddling_left;
    private float paddle_time_left;
    private boolean is_paddling_right;
    private float paddle_time_right;

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
            System.out.println("'Paddle left' ID");
            is_paddling_left = (boolean) entityMetadata.getValue();
            System.out.println("Is paddling? " + is_paddling_left);
            if (!is_paddling_left) {
                metadata.put(EntityData.PADDLE_TIME_LEFT, 0f);
            }
            else {
                // TODO: Fix this.
                // There needs to be a way to count this up. I believe it's in seconds.
                metadata.put(EntityData.PADDLE_TIME_LEFT, 0.5f);
            }
//            if (!is_paddling_left) {
//                paddle_time_left = 0;
//                metadata.put(EntityData.PADDLE_TIME_LEFT, 0);
//            } else if (paddle_time_left == 0) {
//                paddle_time_left = System.currentTimeMillis() / 1000f;
//                metadata.put(EntityData.PADDLE_TIME_LEFT, paddle_time_left / 1000f - System.currentTimeMillis());
//                while (is_paddling_left) {
//                    try {
//                        System.out.println("Paddle time left: " + ((System.currentTimeMillis() - paddle_time_left) / 1000f));
//                        metadata.put(EntityData.PADDLE_TIME_LEFT, ((System.currentTimeMillis() - paddle_time_left) / 1000f));
//                        super.updateBedrockMetadata(entityMetadata, session);
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        break;
//                    }
//
//                }
//
//            }
        }
        else if (entityMetadata.getId() == 12) {
            System.out.println("'Paddle right' ID");
            is_paddling_right = (boolean) entityMetadata.getValue();
            System.out.println("Is paddling? " + is_paddling_right);
            if (!is_paddling_right) {
                metadata.put(EntityData.PADDLE_TIME_RIGHT, 0f);
            }
            else {
                metadata.put(EntityData.PADDLE_TIME_RIGHT, 0.5f);
            }
        } else if (entityMetadata.getId() == 8) {
            System.out.println("Forward: " + entityMetadata.getValue());
        }

        else {
            System.out.println("New metadata ID: " + entityMetadata.getId());
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }
}
