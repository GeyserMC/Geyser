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
     * To simulate collision with the world border using the COLLIDABLE tag.
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

    /**
     * @return the closet distance to the border.
     */
    private double getDistanceToBorder(final double x, final double z) {
        double fromNorth = z - minZ;
        double fromSouth = maxZ - z;
        double fromWest = x - minX;
        double fromEast = maxX - x;
        double min = Math.min(fromWest, fromEast);
        min = Math.min(min, fromNorth);
        return Math.min(min, fromSouth);
    }

    /**
     * @return the position is within's bounds of the border within a certain margin.
     */
    private boolean isWithinBounds(final double x, final double z, final double margin) {
        return x >= this.minX - margin && x < maxX + margin && z >= minZ - margin && z < maxZ + margin;
    }

    /**
     * @return if the player is inside or close to the border, but not beyond it, use to determine whether the player should be able to collide with the border.
     */
    public boolean isInsideCloseToBorder() {
        SessionPlayerEntity player = session.getPlayerEntity();
        double bbMax = Math.max(Math.max(player.getBoundingBoxWidth(), player.getBoundingBoxHeight()), 1.0);
        return this.getDistanceToBorder(player.position().getX(), player.position().getZ()) < bbMax * 2.0 && this.isWithinBounds(player.position().getX(), player.position().getZ(), bbMax);
    }

    private static final int CLOSE_TO_BORDER = 5;

    /**
     * @return if the player is close to the border boundaries. Used to always indicate a border even if there is no
     * warning blocks set and spawn collision entities.
     */
    public boolean isCloseToBorderBoundaries() {
        Vector3f position = session.getPlayerEntity().position();
        return !(position.getX() > minX + CLOSE_TO_BORDER && position.getX() < maxX - CLOSE_TO_BORDER
            && position.getZ() > minZ + CLOSE_TO_BORDER && position.getZ() < maxZ - CLOSE_TO_BORDER);
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
     * Check if the player is passing into the border, mostly meant for vehicles where the collision hacks don't work.
     */
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

    /**
     * Removed the X collision and Z collision entity from cache if needed.
     */
    public void clearCollision() {
        this.xCollisionEntity = this.zCollisionEntity = null;
    }

    /**
     * Move the border (entity) according the player position so that the player able to collide with it.
     */
    public void moveWorldBorderCollision(Vector3f playerPosition) {
        final boolean insideCloseToBorder = isInsideCloseToBorder();

        boolean closeToX = !(playerPosition.getX() > minX + CLOSE_TO_BORDER && playerPosition.getX() < maxX - CLOSE_TO_BORDER) && insideCloseToBorder;
        boolean closeToZ = !(playerPosition.getZ() > minZ + CLOSE_TO_BORDER && playerPosition.getZ() < maxZ - CLOSE_TO_BORDER) && insideCloseToBorder;

        // If the player isn't close enough, then despawn the border since we don't need it.
        if (xCollisionEntity != null && !closeToX) {
            xCollisionEntity.despawnEntity();
            xCollisionEntity = null;
        }
        if (zCollisionEntity != null && !closeToZ) {
            zCollisionEntity.despawnEntity();
            zCollisionEntity = null;
        }

        // If the collision entity is far enough (which shouldn't happen unless teleport which will despawn the entity). Then respawn the entity again.
        if ((xCollisionEntity == null || xCollisionEntity.position().distance(playerPosition) > 300) && closeToX) {
            xCollisionEntity = buildCollisionEntity();
        }
        if ((zCollisionEntity == null || zCollisionEntity.position().distance(playerPosition) > 300) && closeToZ) {
            zCollisionEntity = buildCollisionEntity();
        }

        // We need to account for ULP, also since the bounding box is centered around the entity, it needs to be moved backwards by half the width to be correct.
        // Min Y collision also started at the entity feet, which is why we want to move it downwards, so that the player won't clip through the collision when falling.
        final float xDistance = 5 - Math.ulp(Math.abs(playerPosition.getX())), zDistance = 5 - Math.ulp(Math.abs(playerPosition.getZ()));
        if (xCollisionEntity != null) {
            if (playerPosition.getX() < Math.max(warningMinX, minX + CLOSE_TO_BORDER)) {
                xCollisionEntity.moveAbsolute(Vector3f.from(minX - xDistance, playerPosition.getY() - 20, playerPosition.getZ()), 0, 0, false, true);
            }

            if (playerPosition.getX() > Math.min(warningMaxX, maxX - CLOSE_TO_BORDER)) {
                xCollisionEntity.moveAbsolute(Vector3f.from(maxX + xDistance, playerPosition.getY() - 20, playerPosition.getZ()), 0, 0, false, true);
            }
        }
        if (zCollisionEntity != null) {
            if (playerPosition.getZ() > Math.min(warningMaxZ, maxZ - CLOSE_TO_BORDER)) {
                zCollisionEntity.moveAbsolute(Vector3f.from(playerPosition.getX(), playerPosition.getY() - 20, maxZ + zDistance), 0, 0, false, true);
            }
            if (playerPosition.getZ() < Math.max(warningMinZ, minZ + CLOSE_TO_BORDER)) {
                zCollisionEntity.moveAbsolute(Vector3f.from(playerPosition.getX(), playerPosition.getY() - 20, minZ - zDistance), 0, 0, false, true);
            }
        }
    }

    /**
     * Responsible for spawning the world border collision entity, and setting flags.
     */
    private Entity buildCollisionEntity() {
        Entity entity = new Entity(new EntitySpawnContext(session, EntityDefinitions.ARMOR_STAND, 0, null)); // Armor stand is just a placeholder, anything will works here.
        entity.setPosition(session.getPlayerEntity().getPosition().down(20)); // Initial position, will change.
        entity.setFlag(EntityFlag.COLLIDABLE, true);
        entity.setFlag(EntityFlag.INVISIBLE, true);

        // These values should be safe?
        entity.getDirtyMetadata().put(EntityDataTypes.HEIGHT, 100f);
        entity.getDirtyMetadata().put(EntityDataTypes.WIDTH, 10f);

        entity.spawnEntity();
        return entity;
    }
}
