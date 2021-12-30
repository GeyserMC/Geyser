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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.math.vector.Vector3i;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.*;

@RequiredArgsConstructor
public class SkullCache {
    private static final int MAX_VISIBLE_SKULLS = 128;
    private static final float VISIBLE_SKULL_RANGE = 32 * 32;
    private static final long CLEANUP_PERIOD = 10000;

    @Getter
    private final Map<Vector3i, Skull> skulls = new Object2ObjectOpenHashMap<>();

    private final Map<Vector3i, Skull> visibleSkulls = new Object2ObjectOpenHashMap<>();

    private final Deque<SkullPlayerEntity> unusedSkullEntities = new ArrayDeque<>();

    private final GeyserSession session;

    private Vector3i lastPlayerPosition;

    private long lastCleanup = System.currentTimeMillis();

    public void putSkull(Vector3i position, GameProfile profile, int blockState) {
        Skull skull = skulls.computeIfAbsent(position, Skull::new);
        skull.profile = profile;
        skull.blockState = blockState;
        skull.distance = position.distanceSquared(session.getPlayerEntity().getPosition().toInt());

        if (skull.entity != null) {
            skull.entity.updateSkull(skull);
        } else if (skull.distance < VISIBLE_SKULL_RANGE) {
            updateVisibleSkulls(true);
        }
    }

    public void removeSkull(Vector3i position) {
        Skull skull = skulls.remove(position);
        if (skull != null) {
            freeSkullEntity(skull);
            updateVisibleSkulls(true);
        }
    }

    public void updateVisibleSkulls(boolean force) {
        Vector3i playerPosition = session.getPlayerEntity().getPosition().toInt();
        if (!force) {
            // No need to recheck skull visibility for small movements
            if (lastPlayerPosition != null && playerPosition.distanceSquared(lastPlayerPosition) < 4) {
                return;
            }
        }
        lastPlayerPosition = playerPosition;

        List<Skull> inRangeSkulls = new ArrayList<>();
        for (Map.Entry<Vector3i, Skull> entry : skulls.entrySet()) {
            Skull skull = entry.getValue();
            skull.distance = entry.getKey().distanceSquared(playerPosition);
            if (skull.distance > VISIBLE_SKULL_RANGE) {
                freeSkullEntity(skull);
            } else {
                inRangeSkulls.add(skull);
            }
        }
        inRangeSkulls.sort(Comparator.comparingInt(Skull::getDistance));

        while (inRangeSkulls.size() > MAX_VISIBLE_SKULLS) {
            Skull skull = inRangeSkulls.remove(inRangeSkulls.size() - 1);
            freeSkullEntity(skull);
        }

        for (Skull skull : inRangeSkulls) {
            assignSkullEntity(skull);
        }

        // Keep unused skull entities around for later use, to reduce "player" pop-in
        if ((System.currentTimeMillis() - lastCleanup) > CLEANUP_PERIOD) {
            lastCleanup = System.currentTimeMillis();
            for (SkullPlayerEntity entity : unusedSkullEntities) {
                entity.despawnEntity();
            }
            unusedSkullEntities.clear();
        }
    }

    private void assignSkullEntity(Skull skull) {
        if (skull.entity != null) {
            return;
        }
        if (unusedSkullEntities.isEmpty()) {
            if (visibleSkulls.size() < MAX_VISIBLE_SKULLS) {
                // Create new entity
                long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();
                skull.entity = new SkullPlayerEntity(session, geyserId);
                skull.entity.spawnEntity();
                skull.entity.updateSkull(skull);
                visibleSkulls.put(skull.position, skull);
            }
        } else {
            // Reuse an entity
            skull.entity = unusedSkullEntities.removeFirst();
            skull.entity.updateSkull(skull);
            visibleSkulls.put(skull.position, skull);
        }
    }

    private void freeSkullEntity(Skull skull) {
        if (skull.entity != null) {
            visibleSkulls.remove(skull.position);
            skull.entity.free();
            unusedSkullEntities.addFirst(skull.entity);
            skull.entity = null;
        }
    }

    public Skull get(Vector3i position) {
        return skulls.get(position);
    }

    public void clear() {
        skulls.clear();
        visibleSkulls.clear();
        unusedSkullEntities.clear();
        lastPlayerPosition = null;
    }

    @RequiredArgsConstructor
    @Data
    public static class Skull {
        private GameProfile profile;
        private int blockState;
        private SkullPlayerEntity entity;

        private final Vector3i position;
        private int distance;
    }
}
