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

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.geysermc.connector.entity.Tickable;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Each session has its own EntityCache in the occasion that an entity packet is sent specifically
 * for that player (e.g. seeing vanished players from /vanish)
 */
public class EntityCache {
    private final GeyserSession session;

    @Getter
    private Long2ObjectMap<Entity> entities = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    /**
     * A list of all entities that must be ticked.
     */
    private final List<Tickable> tickableEntities = Collections.synchronizedList(new ArrayList<>());
    private Long2LongMap entityIdTranslations = Long2LongMaps.synchronize(new Long2LongOpenHashMap());
    private Map<UUID, PlayerEntity> playerEntities = Collections.synchronizedMap(new HashMap<>());
    private Map<UUID, BossBar> bossBars = Collections.synchronizedMap(new HashMap<>());
    private final Long2LongMap cachedPlayerEntityLinks = Long2LongMaps.synchronize(new Long2LongOpenHashMap());

    @Getter
    private final AtomicLong nextEntityId = new AtomicLong(2L);

    public EntityCache(GeyserSession session) {
        this.session = session;
        cachedPlayerEntityLinks.defaultReturnValue(-1L);
    }

    public void spawnEntity(Entity entity) {
        if (cacheEntity(entity)) {
            entity.spawnEntity(session);

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

    public boolean removeEntity(Entity entity, boolean force) {
        if (entity != null && entity.isValid() && (force || entity.despawnEntity(session))) {
            long geyserId = entityIdTranslations.remove(entity.getEntityId());
            entities.remove(geyserId);

            if (entity instanceof Tickable) {
                tickableEntities.remove(entity);
            }
            return true;
        }
        return false;
    }

    public void removeAllEntities() {
        List<Entity> entities = new ArrayList<>(session.getEntityCache().getEntities().values());
        for (Entity entity : entities) {
            session.getEntityCache().removeEntity(entity, false);
        }

        // As a precaution
        cachedPlayerEntityLinks.clear();
    }

    public Entity getEntityByGeyserId(long geyserId) {
        return entities.get(geyserId);
    }

    public Entity getEntityByJavaId(long javaId) {
        return entities.get(entityIdTranslations.get(javaId));
    }

    public <T extends Entity> Set<T> getEntitiesByType(Class<T> entityType) {
        Set<T> entitiesOfType = new ObjectOpenHashSet<>();
        for (Entity entity : (entityType == PlayerEntity.class ? playerEntities : entities).values()) {
            if (entity.is(entityType)) {
                entitiesOfType.add(entity.as(entityType));
            }
        }
        return entitiesOfType;
    }

    public void addPlayerEntity(PlayerEntity entity) {
        playerEntities.put(entity.getUuid(), entity);
    }

    public PlayerEntity getPlayerEntity(UUID uuid) {
        return playerEntities.get(uuid);
    }

    public PlayerEntity removePlayerEntity(UUID uuid) {
        return playerEntities.remove(uuid);
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

    public void clear() {
        entities = null;
        entityIdTranslations = null;
        playerEntities = null;
        bossBars = null;
    }

    public long getCachedPlayerEntityLink(long playerId) {
        return cachedPlayerEntityLinks.remove(playerId);
    }

    public void addCachedPlayerEntityLink(long playerId, long linkedEntityId) {
        cachedPlayerEntityLinks.put(playerId, linkedEntityId);
    }

    public List<Tickable> getTickableEntities() {
        return tickableEntities;
    }
}
