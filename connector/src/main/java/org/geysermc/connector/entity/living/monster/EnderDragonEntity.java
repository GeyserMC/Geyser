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
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.living.InsentientEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EnderDragonEntity extends InsentientEntity {
    private final BoundingBox head;
    private final BoundingBox neck;
    private final BoundingBox body;
    private final BoundingBox leftWing;
    private final BoundingBox rightWing;
    private final BoundingBox[] tail;

    private final BoundingBox[] allBoundingBoxes;

    private final Segment[] segmentHistory = new Segment[19];
    private int latestSegment = -1;
    private ScheduledFuture<?> segmentUpdater;

    private boolean hovering;

    public EnderDragonEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        this.head = new BoundingBox(entityId + 1, Vector3f.from(1f));
        this.neck = new BoundingBox(entityId + 2, Vector3f.from(3f));
        this.body = new BoundingBox(entityId + 3, Vector3f.from(5f, 3f, 5f));
        this.leftWing = new BoundingBox(entityId + 4, Vector3f.from(4f, 2f, 4f));
        this.rightWing = new BoundingBox(entityId + 5, Vector3f.from(4f, 2f, 4f));
        this.tail = new BoundingBox[3];
        for (int i = 0; i < 3; i++) {
            this.tail[i] = new BoundingBox(entityId + 6 + i, Vector3f.from(2f));
        }

        this.allBoundingBoxes = new BoundingBox[]{head, neck, body, leftWing, rightWing, tail[0], tail[1], tail[2]};
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        // Phase
        if (entityMetadata.getId() == 15) {
            metadata.getFlags().setFlag(EntityFlag.FIRE_IMMUNE, true);
            hovering = false;
            switch ((int) entityMetadata.getValue()) {
                // Performing breath attack
                case 5:
                    EntityEventPacket entityEventPacket = new EntityEventPacket();
                    entityEventPacket.setType(EntityEventType.DRAGON_FLAMING);
                    entityEventPacket.setRuntimeEntityId(geyserId);
                    entityEventPacket.setData(0);
                    session.sendUpstreamPacket(entityEventPacket);
                case 6:
                case 7:
                    metadata.getFlags().setFlag(EntityFlag.SITTING, true);
                    break;
                case 10:
                    hovering = true;
                    break;
            }
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

        for (int i = 0; i < segmentHistory.length; i++) {
            segmentHistory[i] = new Segment();
            segmentHistory[i].yaw = rotation.getZ();
            segmentHistory[i].y = position.getY();
        }
        segmentUpdater = session.getConnector().getGeneralThreadPool().scheduleAtFixedRate(this::pushSegment, 0, 50, TimeUnit.MILLISECONDS);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        segmentUpdater.cancel(true);
        return super.despawnEntity(session);
    }

    private void updateBoundingBoxes() {
        Vector3f facingDir = Vector3f.createDirectionDeg(0, rotation.getZ());
        Segment baseSegment = getSegment(5);

        float headBobbingAngle = (float) Math.toRadians(10 * (baseSegment.getY() - getSegment(10).getY()));
        float sway = (float) Math.cos(headBobbingAngle);
        float bobbing = (float) Math.sin(headBobbingAngle);

        float headDuck;
        if (hovering || metadata.getFlags().getFlag(EntityFlag.SITTING)) {
            headDuck = -1f;
        } else {
            headDuck = baseSegment.y - getSegment(0).y;
        }

        head.position = facingDir.up(bobbing).mul(6.5f, 6.5f, -6.5f).mul(sway, 1, sway).up(headDuck);
        neck.position = facingDir.up(bobbing).mul(5.5f, 5.5f, -5.5f).mul(sway, 1, sway).up(headDuck);

        body.position = facingDir.mul(0.5f, 0f, -0.5f);
        rightWing.position = Vector3f.createDirectionDeg(0, 90f - rotation.getZ()).mul(4.5f).up(2f);
        leftWing.position = rightWing.position.mul(-1, 1, -1);

        Vector3f tailBase = facingDir.mul(1.5f);
        for (int i = 0; i < tail.length; i++) {
            Segment targetSegment = getSegment(12 + 2 * i);
            float angle = rotation.getZ() + targetSegment.yaw - baseSegment.yaw;
            float distance = (i + 1) * 2f;
            float tailYOffset = targetSegment.y - baseSegment.y - (distance + 1.5f) * bobbing + 1.5f;
            tail[i].position = Vector3f.createDirectionDeg(0, angle).mul(distance).add(tailBase).mul(-sway, 1, sway).up(tailYOffset);
        }

        for (int i = 0; i < allBoundingBoxes.length; i++) {
            allBoundingBoxes[i].position = allBoundingBoxes[i].position.add(position);
        }
    }

    public void handleAttack(GeyserSession session) {
        updateBoundingBoxes();

        Vector3f eyePosition = session.getPlayerEntity().getEyePosition();
        float reachDistance = session.getGameMode() == GameMode.CREATIVE ? 25.0f : 20.25f;

        // TODO Stop kill aura
        BoundingBox closest = null;
        float shortestDistance = Float.MAX_VALUE;
        for (BoundingBox boundingBox : allBoundingBoxes) {
            Vector3f center = boundingBox.position.up(boundingBox.size.getY() * 0.5f);
            float distance = center.distanceSquared(eyePosition);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closest = boundingBox;
            }
        }

        if (closest != null && shortestDistance < reachDistance) {
            ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) (closest.entityId),
                    InteractAction.ATTACK, session.isSneaking());
            session.sendDownstreamPacket(attackPacket);
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

    @Getter
    private static class BoundingBox {
        private final long entityId;
        private final Vector3f size;

        // The bottom center of the bounding box
        @Setter
        private Vector3f position;

        BoundingBox(long entityId, Vector3f size) {
            this.entityId = entityId;
            this.size = size;
        }
    }
}
