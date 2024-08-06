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
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;

public class WorldBorder {
    private static final double DEFAULT_WORLD_BORDER_SIZE = 5.9999968E7D;

    @Setter
    private @NonNull Vector2d center = Vector2d.ZERO;
    /**
     * The diameter in blocks of the world border before it got changed or similar to newDiameter if not changed.
     */
    @Setter
    private double oldDiameter = DEFAULT_WORLD_BORDER_SIZE;
    /**
     * The diameter in blocks of the new world border.
     */
    @Setter
    private double newDiameter = DEFAULT_WORLD_BORDER_SIZE;
    /**
     * The speed to apply an expansion/shrinking of the world border.
     * When a client joins they get the actual border oldDiameter and the time left to reach the newDiameter.
     */
    @Setter
    private long speed = 0;
    /**
     * The time in seconds before a shrinking world border would hit a not moving player.
     * Creates the same visual warning effect as warningBlocks.
     */
    @Setter
    private int warningDelay = 15;
    /**
     * Block length before you reach the border to show warning particles.
     */
    @Setter
    private int warningBlocks = 5;
    /**
     * The world border cannot go beyond this number, positive or negative, in world coordinates
     */
    @Setter
    private int absoluteMaxSize = 29999984;

    /**
     * The world coordinate scale as sent in the dimension registry. Used to scale the center X and Z.
     */
    private double worldCoordinateScale = 1.0D;

    @Getter
    private boolean resizing;
    private double currentDiameter;

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

    /**
     * To track when to send wall particle packets.
     */
    private int currentWallTick;

    /**
     * If the world border is resizing, this variable saves how many ticks have progressed in the resizing
     */
    private long lastUpdatedWorldBorderTime = 0;

    private final GeyserSession session;

    public WorldBorder(GeyserSession session) {
        this.session = session;
        // Initialize all min/max/warning variables
        update();
    }

    public void setWorldCoordinateScale(double worldCoordinateScale) {
        boolean needsUpdate = worldCoordinateScale != this.worldCoordinateScale;
        this.worldCoordinateScale = worldCoordinateScale;
        if (needsUpdate) {
            this.update();
        }
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
            playerEntity.moveAbsolute(Vector3f.from(playerEntity.getPosition().getX(), (newPosition.getY() - EntityDefinitions.PLAYER.offset()), playerEntity.getPosition().getZ()),
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
     * Updates the world border's minimum and maximum properties
     */
    public void update() {
        /*
         * Setting the correct boundary of our world border's square.
         */
        double radius;
        if (resizing) {
            radius = this.currentDiameter / 2.0D;
        } else {
            radius = this.newDiameter / 2.0D;
        }
        
        double absoluteMinSize = -this.absoluteMaxSize;
        // Used in the Nether by default
        double centerX = this.center.getX() / this.worldCoordinateScale;
        double centerZ = this.center.getY() / this.worldCoordinateScale; // Mapping 2D vector to 3D coordinates >> Y becomes Z

        this.minX = GenericMath.clamp(centerX - radius, absoluteMinSize, this.absoluteMaxSize);
        this.minZ = GenericMath.clamp(centerZ - radius, absoluteMinSize, this.absoluteMaxSize);
        this.maxX = GenericMath.clamp(centerX + radius, absoluteMinSize, this.absoluteMaxSize);
        this.maxZ = GenericMath.clamp(centerZ + radius, absoluteMinSize, this.absoluteMaxSize);

        /*
         * Caching the warning boundaries.
         */
        this.warningMinX = this.minX + this.warningBlocks;
        this.warningMinZ = this.minZ + this.warningBlocks;
        this.warningMaxX = this.maxX - this.warningBlocks;
        this.warningMaxZ = this.maxZ - this.warningBlocks;
    }

    public void resize() {
        if (this.lastUpdatedWorldBorderTime >= this.speed) {
            // Diameter has now updated to the new diameter
            this.resizing = false;
            this.lastUpdatedWorldBorderTime = 0;
        } else if (resizing) {
            this.currentDiameter = this.oldDiameter + ((double) this.lastUpdatedWorldBorderTime / (double) this.speed) * (this.newDiameter - this.oldDiameter);
            this.lastUpdatedWorldBorderTime += 50;
        }
        update();
    }

    public void setResizing(boolean resizing) {
        this.resizing = resizing;
        if (!resizing) {
            this.lastUpdatedWorldBorderTime = 0;
        }
    }

    private static final LevelEventType WORLD_BORDER_PARTICLE = LevelEvent.PARTICLE_DENY_BLOCK;

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
        effectPacket.setType(WORLD_BORDER_PARTICLE);
        session.getUpstream().sendPacket(effectPacket);
    }
}
