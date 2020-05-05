/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world;

import com.nukkitx.math.vector.Vector2f;
import com.nukkitx.math.vector.Vector3f;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geysermc.common.ChatColor;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@RequiredArgsConstructor
public class WorldBorder {
    private @NonNull Vector2f center;
    private @NonNull double oldRadius;
    private @NonNull double newRadius;
    private @NonNull long speed;
    private @NonNull int warningTime;
    private @NonNull int warningBlocks;

    // Runs the onTick method
    private ScheduledFuture<?> worldBorderTask;

    private double minX;
    private double minZ;
    private double maxX;
    private double maxZ;

    public void onTick(GeyserSession session) {
        PlayerEntity player = session.getPlayerEntity();
        if(isNearEdge(player)) {
            session.sendActionBar(ChatColor.BOLD + "" + ChatColor.RED + "You are near the world border (" + (int) getDistanceToEdge(player) + " blocks)");
        }
    }

    /**
     * Updates the min and max positions of the world border.
     * This should be called every time there is a modification to either the center coordinates or the radius.
     */
    public void update(GeyserSession session) {
        this.minX = Math.max(center.getX() - newRadius / 2.0D, -newRadius);
        this.minZ = Math.max(center.getY() - newRadius / 2.0D, -newRadius);
        this.maxX = Math.min(center.getX() + newRadius / 2.0D, newRadius);
        this.maxZ = Math.min(center.getY() + newRadius / 2.0D, newRadius);
        if (worldBorderTask != null) {
            worldBorderTask.cancel(false);
        }

        // If world border is at that number then it's 'disabled', no need to run tasks for it
        // https://minecraft.gamepedia.com/World_border#Commands
        if (!(newRadius >= 59999967)) {
            worldBorderTask = session.getConnector().getGeneralThreadPool().scheduleAtFixedRate(() ->
                    onTick(session), 1, 50, TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Checks if an entity is within the warning distance to the edge of the world border.
     * https://wiki.vg/Protocol#World_Border
     */
    public boolean isNearEdge(Entity entity) {
        double distance = Math.max(Math.min(speed * 1000 * warningTime, Math.abs(newRadius - oldRadius)), warningBlocks);

        float entityDistance = (float) getDistanceToEdge(entity);

        return (double) entityDistance < distance;
    }

    /**
     * Checks if an entity is inside the world border.
     *
     * This method needs to be improved as it doesn't account for when the world border
     * is currently changing size, it only accounts for the target size.
     *
     * Something similar to the method above should work.
     */
    public boolean isInsideBorder(Entity entity) {
        return entity.getPosition().getX() > minX && entity.getPosition().getX() < maxX && entity.getPosition().getZ() > minZ && entity.getPosition().getZ() < maxZ;
    }

    /**
     * Calculates how close the entity is to the edge of the world border.
     */
    public double getDistanceToEdge(Entity entity) {
        Vector3f pos = entity.getPosition();

        double minPosZ = pos.getZ() - minZ;
        double maxPosZ = maxZ - pos.getZ();
        double minPosX = pos.getX() - minX;
        double maxPosX = maxX - pos.getX();

        return Math.min(Math.min(Math.min(minPosX, maxPosX), minPosZ), maxPosZ);
    }
}
