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

package org.geysermc.connector.network.session.cache;

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerFogPacket;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.Collections;

public class WorldBorder {

    @Getter @Setter
    private @NonNull Vector2f center;
    @Getter @Setter private double oldDiameter;
    @Getter @Setter private double newDiameter;
    @Getter @Setter private long speed;
    @Getter @Setter private int warningDelay;
    @Getter @Setter private int warningBlocks;

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
     * If the world border is resizing, this variable saves how many ticks have progressed in the resizing
     */
    private long lastUpdatedWorldBorderTime = 0;

    private final GeyserSession session;

    /**
     * Default constructor.
     *
     * @param center Vector2f of the current world border.
     * @param oldDiameter double for the diameter in blocks of the world border before it got changed or similar to newDiameter if not changed.
     * @param newDiameter double for the diameter in blocks of the new world border.
     * @param speed long for the speed to apply an expansion/shrinking of the world border. When a client joins they get the actual border oldDiameter and the time left to reach the newDiameter.
     * @param warningDelay int for the time in seconds before a shrinking world border would hit a not moving player. Creates the same visual warning effect as warningBlocks.
     * @param warningBlocks int in blocks before you reach the border to show warning particles.
     */
    public WorldBorder(GeyserSession session, @NonNull Vector2f center, double oldDiameter, double newDiameter,
                       long speed, int warningDelay, int warningBlocks) {
        this.session = session;

        this.center = center;
        this.oldDiameter = oldDiameter;
        this.newDiameter = newDiameter;
        this.speed = speed;
        this.warningDelay = warningDelay;
        this.warningBlocks = warningBlocks;

        this.currentDiameter = newDiameter;
        this.resizing = speed != 0L;
    }

    /**
     * @return true as long the entity is within the world limits.
     */
    public boolean isInsideBorderBoundaries() {
        Vector3f entityPosition = session.getPlayerEntity().getPosition();
        return entityPosition.getX() > minX && entityPosition.getX() < maxX && entityPosition.getZ() > minZ && entityPosition.getZ() < maxZ;
    }

    public boolean isPassingIntoBorderBoundaries(Vector3f entityPosition) {
        Vector3i flattenedEntityPosition = entityPosition.toInt();
        Vector3f currentEntityPosition = session.getPlayerEntity().getPosition();
        // Make sure we can't move out of the world border, but if we're out of the world border, we can move in
        return (flattenedEntityPosition.getX() == (int) minX && currentEntityPosition.getX() > entityPosition.getX()) ||
                (flattenedEntityPosition.getX() == (int) maxX && currentEntityPosition.getX() < entityPosition.getX()) ||
                (flattenedEntityPosition.getZ() == (int) minZ && currentEntityPosition.getZ() > entityPosition.getZ()) ||
                (flattenedEntityPosition.getZ() == (int) maxZ && currentEntityPosition.getZ() < entityPosition.getZ());
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
        this.minX = center.getX() - radius;
        this.minZ = center.getY() - radius; // Mapping 2D vector to 3D coordinates >> Y becomes Z
        this.maxX = center.getX() + radius;
        this.maxZ = center.getY() + radius; // Mapping 2D vector to 3D coordinates >> Y becomes Z

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

    private static final LevelEventType WORLD_BORDER_PARTICLE = LevelEventType.PARTICLE_DENY_BLOCK;

    /**
     * Draws a wall of particles where the world border resides
     */
    public void drawWall() {
        Vector3f entityPosition = session.getPlayerEntity().getPosition();
        float particlePosX = entityPosition.getX();
        float particlePosY = entityPosition.getY();
        float particlePosZ = entityPosition.getZ();

        if (entityPosition.getX() > warningMaxX) {
            drawWall(Vector3f.from(maxX, particlePosY, particlePosZ), true);
        }
        if (entityPosition.getX() < warningMinX) {
            drawWall(Vector3f.from(minX, particlePosY, particlePosZ), true);
        }
        if (entityPosition.getZ() > warningMaxZ) {
            drawWall(Vector3f.from(particlePosX, particlePosY, maxZ), false);
        }
        if (entityPosition.getZ() < warningMinZ) {
            drawWall(Vector3f.from(particlePosX, particlePosY, minZ), false);
        }
    }

    private void drawWall(Vector3f position, boolean drawWallX) {
        float initialY = position.getY() - EntityType.PLAYER.getOffset() - 1;
        for (float y = initialY; y < (initialY + 5); y++) {
            if (drawWallX) {
                for (float z = position.getZ() - 3; z < (position.getZ() + 3); z++) {
                    if (z < minZ) {
                        continue;
                    }
                    if (z > maxZ) {
                        break;
                    }

                    LevelEventPacket effectPacket = new LevelEventPacket();
                    effectPacket.setPosition(Vector3f.from(position.getX(), y, z));
                    effectPacket.setType(WORLD_BORDER_PARTICLE);
                    session.getUpstream().sendPacket(effectPacket);
                }
            } else {
                for (float x = position.getX() - 3; x < (position.getX() + 3); x++) {
                    if (x < minX) {
                        continue;
                    }
                    if (x > maxX) {
                        break;
                    }

                    LevelEventPacket effectPacket = new LevelEventPacket();
                    effectPacket.setPosition(Vector3f.from(x, y, position.getZ()));
                    effectPacket.setType(WORLD_BORDER_PARTICLE);
                    session.getUpstream().sendPacket(effectPacket);
                }
            }
        }
    }

    /**
     * Send the following fog IDs to the client
     */
    public static void sendFog(GeyserSession session, String... fogNameSpaces) {
        PlayerFogPacket packet = new PlayerFogPacket();
        Collections.addAll(packet.getFogStack(), fogNameSpaces);
        session.sendUpstreamPacket(packet);
    }

    /**
     * Clear any additional fog sent to the client
     */
    public static void removeFog(GeyserSession session) {
        session.sendUpstreamPacket(new PlayerFogPacket());
    }

}
