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

package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import lombok.Data;
import org.geysermc.connector.entity.living.InsentientEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EnderDragonEntity extends InsentientEntity {
    /**
     * The Ender Dragon has multiple hit boxes, which
     * are each its own invisible entity
     */
    private EnderDragonPartEntity head;
    private EnderDragonPartEntity neck;
    private EnderDragonPartEntity body;
    private EnderDragonPartEntity leftWing;
    private EnderDragonPartEntity rightWing;
    private EnderDragonPartEntity[] tail;

    private EnderDragonPartEntity[] allParts;

    /**
     * A circular buffer that stores a history of
     * y and yaw values.
     */
    private final Segment[] segmentHistory = new Segment[19];
    private int latestSegment = -1;

    private boolean hovering;

    private ScheduledFuture<?> partPositionUpdater;

    public EnderDragonEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        metadata.getFlags().setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        // Phase
        if (entityMetadata.getId() == 15) {
            int value = (int) entityMetadata.getValue();
            if (value == 5) {
                // Performing breath attack
                EntityEventPacket entityEventPacket = new EntityEventPacket();
                entityEventPacket.setType(EntityEventType.DRAGON_FLAMING);
                entityEventPacket.setRuntimeEntityId(geyserId);
                entityEventPacket.setData(0);
                session.sendUpstreamPacket(entityEventPacket);
            }
            metadata.getFlags().setFlag(EntityFlag.SITTING, value == 5 || value == 6 || value == 7);
            hovering = value == 10;
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setIdentifier("minecraft:" + entityType.name().toLowerCase());
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.setEntityType(entityType.getType());
        addEntityPacket.getMetadata().putAll(metadata);

        // Otherwise dragon is always 'dying'
        addEntityPacket.getAttributes().add(new AttributeData("minecraft:health", 0.0f, 200f, 200f, 200f));

        valid = true;
        session.sendUpstreamPacket(addEntityPacket);

        head = new EnderDragonPartEntity(entityId + 1, session.getEntityCache().getNextEntityId().incrementAndGet(), EntityType.ENDER_DRAGON_PART, position, motion, rotation, 1, 1);
        neck = new EnderDragonPartEntity(entityId + 2, session.getEntityCache().getNextEntityId().incrementAndGet(), EntityType.ENDER_DRAGON_PART, position, motion, rotation, 3, 3);
        body = new EnderDragonPartEntity(entityId + 3, session.getEntityCache().getNextEntityId().incrementAndGet(), EntityType.ENDER_DRAGON_PART, position, motion, rotation, 5, 3);
        leftWing = new EnderDragonPartEntity(entityId + 4, session.getEntityCache().getNextEntityId().incrementAndGet(), EntityType.ENDER_DRAGON_PART, position, motion, rotation, 4, 2);
        rightWing = new EnderDragonPartEntity(entityId + 5, session.getEntityCache().getNextEntityId().incrementAndGet(), EntityType.ENDER_DRAGON_PART, position, motion, rotation, 4, 2);
        tail = new EnderDragonPartEntity[3];
        for (int i = 0; i < 3; i++) {
            tail[i] = new EnderDragonPartEntity(entityId + 6 + i, session.getEntityCache().getNextEntityId().incrementAndGet(), EntityType.ENDER_DRAGON_PART, position, motion, rotation, 2, 2);
        }

        allParts = new EnderDragonPartEntity[]{head, neck, body, leftWing, rightWing, tail[0], tail[1], tail[2]};

        for (EnderDragonPartEntity part : allParts) {
            session.getEntityCache().spawnEntity(part);
        }

        for (int i = 0; i < segmentHistory.length; i++) {
            segmentHistory[i] = new Segment();
            segmentHistory[i].yaw = rotation.getZ();
            segmentHistory[i].y = position.getY();
        }

        partPositionUpdater = session.getConnector().getGeneralThreadPool().scheduleAtFixedRate(() -> {
            pushSegment();
            updateBoundingBoxes(session);
        }, 0, 50, TimeUnit.MILLISECONDS);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        partPositionUpdater.cancel(true);

        for (EnderDragonPartEntity part : allParts) {
            part.despawnEntity(session);
        }
        return super.despawnEntity(session);
    }

    private void updateBoundingBoxes(GeyserSession session) {
        Vector3f facingDir = Vector3f.createDirectionDeg(0, rotation.getZ());
        Segment baseSegment = getSegment(5);
        // Used to angle the head, neck, and tail when the dragon flies up/down
        float pitch = (float) Math.toRadians(10 * (baseSegment.getY() - getSegment(10).getY()));
        float pitchXZ = (float) Math.cos(pitch);
        float pitchY = (float) Math.sin(pitch);

        // Lowers the head when the dragon sits/hovers
        float headDuck;
        if (hovering || metadata.getFlags().getFlag(EntityFlag.SITTING)) {
            headDuck = -1f;
        } else {
            headDuck = baseSegment.y - getSegment(0).y;
        }

        head.setPosition(facingDir.up(pitchY).mul(pitchXZ, 1, -pitchXZ).mul(6.5f).up(headDuck));
        neck.setPosition(facingDir.up(pitchY).mul(pitchXZ, 1, -pitchXZ).mul(5.5f).up(headDuck));
        body.setPosition(facingDir.mul(0.5f, 0f, -0.5f));

        rightWing.setPosition(Vector3f.createDirectionDeg(0, 90f - rotation.getZ()).mul(4.5f).up(2f));
        leftWing.setPosition(rightWing.getPosition().mul(-1, 1, -1)); // Mirror horizontally

        Vector3f tailBase = facingDir.mul(1.5f);
        for (int i = 0; i < tail.length; i++) {
            float distance = (i + 1) * 2f;
            // Curls the tail when the dragon turns
            Segment targetSegment = getSegment(12 + 2 * i);
            float angle = rotation.getZ() + targetSegment.yaw - baseSegment.yaw;

            float tailYOffset = targetSegment.y - baseSegment.y - (distance + 1.5f) * pitchY + 1.5f;
            tail[i].setPosition(Vector3f.createDirectionDeg(0, angle).mul(distance).add(tailBase).mul(-pitchXZ, 1, pitchXZ).up(tailYOffset));
        }
        // Send updated positions
        for (EnderDragonPartEntity part : allParts) {
             part.moveAbsolute(session, part.getPosition().add(position), Vector3f.ZERO, false, false);
        }
    }

    private void pushSegment() {
        latestSegment = (latestSegment + 1) % segmentHistory.length;
        segmentHistory[latestSegment].yaw = rotation.getZ();
        segmentHistory[latestSegment].y = position.getY();
    }

    private Segment getSegment(int index) {
        index = (latestSegment - index) % segmentHistory.length;
        if (index < 0) {
            index += segmentHistory.length;
        }
        return segmentHistory[index];
    }

    @Data
    private static class Segment {
        private float yaw;
        private float y;
    }
}
