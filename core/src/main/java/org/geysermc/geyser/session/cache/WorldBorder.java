/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.GeyserSession;

import java.awt.Color;

import static org.geysermc.geyser.level.physics.CollisionManager.COLLISION_TOLERANCE;

public class WorldBorder {
    private static final double DEFAULT_WORLD_BORDER_SIZE = 5.9999968E7D;
    private static final Color DEFAULT_WORLD_BORDER_COLOR = new Color(32, 160, 255);
    private static final Color SHRINKING_WORLD_BORDER_COLOR = new Color(255, 48, 48);
    private static final Color GROWING_WORLD_BORDER_COLOR = new Color(64, 255, 128);

    @Setter
    private @NonNull Vector2d center = Vector2d.ZERO;

    /**
     * Progress through the current movement
     */
    private long lerpProgress;

    /**
     * The duration of the current movement
     */
    private long lerpDuration;

    /**
     * The diameter in blocks of the new world border.
     */
    @Setter
    private double size = DEFAULT_WORLD_BORDER_SIZE;
    /**
     * The target diameter
     */
    @Setter
    private double to = DEFAULT_WORLD_BORDER_SIZE;
    /**
     * The diameter the current moving target came from
     */
    @Setter
    private double from = DEFAULT_WORLD_BORDER_SIZE;
    /**
     * Block length before you reach the border to show warning particles.
     */
    @Setter
    private int warningBlocks = 5;

    @Setter
    private int warningDelay = 15;
    /**
     * The world border cannot go beyond this number, positive or negative, in world coordinates
     */
    @Setter
    private int absoluteMaxSize = 29999984;

    @Setter
    @Getter
    private boolean resizing;

    /*
     * Boundaries of the actual world border.
     * (The will get updated on expanding or shrinking)
     */
    private double minX = 0.0D;
    private double minZ = 0.0D;
    private double maxX = 0.0D;
    private double maxZ = 0.0D;

    /*
     * The boundaries for the for the warning visuals.
     */
    private double warningMaxX = 0.0D;
    private double warningMaxZ = 0.0D;
    private double warningMinX = 0.0D;
    private double warningMinZ = 0.0D;

    @SuppressWarnings("FieldCanBeLocal") // We will use this at some point down the line for Integrated Pack
    private Color currentWorldBorderColor = DEFAULT_WORLD_BORDER_COLOR;

    /**
     * To track when to send wall particle packets.
     */
    private int currentWallTick;


    private final GeyserSession session;

    public WorldBorder(GeyserSession session) {
        this.session = session;
        // Initialize all min/max/warning variables
        update();
    }

    /**
     * @return true as long as the player entity is within the world limits.
     */
    public boolean isInsideBorderBoundaries() {
        return isInsideBorderBoundaries(session.getPlayerEntity().getPosition());
    }

    public boolean isInsideBorderBoundaries(Vector3f position) {
        return position.getX() > minX && position.getX() < maxX && position.getZ() > minZ && position.getZ() < maxZ;
    }

    private static final int CLOSE_TO_BORDER = 5;

    /**
     * @return if the player is close to the border boundaries. Used to always indicate a border even if there is no
     * warning blocks set.
     */
    public boolean isCloseToBorderBoundaries() {
        Vector3f position = session.getPlayerEntity().getPosition();
        return !(position.getX() > minX + CLOSE_TO_BORDER && position.getX() < maxX - CLOSE_TO_BORDER
            && position.getZ() > minZ + CLOSE_TO_BORDER && position.getZ() < maxZ - CLOSE_TO_BORDER);
    }

    /**
     * Confirms that the entity is within world border boundaries when they move.
     * Otherwise, if {@code adjustPosition} is true, this function will push the player back.
     *
     * @return if this player was indeed against the world border. Will return false if no world border was defined for us.
     */
    public boolean isPassingIntoBorderBoundaries(Vector3f newPosition, boolean adjustPosition) {
        boolean isInWorldBorder = isPassingIntoBorderBoundaries(newPosition);
        if (isInWorldBorder && adjustPosition) {
            PlayerEntity playerEntity = session.getPlayerEntity();
            // Move the player back, but allow gravity to take place
            // Teleported = true makes going back better, but disconnects the player from their mounted entity
            playerEntity.moveAbsolute(Vector3f.from(playerEntity.getPosition().getX(), newPosition.getY(), playerEntity.getPosition().getZ()),
                playerEntity.getYaw(), playerEntity.getPitch(), playerEntity.getHeadYaw(), playerEntity.isOnGround(), playerEntity.getVehicle() == null);
        }
        return isInWorldBorder;
    }

    public boolean isPassingIntoBorderBoundaries(Vector3f newEntityPosition) {
        int entityX = GenericMath.floor(newEntityPosition.getX());
        int entityZ = GenericMath.floor(newEntityPosition.getZ());
        Vector3f currentEntityPosition = session.getPlayerEntity().getPosition();
        // Make sure we can't move out of the world border, but if we're out of the world border, we can move in
        return (entityX == (int) minX && currentEntityPosition.getX() > newEntityPosition.getX()) ||
            (entityX == (int) maxX && currentEntityPosition.getX() < newEntityPosition.getX()) ||
            (entityZ == (int) minZ && currentEntityPosition.getZ() > newEntityPosition.getZ()) ||
            (entityZ == (int) maxZ && currentEntityPosition.getZ() < newEntityPosition.getZ());
    }

    /**
     * Same as {@link #isInsideBorderBoundaries()} but using the warning boundaries.
     *
     * @return true as long the entity is within the world limits and not in the warning zone at the edge to the border.
     */
    public boolean isWithinWarningBoundaries() {
        Vector3f entityPosition = session.getPlayerEntity().getPosition();
        return entityPosition.getX() > warningMinX && entityPosition.getX() < warningMaxX && entityPosition.getZ() > warningMinZ && entityPosition.getZ() < warningMaxZ;
    }

    /**
     * Adjusts the movement of an entity so that it does not cross the world border.
     *
     * @param boundingBox bounding box of the entity
     * @param movement movement of the entity
     * @return the corrected movement
     */
    public Vector3d correctMovement(BoundingBox boundingBox, Vector3d movement) {
        double correctedX;
        if (movement.getX() < 0) {
            correctedX = -limitMovement(-movement.getX(), boundingBox.getMin(Axis.X) - GenericMath.floor(minX));
        } else {
            correctedX = limitMovement(movement.getX(), GenericMath.ceil(maxX) - boundingBox.getMax(Axis.X));
        }

        // Outside of border, don't adjust movement
        if (Double.isNaN(correctedX)) {
            return movement;
        }

        double correctedZ;
        if (movement.getZ() < 0) {
            correctedZ = -limitMovement(-movement.getZ(), boundingBox.getMin(Axis.Z) - GenericMath.floor(minZ));
        } else {
            correctedZ = limitMovement(movement.getZ(), GenericMath.ceil(maxZ) - boundingBox.getMax(Axis.Z));
        }

        if (Double.isNaN(correctedZ)) {
            return movement;
        }

        return Vector3d.from(correctedX, movement.getY(), correctedZ);
    }

    private double limitMovement(double movement, double limit) {
        if (limit < 0) {
            // Return NaN to indicate outside of border
            return Double.NaN;
        }

        if (limit < COLLISION_TOLERANCE) {
            return 0;
        }

        return Math.min(movement, limit);
    }

    /**
     * Updates the world border's minimum and maximum properties
     */
    public void update() {
        /*
         * Setting the correct boundary of our world border's square.
         */
        double radius;
        if (resizing) {
            radius = this.size / 2.0D;
            if (this.size > this.to) {
                currentWorldBorderColor = SHRINKING_WORLD_BORDER_COLOR;
            } else {
                currentWorldBorderColor = GROWING_WORLD_BORDER_COLOR;
            }
        } else {
            radius = this.size / 2.0D;
            currentWorldBorderColor = DEFAULT_WORLD_BORDER_COLOR;
        }

        double absoluteMinSize = -this.absoluteMaxSize;

        // Mapping 2D vector to 3D coordinates >> Y becomes Z
        this.minX = GenericMath.clamp(this.center.getX() - radius, absoluteMinSize, this.absoluteMaxSize);
        this.minZ = GenericMath.clamp(this.center.getY() - radius, absoluteMinSize, this.absoluteMaxSize);
        this.maxX = GenericMath.clamp(this.center.getX() + radius, absoluteMinSize, this.absoluteMaxSize);
        this.maxZ = GenericMath.clamp(this.center.getY() + radius, absoluteMinSize, this.absoluteMaxSize);

        /*
         * Caching the warning boundaries.
         */
        this.warningMinX = this.minX + this.warningBlocks;
        this.warningMinZ = this.minZ + this.warningBlocks;
        this.warningMaxX = this.maxX - this.warningBlocks;
        this.warningMaxZ = this.maxZ - this.warningBlocks;
    }

    public void tick() {
        if (!resizing) return;
        this.lerpProgress++;
        this.size = this.calculateSize();
        if (this.lerpProgress >= this.lerpDuration) {
            this.resizing = false;
            this.lerpProgress = 0;
            this.lerpDuration = 0;
            this.from = this.to;
        } else {
            this.resizing = true;
        }
        update();
    }

    public void startResize(double from, double to, long lerpDuration) {
        this.from = from;
        this.to = to;
        this.lerpDuration = lerpDuration;
        this.lerpProgress = 0;
        this.resizing = true;
    }

    private double calculateSize() {
        double d0 = (this.lerpDuration - this.lerpProgress) / (double) this.lerpDuration;
        return d0 < 1.0 ? (this.to + d0 * (this.from - this.to)) : this.to;
    }

    /**
     * Draws a wall of particles where the world border resides
     */
    public void drawWall() {
        if (currentWallTick++ != 20) {
            // Only draw a wall once every second
            return;
        }
        currentWallTick = 0;
        Vector3f entityPosition = session.getPlayerEntity().getPosition();
        float particlePosX = entityPosition.getX();
        float particlePosY = entityPosition.getY();
        float particlePosZ = entityPosition.getZ();

        if (entityPosition.getX() > Math.min(warningMaxX, maxX - CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(maxX, particlePosY, particlePosZ), true);
        }
        if (entityPosition.getX() < Math.max(warningMinX, minX + CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(minX, particlePosY, particlePosZ), true);
        }
        if (entityPosition.getZ() > Math.min(warningMaxZ, maxZ - CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(particlePosX, particlePosY, maxZ), false);
        }
        if (entityPosition.getZ() < Math.max(warningMinZ, minZ + CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(particlePosX, particlePosY, minZ), false);
        }
    }

    private void drawWall(Vector3f position, boolean drawWallX) {
        int initialY = (int) (position.getY() - EntityDefinitions.PLAYER.offset() - 1);
        for (int y = initialY; y < (initialY + 5); y++) {
            if (drawWallX) {
                float x = position.getX();
                for (int z = (int) position.getZ() - 3; z < ((int) position.getZ() + 3); z++) {
                    if (z < minZ) {
                        continue;
                    }
                    if (z > maxZ) {
                        break;
                    }

                    sendWorldBorderParticle(x, y, z);
                }
            } else {
                float z = position.getZ();
                for (int x = (int) position.getX() - 3; x < ((int) position.getX() + 3); x++) {
                    if (x < minX) {
                        continue;
                    }
                    if (x > maxX) {
                        break;
                    }

                    sendWorldBorderParticle(x, y, z);
                }
            }
        }
    }

    private void sendWorldBorderParticle(float x, float y, float z) {
        LevelEventPacket effectPacket = new LevelEventPacket();
        effectPacket.setPosition(Vector3f.from(x, y, z));
        effectPacket.setType(LevelEvent.PARTICLE_DENY_BLOCK);
        session.getUpstream().sendPacket(effectPacket);
    }
}
