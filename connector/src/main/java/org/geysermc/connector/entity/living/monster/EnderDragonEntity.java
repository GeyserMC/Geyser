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

package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.Data;
import org.geysermc.connector.entity.Tickable;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.living.InsentientEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.AttributeUtils;
import org.geysermc.connector.utils.DimensionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class EnderDragonEntity extends InsentientEntity implements Tickable {
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

    private int phase;
    /**
     * The number of ticks since the beginning of the phase
     */
    private int phaseTicks;

    private int ticksTillNextGrowl = 100;

    /**
     * Used to determine when the wing flap sound should be played
     */
    private float wingPosition;
    private float lastWingPosition;

    public EnderDragonEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        metadata.getFlags().setFlag(EntityFlag.FIRE_IMMUNE, true);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 16) { // Phase
            phase = (int) entityMetadata.getValue();
            phaseTicks = 0;
            metadata.getFlags().setFlag(EntityFlag.SITTING, isSitting());
        }

        super.updateBedrockMetadata(entityMetadata, session);

        if (entityMetadata.getId() == 9) { // Health
            // Update the health attribute, so that the death animation gets played
            // Round health up, so that Bedrock doesn't consider the dragon to be dead when health is between 0 and 1
            float health = (float) Math.ceil(metadata.getFloat(EntityData.HEALTH));
            if (phase == 9 && health <= 0) { // Dying phase
                EntityEventPacket entityEventPacket = new EntityEventPacket();
                entityEventPacket.setType(EntityEventType.ENDER_DRAGON_DEATH);
                entityEventPacket.setRuntimeEntityId(geyserId);
                entityEventPacket.setData(0);
                session.sendUpstreamPacket(entityEventPacket);
            }
            attributes.put(AttributeType.HEALTH, AttributeType.HEALTH.getAttribute(health, 200));
            updateBedrockAttributes(session);
        }
    }

    /**
     * Send an updated list of attributes to the Bedrock client.
     * This is overwritten to allow the health attribute to differ from
     * the health specified in the metadata.
     *
     * @param session GeyserSession
     */
    @Override
    public void updateBedrockAttributes(GeyserSession session) {
        if (!valid) return;

        List<AttributeData> attributes = new ArrayList<>();
        for (Map.Entry<AttributeType, org.geysermc.connector.entity.attribute.Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;
            attributes.add(AttributeUtils.getBedrockAttribute(entry.getValue()));
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(attributes);
        session.sendUpstreamPacket(updateAttributesPacket);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        super.spawnEntity(session);

        AtomicLong nextEntityId = session.getEntityCache().getNextEntityId();
        head = new EnderDragonPartEntity(entityId + 1, nextEntityId.incrementAndGet(), EntityType.ENDER_DRAGON_PART, 1, 1);
        neck = new EnderDragonPartEntity(entityId + 2, nextEntityId.incrementAndGet(), EntityType.ENDER_DRAGON_PART, 3, 3);
        body = new EnderDragonPartEntity(entityId + 3, nextEntityId.incrementAndGet(), EntityType.ENDER_DRAGON_PART, 5, 3);
        leftWing = new EnderDragonPartEntity(entityId + 4, nextEntityId.incrementAndGet(), EntityType.ENDER_DRAGON_PART, 4, 2);
        rightWing = new EnderDragonPartEntity(entityId + 5, nextEntityId.incrementAndGet(), EntityType.ENDER_DRAGON_PART, 4, 2);
        tail = new EnderDragonPartEntity[3];
        for (int i = 0; i < 3; i++) {
            tail[i] = new EnderDragonPartEntity(entityId + 6 + i, nextEntityId.incrementAndGet(), EntityType.ENDER_DRAGON_PART, 2, 2);
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
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        for (EnderDragonPartEntity part : allParts) {
            part.despawnEntity(session);
        }
        return super.despawnEntity(session);
    }

    @Override
    public void tick(GeyserSession session) {
        effectTick(session);
        if (!metadata.getFlags().getFlag(EntityFlag.NO_AI) && isAlive()) {
            pushSegment();
            updateBoundingBoxes(session);
        }
    }

    /**
     * Updates the positions of the Ender Dragon's multiple bounding boxes
     *
     * @param session GeyserSession.
     */
    private void updateBoundingBoxes(GeyserSession session) {
        Vector3f facingDir = Vector3f.createDirectionDeg(0, rotation.getZ());
        Segment baseSegment = getSegment(5);
        // Used to angle the head, neck, and tail when the dragon flies up/down
        float pitch = (float) Math.toRadians(10 * (baseSegment.getY() - getSegment(10).getY()));
        float pitchXZ = (float) Math.cos(pitch);
        float pitchY = (float) Math.sin(pitch);

        // Lowers the head when the dragon sits/hovers
        float headDuck;
        if (isHovering() || isSitting()) {
            headDuck = -1f;
        } else {
            headDuck = baseSegment.y - getSegment(0).y;
        }

        head.setPosition(facingDir.up(pitchY).mul(pitchXZ, 1, -pitchXZ).mul(6.5f).up(headDuck));
        neck.setPosition(facingDir.up(pitchY).mul(pitchXZ, 1, -pitchXZ).mul(5.5f).up(headDuck));
        body.setPosition(facingDir.mul(0.5f, 0f, -0.5f));

        Vector3f wingPos = Vector3f.createDirectionDeg(0, 90f - rotation.getZ()).mul(4.5f).up(2f);
        rightWing.setPosition(wingPos);
        leftWing.setPosition(wingPos.mul(-1, 1, -1)); // Mirror horizontally

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

    /**
     * Handles the particles and sounds of the Ender Dragon
     * @param session GeyserSession.
     */
    private void effectTick(GeyserSession session) {
        Random random = ThreadLocalRandom.current();
        if (!metadata.getFlags().getFlag(EntityFlag.SILENT)) {
            if (Math.cos(wingPosition * 2f * Math.PI) <= -0.3f && Math.cos(lastWingPosition * 2f * Math.PI) >= -0.3f) {
                PlaySoundPacket playSoundPacket = new PlaySoundPacket();
                playSoundPacket.setSound("mob.enderdragon.flap");
                playSoundPacket.setPosition(position);
                playSoundPacket.setVolume(5f);
                playSoundPacket.setPitch(0.8f + random.nextFloat() * 0.3f);
                session.sendUpstreamPacket(playSoundPacket);
            }

            if (!isSitting() && !isHovering() && ticksTillNextGrowl-- == 0) {
                playGrowlSound(session);
                ticksTillNextGrowl = 200 + random.nextInt(200);
            }

            lastWingPosition = wingPosition;
        }
        if (isAlive()) {
            if (metadata.getFlags().getFlag(EntityFlag.NO_AI)) {
                wingPosition = 0.5f;
            } else if (isHovering() || isSitting()) {
                wingPosition += 0.1f;
            } else {
                double speed = motion.length();
                wingPosition += 0.2f / (speed * 10f + 1) * Math.pow(2, motion.getY());
            }

            phaseTicks++;
            if (phase == 3) { // Landing Phase
                float headHeight = head.getMetadata().getFloat(EntityData.BOUNDING_BOX_HEIGHT);
                Vector3f headCenter = head.getPosition().up(headHeight * 0.5f);

                for (int i = 0; i < 8; i++) {
                    Vector3f particlePos = headCenter.add(random.nextGaussian() / 2f, random.nextGaussian() / 2f, random.nextGaussian() / 2f);
                    // This is missing velocity information
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(LevelEventType.PARTICLE_DRAGONS_BREATH);
                    particlePacket.setPosition(particlePos);
                    session.sendUpstreamPacket(particlePacket);
                }
            } else if (phase == 5) { // Sitting Flaming Phase
                if (phaseTicks % 2 == 0 && phaseTicks < 10) {
                    // Performing breath attack
                    // Entity event DRAGON_FLAMING seems to create particles from the origin of the dragon,
                    // so we need to manually spawn particles
                    for (int i = 0; i < 8; i++) {
                        SpawnParticleEffectPacket spawnParticleEffectPacket = new SpawnParticleEffectPacket();
                        spawnParticleEffectPacket.setDimensionId(DimensionUtils.javaToBedrock(session.getDimension()));
                        spawnParticleEffectPacket.setPosition(head.getPosition().add(random.nextGaussian() / 2f, random.nextGaussian() / 2f, random.nextGaussian() / 2f));
                        spawnParticleEffectPacket.setIdentifier("minecraft:dragon_breath_fire");
                        session.sendUpstreamPacket(spawnParticleEffectPacket);
                    }
                }
            } else if (phase == 7) { // Sitting Attacking Phase
                playGrowlSound(session);
            } else if (phase == 9) { // Dying Phase
                // Send explosion particles as the dragon move towards the end portal
                if (phaseTicks % 10 == 0) {
                    float xOffset = 8f * (random.nextFloat() - 0.5f);
                    float yOffset = 4f * (random.nextFloat() - 0.5f) + 2f;
                    float zOffset = 8f * (random.nextFloat() - 0.5f);
                    Vector3f particlePos = position.add(xOffset, yOffset, zOffset);
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(LevelEventType.PARTICLE_EXPLOSION);
                    particlePacket.setPosition(particlePos);
                    session.sendUpstreamPacket(particlePacket);
                }
            }
        }
    }

    private void playGrowlSound(GeyserSession session) {
        Random random = ThreadLocalRandom.current();
        PlaySoundPacket playSoundPacket = new PlaySoundPacket();
        playSoundPacket.setSound("mob.enderdragon.growl");
        playSoundPacket.setPosition(position);
        playSoundPacket.setVolume(2.5f);
        playSoundPacket.setPitch(0.8f + random.nextFloat() * 0.3f);
        session.sendUpstreamPacket(playSoundPacket);
    }

    private boolean isAlive() {
        return metadata.getFloat(EntityData.HEALTH) > 0;
    }

    private boolean isHovering() {
        return phase == 10;
    }

    private boolean isSitting() {
        return phase == 5 || phase == 6 || phase == 7;
    }

    /**
     * Store the current yaw and y into the circular buffer
     */
    private void pushSegment() {
        latestSegment = (latestSegment + 1) % segmentHistory.length;
        segmentHistory[latestSegment].yaw = rotation.getZ();
        segmentHistory[latestSegment].y = position.getY();
    }

    /**
     * Gets the previous yaw and y
     * Used to curl the tail and pitch the head and tail up/down
     *
     * @param index Number of ticks in the past
     * @return Segment with the yaw and y
     */
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
