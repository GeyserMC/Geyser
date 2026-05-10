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

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
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

    /**
     * To simulate collision with the world border.
     */
    private Entity xCollisionEntity, zCollisionEntity;

    public WorldBorder(GeyserSession session) {
        this.session = session;
        // Initialize all min/max/warning variables
        update();
    }

    /**
     * @return true as long as the player entity is within the world limits.
     */
    public boolean isInsideBorderBoundaries() {
        return isInsideBorderBoundaries(session.getPlayerEntity().position());
    }

    public boolean isInsideBorderBoundaries(Vector3f position) {
        return position.getX() > minX && position.getX() < maxX && position.getZ() > minZ && position.getZ() < maxZ;
    }

    private static final int CLOSE_TO_BORDER = 5;

    private double getDistanceToBorder(final double x, final double z) {
        double fromNorth = z - minZ;
        double fromSouth = maxZ - z;
        double fromWest = x - minX;
        double fromEast = maxX - x;
        double min = Math.min(fromWest, fromEast);
        min = Math.min(min, fromNorth);
        return Math.min(min, fromSouth);
    }

    private boolean isWithinBounds(final double x, final double z, final double margin) {
        return x >= this.minX - margin && x < maxX + margin && z >= minZ - margin && z < maxZ + margin;
    }

    public boolean isInsideCloseToBorder() {
        SessionPlayerEntity player = session.getPlayerEntity();
        double bbMax = Math.max(Math.max(player.getBoundingBoxWidth(), player.getBoundingBoxHeight()), 1.0);
        return this.getDistanceToBorder(player.position().getX(), player.position().getZ())
            < bbMax * 2.0 && this.isWithinBounds(player.position().getX(), player.position().getZ(), bbMax);
    }

    /**
     * @return if the player is close to the border boundaries. Used to always indicate a border even if there is no
     * warning blocks set.
     */
    public boolean isCloseToBorderBoundaries() {
        Vector3f position = session.getPlayerEntity().position();
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
            Vector3f combinedPosition = Vector3f.from(playerEntity.position().getX(), newPosition.getY(), playerEntity.position().getZ());
            playerEntity.moveAbsoluteRaw(combinedPosition, playerEntity.getYaw(), playerEntity.getPitch(), playerEntity.getHeadYaw(), playerEntity.isOnGround(), playerEntity.getVehicle() == null);
        }
        return isInWorldBorder;
    }

    public boolean isPassingIntoBorderBoundaries(Vector3f newEntityPosition) {
        int entityX = GenericMath.floor(newEntityPosition.getX());
        int entityZ = GenericMath.floor(newEntityPosition.getZ());
        Vector3f currentEntityPosition = session.getPlayerEntity().position();
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
        Vector3f entityPosition = session.getPlayerEntity().position();
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
        double radius = this.size / 2.0D;
        if (resizing) {
            if (this.size > this.to) {
                currentWorldBorderColor = SHRINKING_WORLD_BORDER_COLOR;
            } else {
                currentWorldBorderColor = GROWING_WORLD_BORDER_COLOR;
            }
        } else {
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

    public void resetCollisionEntity() {
        this.xCollisionEntity = this.zCollisionEntity = null;
    }

    public void moveWorldBorderCollision(Vector3f playerPosition) {
        if (!isCloseToBorderBoundaries()) {
            return;
        }

        boolean closeToX =
            !(playerPosition.getX() > minX + CLOSE_TO_BORDER && playerPosition.getX() < maxX - CLOSE_TO_BORDER) && isInsideCloseToBorder();
        boolean closeToZ =
            !(playerPosition.getZ() > minZ + CLOSE_TO_BORDER && playerPosition.getZ() < maxZ - CLOSE_TO_BORDER) && isInsideCloseToBorder();

        if (xCollisionEntity != null && !closeToX) {
            xCollisionEntity.despawnEntity();
            xCollisionEntity = null;
        }
        if (zCollisionEntity != null && !closeToZ) {
            zCollisionEntity.despawnEntity();
            zCollisionEntity = null;
        }

        if ((xCollisionEntity == null || xCollisionEntity.position().distance(playerPosition) > 300) && closeToX) {
            xCollisionEntity = buildCollisionEntity();
        }
        if ((zCollisionEntity == null || zCollisionEntity.position().distance(playerPosition) > 300) && closeToZ) {
            zCollisionEntity = buildCollisionEntity();
        }

        if (xCollisionEntity != null && playerPosition.getX() < Math.max(warningMinX, minX + CLOSE_TO_BORDER)) {
            xCollisionEntity.moveAbsolute(Vector3f.from(minX - 4.999, playerPosition.getY() - 20, playerPosition.getZ()), 0, 0, false, true);
        }

        if (xCollisionEntity != null && playerPosition.getX() > Math.min(warningMaxX, maxX - CLOSE_TO_BORDER)) {
            xCollisionEntity.moveAbsolute(Vector3f.from(maxX + 4.999, playerPosition.getY() - 20, playerPosition.getZ()), 0, 0, false, true);
        }

        if (zCollisionEntity != null && playerPosition.getZ() > Math.min(warningMaxZ, maxZ - CLOSE_TO_BORDER)) {
            zCollisionEntity.moveAbsolute(Vector3f.from(playerPosition.getX(), playerPosition.getY() - 20, maxZ + 4.999), 0, 0, false, true);
        }
        if (zCollisionEntity != null && playerPosition.getZ() < Math.max(warningMinZ, minZ + CLOSE_TO_BORDER)) {
            zCollisionEntity.moveAbsolute(Vector3f.from(playerPosition.getX(), playerPosition.getY() - 20, minZ - 4.999), 0, 0, false, true);
        }
    }

    private Entity buildCollisionEntity() {
        Entity entity = new Entity(new EntitySpawnContext(session, EntityDefinitions.ARMOR_STAND, 0, null));
        entity.setPosition(session.getPlayerEntity().getPosition().up(5)); // Initial position, will change.
        entity.setFlag(EntityFlag.COLLIDABLE, true);
        entity.setFlag(EntityFlag.INVISIBLE, true);
        entity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 100f);
        entity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 10f);

        entity.spawnEntity();
        return entity;
    }

    public void tick() {
        if (!resizing) {
            return;
        }
        this.lerpProgress++;
        this.size = this.calculateSize();
        if (this.lerpProgress >= this.lerpDuration) {
            stopResize(to);
        } else {
            this.resizing = true;
        }
        update();
    }

    public void createStatic(double size) {
        stopResize(size);
        this.size = size;
        this.update();
    }

    public void stopResize(double newSize) {
        this.resizing = false;
        this.lerpProgress = 0;
        this.lerpDuration = 0;
        this.from = newSize;
        this.to = newSize;
    }

    public void createMoving(double from, double to, long lerpDuration) {
        this.size = from;
        this.from = from;
        this.to = to;
        this.lerpDuration = lerpDuration;
        this.lerpProgress = 0;
        this.resizing = true;
        this.update();
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
        Vector3f entityPosition = session.getPlayerEntity().position();
        float particlePosX = entityPosition.getX();
        float particlePosY = entityPosition.getY();
        float particlePosZ = entityPosition.getZ();

        if (particlePosX > Math.min(warningMaxX, maxX - CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(maxX + 0.5f, particlePosY, particlePosZ), true);
        }
        if (particlePosX < Math.max(warningMinX, minX + CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(minX - 0.5, particlePosY, particlePosZ), true);
        }
        if (particlePosZ > Math.min(warningMaxZ, maxZ - CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(particlePosX, particlePosY, maxZ + 0.5f), false);
        }
        if (particlePosZ < Math.max(warningMinZ, minZ + CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(particlePosX, particlePosY, minZ - 0.5f), false);
        }
    }

    private void drawWall(Vector3f position, boolean drawWallX) {
        int initialY = (int) (position.getY() - 1);
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
