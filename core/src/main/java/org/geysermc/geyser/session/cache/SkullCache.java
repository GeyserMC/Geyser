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
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
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

    @Getter
    private final Map<Vector3i, Skull> skulls = new Object2ObjectOpenHashMap<>();

    private final Map<Vector3i, Skull> visibleSkulls = new Object2ObjectOpenHashMap<>();

    private final Deque<SkullPlayerEntity> freeSkullEntities = new ArrayDeque<>();

    private final GeyserSession session;

    public void putSkull(Vector3i position, GameProfile profile, int blockState) {
        Skull skull = skulls.computeIfAbsent(position, pos -> new Skull());
        skull.profile = profile;
        skull.blockState = blockState;

        if (skull.entity != null) {
            skull.entity.updateSkull(profile, position, blockState);
        } else if (isVisible(position)) {
            assignSkullEntity(position, skull);
        }
    }

    public void removeSkull(Vector3i position) {
        Skull skull = skulls.remove(position);
        if (skull != null) {
            freeSkullEntity(skull);
            visibleSkulls.remove(position);
        }
    }

    public void updateVisibleSkulls() {
        // Free skull entities that are out of range
        Iterator<Map.Entry<Vector3i, Skull>> iterator = visibleSkulls.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Vector3i, Skull> entry = iterator.next();
            Vector3i position = entry.getKey();
            Skull skull = entry.getValue();

            if (!isVisible(position)) {
                freeSkullEntity(skull);
                iterator.remove();
            }
        }

        // Assign skulls entities to skulls in range
        Vector3i playerPosition = session.getPlayerEntity().getPosition().toInt();
        List<IntObjectPair<Vector3i>> inRangeSkulls = new ArrayList<>();
        for (Map.Entry<Vector3i, Skull> entry : skulls.entrySet()) {
            if (entry.getValue().entity == null) {
                Vector3i position = entry.getKey();
                int distanceSquared = position.distanceSquared(playerPosition);
                if (distanceSquared < VISIBLE_SKULL_RANGE) {
                    inRangeSkulls.add(new IntObjectImmutablePair<>(distanceSquared, position));
                }
            }
        }
        inRangeSkulls.sort(Comparator.comparingInt(IntObjectPair::firstInt));
        for (IntObjectPair<Vector3i> pair : inRangeSkulls) {
            assignSkullEntity(pair.second(), skulls.get(pair.second()));
        }
    }

    private void assignSkullEntity(Vector3i position, Skull skull) {
        if (freeSkullEntities.isEmpty()) {
            if (visibleSkulls.size() < MAX_VISIBLE_SKULLS) {
                // Create new entity
                long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();
                skull.entity = new SkullPlayerEntity(session, geyserId, skull.profile, position, skull.blockState);
                skull.entity.spawnEntity();
                visibleSkulls.put(position, skull);
            }
        } else {
            // Reuse an entity
            skull.entity = freeSkullEntities.removeFirst();
            skull.entity.updateSkull(skull.profile, position, skull.blockState);
            visibleSkulls.put(position, skull);
        }
    }

    private void freeSkullEntity(Skull skull) {
        if (skull.entity != null) {
            skull.entity.free();
            freeSkullEntities.addFirst(skull.entity);
            skull.entity = null;
        }
    }

    public Skull get(Vector3i position) {
        return skulls.get(position);
    }

    public void clear() {
        skulls.clear();
        visibleSkulls.clear();
        freeSkullEntities.clear();
    }

    private boolean isVisible(Vector3i position) {
        return position.distanceSquared(session.getPlayerEntity().getPosition().toInt()) < VISIBLE_SKULL_RANGE;
    }

    @Data
    public static class Skull {
        private GameProfile profile;
        private int blockState;
        private SkullPlayerEntity entity;
    }
}
