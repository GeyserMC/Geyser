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

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Each session has its own EntityCache in the occasion that an entity packet is sent specifically
 * for that player (e.g. seeing vanished players from /vanish)
 */
public class EntityCache {
    private final GeyserSession session;

    @Getter
    private final Long2ObjectMap<Entity> entities = new Long2ObjectOpenHashMap<>();
    /**
     * A list of all entities that must be ticked.
     */
    private final List<Tickable> tickableEntities = new ObjectArrayList<>();
    private final Int2LongMap entityIdTranslations = new Int2LongOpenHashMap();
    private final Map<UUID, PlayerEntity> playerEntities = new Object2ObjectOpenHashMap<>();
    private final Map<UUID, BossBar> bossBars = new Object2ObjectOpenHashMap<>();

    @Getter
    private final AtomicLong nextEntityId = new AtomicLong(2L);

    public EntityCache(GeyserSession session) {
        this.session = session;
    }

    public void spawnEntity(Entity entity) {
        if (cacheEntity(entity)) {
            entity.spawnEntity();

            if (entity instanceof Tickable) {
                // Start ticking it
                tickableEntities.add((Tickable) entity);
            }
        }
    }

    public boolean cacheEntity(Entity entity) {
        // Check to see if the entity exists, otherwise we can end up with duplicated mobs
        if (!entityIdTranslations.containsKey(entity.getEntityId())) {
            entityIdTranslations.put(entity.getEntityId(), entity.getGeyserId());
            entities.put(entity.getGeyserId(), entity);
            return true;
        }
        return false;
    }

    public void removeEntity(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            session.getPlayerWithCustomHeads().remove(player.getUuid());
        }

        if (entity != null) {
            if (entity.isValid()) {
                entity.despawnEntity();
            }

            long geyserId = entityIdTranslations.remove(entity.getEntityId());
            entities.remove(geyserId);

            if (entity instanceof Tickable) {
                tickableEntities.remove(entity);
            }
        }
    }

    public void removeAllEntities() {
        List<Entity> entities = new ArrayList<>(this.entities.values());
        for (Entity entity : entities) {
            removeEntity(entity);
        }

        session.getPlayerWithCustomHeads().clear();
    }

    public Entity getEntityByGeyserId(long geyserId) {
        return entities.get(geyserId);
    }

    public Entity getEntityByJavaId(int javaId) {
        if (javaId == session.getPlayerEntity().getEntityId()) {
            return session.getPlayerEntity();
        }
        return entities.get(entityIdTranslations.get(javaId));
    }

    public void addPlayerEntity(PlayerEntity entity) {
        // putIfAbsent matches the behavior of playerInfoMap in Java as of 1.19.3
        playerEntities.putIfAbsent(entity.getUuid(), entity);
    }

    public PlayerEntity getPlayerEntity(UUID uuid) {
        return playerEntities.get(uuid);
    }

    public PlayerEntity removePlayerEntity(UUID uuid) {
        return playerEntities.remove(uuid);
    }

    public Collection<PlayerEntity> getAllPlayerEntities() {
        return playerEntities.values();
    }

    public void addBossBar(UUID uuid, BossBar bossBar) {
        bossBars.put(uuid, bossBar);
        bossBar.addBossBar();
    }

    public BossBar getBossBar(UUID uuid) {
        return bossBars.get(uuid);
    }

    public void removeBossBar(UUID uuid) {
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeBossBar();
        }
    }

    public void updateBossBars() {
        bossBars.values().forEach(BossBar::updateBossBar);
    }

    public List<Tickable> getTickableEntities() {
        return tickableEntities;
    }

    public void removeAllBossBars() {
        bossBars.values().forEach(BossBar::removeBossBar);
        bossBars.clear();
    }
}
