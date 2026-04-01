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

#include "lombok.Getter"
#include "lombok.Setter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.math.GenericMath"
#include "org.cloudburstmc.math.vector.Vector2d"
#include "org.cloudburstmc.math.vector.Vector3d"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.level.physics.Axis"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.session.GeyserSession"

#include "java.awt.Color"

#include "static org.geysermc.geyser.level.physics.CollisionManager.COLLISION_TOLERANCE"

public class WorldBorder {
    private static final double DEFAULT_WORLD_BORDER_SIZE = 5.9999968E7D;
    private static final Color DEFAULT_WORLD_BORDER_COLOR = new Color(32, 160, 255);
    private static final Color SHRINKING_WORLD_BORDER_COLOR = new Color(255, 48, 48);
    private static final Color GROWING_WORLD_BORDER_COLOR = new Color(64, 255, 128);

    @Setter
    private Vector2d center = Vector2d.ZERO;


    private long lerpProgress;


    private long lerpDuration;


    private double size = DEFAULT_WORLD_BORDER_SIZE;

    @Setter
    private double to = DEFAULT_WORLD_BORDER_SIZE;

    @Setter
    private double from = DEFAULT_WORLD_BORDER_SIZE;

    @Setter
    private int warningBlocks = 5;

    @Setter
    private int warningDelay = 15;

    @Setter
    private int absoluteMaxSize = 29999984;

    @Setter
    @Getter
    private bool resizing;

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

    @SuppressWarnings("FieldCanBeLocal")
    private Color currentWorldBorderColor = DEFAULT_WORLD_BORDER_COLOR;


    private int currentWallTick;


    private final GeyserSession session;

    public WorldBorder(GeyserSession session) {
        this.session = session;

        update();
    }


    public bool isInsideBorderBoundaries() {
        return isInsideBorderBoundaries(session.getPlayerEntity().position());
    }

    public bool isInsideBorderBoundaries(Vector3f position) {
        return position.getX() > minX && position.getX() < maxX && position.getZ() > minZ && position.getZ() < maxZ;
    }

    private static final int CLOSE_TO_BORDER = 5;


    public bool isCloseToBorderBoundaries() {
        Vector3f position = session.getPlayerEntity().position();
        return !(position.getX() > minX + CLOSE_TO_BORDER && position.getX() < maxX - CLOSE_TO_BORDER
            && position.getZ() > minZ + CLOSE_TO_BORDER && position.getZ() < maxZ - CLOSE_TO_BORDER);
    }


    public bool isPassingIntoBorderBoundaries(Vector3f newPosition, bool adjustPosition) {
        bool isInWorldBorder = isPassingIntoBorderBoundaries(newPosition);
        if (isInWorldBorder && adjustPosition) {
            PlayerEntity playerEntity = session.getPlayerEntity();


            Vector3f combinedPosition = Vector3f.from(playerEntity.position().getX(), newPosition.getY(), playerEntity.position().getZ());
            playerEntity.moveAbsoluteRaw(combinedPosition, playerEntity.getYaw(), playerEntity.getPitch(), playerEntity.getHeadYaw(), playerEntity.isOnGround(), playerEntity.getVehicle() == null);
        }
        return isInWorldBorder;
    }

    public bool isPassingIntoBorderBoundaries(Vector3f newEntityPosition) {
        int entityX = GenericMath.floor(newEntityPosition.getX());
        int entityZ = GenericMath.floor(newEntityPosition.getZ());
        Vector3f currentEntityPosition = session.getPlayerEntity().position();

        return (entityX == (int) minX && currentEntityPosition.getX() > newEntityPosition.getX()) ||
            (entityX == (int) maxX && currentEntityPosition.getX() < newEntityPosition.getX()) ||
            (entityZ == (int) minZ && currentEntityPosition.getZ() > newEntityPosition.getZ()) ||
            (entityZ == (int) maxZ && currentEntityPosition.getZ() < newEntityPosition.getZ());
    }


    public bool isWithinWarningBoundaries() {
        Vector3f entityPosition = session.getPlayerEntity().position();
        return entityPosition.getX() > warningMinX && entityPosition.getX() < warningMaxX && entityPosition.getZ() > warningMinZ && entityPosition.getZ() < warningMaxZ;
    }


    public Vector3d correctMovement(BoundingBox boundingBox, Vector3d movement) {
        double correctedX;
        if (movement.getX() < 0) {
            correctedX = -limitMovement(-movement.getX(), boundingBox.getMin(Axis.X) - GenericMath.floor(minX));
        } else {
            correctedX = limitMovement(movement.getX(), GenericMath.ceil(maxX) - boundingBox.getMax(Axis.X));
        }


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

            return Double.NaN;
        }

        if (limit < COLLISION_TOLERANCE) {
            return 0;
        }

        return Math.min(movement, limit);
    }


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


    public void drawWall() {
        if (currentWallTick++ != 20) {

            return;
        }
        currentWallTick = 0;
        Vector3f entityPosition = session.getPlayerEntity().position();
        float particlePosX = entityPosition.getX();
        float particlePosY = entityPosition.getY();
        float particlePosZ = entityPosition.getZ();

        if (particlePosX > Math.min(warningMaxX, maxX - CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(maxX, particlePosY, particlePosZ), true);
        }
        if (particlePosX < Math.max(warningMinX, minX + CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(minX, particlePosY, particlePosZ), true);
        }
        if (particlePosZ > Math.min(warningMaxZ, maxZ - CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(particlePosX, particlePosY, maxZ), false);
        }
        if (particlePosZ < Math.max(warningMinZ, minZ + CLOSE_TO_BORDER)) {
            drawWall(Vector3f.from(particlePosX, particlePosY, minZ), false);
        }
    }

    private void drawWall(Vector3f position, bool drawWallX) {
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
