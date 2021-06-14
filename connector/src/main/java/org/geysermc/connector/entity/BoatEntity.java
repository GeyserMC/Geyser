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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.packet.AnimatePacket;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.TimeUnit;

public class BoatEntity extends Entity {

    /**
     * Required when IS_BUOYANT is sent in order for boats to work in the water. <br>
     *
     * Taken from BDS 1.16.200, with the modification of <code>simulate_waves</code> since Java doesn't bob the boat up and down
     * like Bedrock.
     */
    private static final String BUOYANCY_DATA = "{\"apply_gravity\":true,\"base_buoyancy\":1.0,\"big_wave_probability\":0.02999999932944775," +
            "\"big_wave_speed\":10.0,\"drag_down_on_buoyancy_removed\":0.0,\"liquid_blocks\":[\"minecraft:water\"," +
            "\"minecraft:flowing_water\"],\"simulate_waves\":false}";

    private boolean isPaddlingLeft;
    private float paddleTimeLeft;
    private boolean isPaddlingRight;
    private float paddleTimeRight;

    // Looks too fast and too choppy with 0.1f, which is how I believe the Microsoftian client handles it
    private final float ROWING_SPEED = 0.05f;

    public BoatEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position.add(0d, entityType.getOffset(), 0d), motion, rotation.add(90, 0, 90));

        // Required to be able to move on land 1.16.200+ or apply gravity not in the water 1.16.100+
        metadata.put(EntityData.IS_BUOYANT, (byte) 1);
        metadata.put(EntityData.BUOYANCY_DATA, BUOYANCY_DATA);
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        // We don't include the rotation (y) as it causes the boat to appear sideways
        setPosition(position.add(0d, this.entityType.getOffset(), 0d));
        setRotation(Vector3f.from(rotation.getX() + 90, 0, rotation.getX() + 90));
        setOnGround(isOnGround);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        // Minimal glitching when ServerVehicleMovePacket is sent
        moveEntityPacket.setPosition(session.getRidingVehicleEntity() == this ? position.up(EntityType.PLAYER.getOffset() - this.entityType.getOffset()) : this.position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(teleported);

        session.sendUpstreamPacket(moveEntityPacket);
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
        if (entityMetadata.getId() == 8) {
            metadata.put(EntityData.HURT_TIME, entityMetadata.getValue());
        }

        // Rocking direction
        if (entityMetadata.getId() == 9) {
            metadata.put(EntityData.HURT_DIRECTION, entityMetadata.getValue());
        }

        // 'Health' in Bedrock, damage taken in Java
        if (entityMetadata.getId() == 10) {
            // Not exactly health but it makes motion in Bedrock
            metadata.put(EntityData.HEALTH, 40 - ((int) (float) entityMetadata.getValue()));
        }

        if (entityMetadata.getId() == 11) {
            metadata.put(EntityData.VARIANT, entityMetadata.getValue());
        } else if (entityMetadata.getId() == 12) {
            isPaddlingLeft = (boolean) entityMetadata.getValue();
            if (isPaddlingLeft) {
                // Java sends simply "true" and "false" (is_paddling_left), Bedrock keeps sending packets as you're rowing
                // This is an asynchronous method that emulates Bedrock rowing until "false" is sent.
                paddleTimeLeft = 0f;
                if (!this.passengers.isEmpty()) {
                    // Get the entity by the first stored passenger and convey motion in this manner
                    Entity entity = session.getEntityCache().getEntityByJavaId(this.passengers.iterator().nextLong());
                    if (entity != null) {
                        session.getConnector().getGeneralThreadPool().execute(() ->
                                updateLeftPaddle(session, entity)
                        );
                    }
                }
            } else {
                // Indicate that the row position should be reset
                metadata.put(EntityData.ROW_TIME_LEFT, 0.0f);
            }
        }
        else if (entityMetadata.getId() == 13) {
            isPaddlingRight = (boolean) entityMetadata.getValue();
            if (isPaddlingRight) {
                paddleTimeRight = 0f;
                if (!this.passengers.isEmpty()) {
                    Entity entity = session.getEntityCache().getEntityByJavaId(this.passengers.iterator().nextLong());
                    if (entity != null) {
                        session.getConnector().getGeneralThreadPool().execute(() ->
                                updateRightPaddle(session, entity)
                        );
                    }
                }
            } else {
                metadata.put(EntityData.ROW_TIME_RIGHT, 0.0f);
            }
        } else if (entityMetadata.getId() == 14) {
            // Possibly - I don't think this does anything?
            metadata.put(EntityData.BOAT_BUBBLE_TIME, entityMetadata.getValue());
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void updateBedrockMetadata(GeyserSession session) {
        super.updateBedrockMetadata(session);

        // As these indicate to reset rowing, remove them until it is time to send them out again.
        metadata.remove(EntityData.ROW_TIME_LEFT);
        metadata.remove(EntityData.ROW_TIME_RIGHT);
    }

    private void updateLeftPaddle(GeyserSession session, Entity rower) {
        if (isPaddlingLeft) {
            paddleTimeLeft += ROWING_SPEED;
            sendAnimationPacket(session, rower, AnimatePacket.Action.ROW_LEFT, paddleTimeLeft);

            session.getConnector().getGeneralThreadPool().schedule(() ->
                    updateLeftPaddle(session, rower),
                    100,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void updateRightPaddle(GeyserSession session, Entity rower) {
        if (isPaddlingRight) {
            paddleTimeRight += ROWING_SPEED;
            sendAnimationPacket(session, rower, AnimatePacket.Action.ROW_RIGHT, paddleTimeRight);

            session.getConnector().getGeneralThreadPool().schedule(() ->
                            updateRightPaddle(session, rower),
                    100,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void sendAnimationPacket(GeyserSession session, Entity rower, AnimatePacket.Action action, float rowTime) {
        AnimatePacket packet = new AnimatePacket();
        packet.setRuntimeEntityId(rower.getGeyserId());
        packet.setAction(action);
        packet.setRowingTime(rowTime);
        session.sendUpstreamPacket(packet);
    }
}
