/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.geysermc.geyser.entity.EntitySpectateHelper;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.waypoint.GeyserWaypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

/**
 * Each session has its own EntityCache in the occasion that an entity packet is sent specifically
 * for that player (e.g. seeing vanished players from /vanish)
 */
public class EntityCache {
    private final GeyserSession session;

    /**
     * Guards the {@link #entities}, {@link #entityIdTranslations} and {@link #entityUuidTranslations} maps.
     * <p>
     * All mutations to these maps happen on the session's event loop thread. However, the Geyser entity API exposes
     * {@link #getEntityByGeyserId(long)}, {@link #getEntityByJavaId(int)} and {@link #getEntityByUuid(UUID)},
     * which can be called from arbitrary threads. The backing fastutil open-hash maps are not thread safe :/
     * <p>
     * A {@link ReentrantReadWriteLock} (rather than a {@link StampedLock}) is used because {@link #removeAllEntities()}
     * re-enters the write lock via {@link #removeEntity(Entity)}.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Long2ObjectMap<Entity> entities = new Long2ObjectOpenHashMap<>();
    /**
     * A list of all entities that must be ticked.
     */
    @Getter
    private final List<Tickable> tickableEntities = new ObjectArrayList<>();
    private final Int2LongMap entityIdTranslations = new Int2LongOpenHashMap();
    private final Object2LongMap<UUID> entityUuidTranslations = new Object2LongOpenHashMap<>();
    private final Map<UUID, PlayerEntity> playerEntities = new Object2ObjectOpenHashMap<>();
    private final Map<UUID, BossBar> bossBars = new Object2ObjectOpenHashMap<>();
    @Getter
    private final Set<Entity> dirtyEntities = new ObjectOpenHashSet<>();

    @Getter
    private final AtomicLong nextEntityId = new AtomicLong(2L);

    public EntityCache(GeyserSession session) {
        this.session = session;
    }

    public long nextEntityId() {
        return nextEntityId.incrementAndGet();
    }

    /**
     * Returns the raw, mutable backing entity map. This bypasses the read/write lock that guards the map, so it
     * <b>must only be accessed on the session's event loop thread</b>, where it is sequential with all mutations. You should never
     * mutate it directly, use {@link #spawnEntity(Entity)} or {@link #removeEntity(Entity)} and related methods instead.
     * Off-thread callers - mainly API users - must instead use {@link #getEntityByGeyserId(long)}, {@link #getEntityByJavaId(int)} or
     * {@link #getEntityByUuid(UUID)}.
     */
    public Long2ObjectMap<Entity> getEntitiesUnsafe() {
        return entities;
    }

    public void spawnEntity(Entity entity) {
        if (cacheEntity(entity)) {
            // start tracking newly spawned entities. Doing this before the actual entity spawn can result in combining
            // the otherwise sent metadata packet (in the case of team visibility, which sets the NAME metadata to
            // empty) with the entity spawn packet (which also includes metadata). Resulting in 1 less packet sent.
            session.getWorldCache().getScoreboard().entityRegistered(entity);

            entity.spawnEntity();

            if (entity instanceof Tickable) {
                // Start ticking it
                tickableEntities.add((Tickable) entity);
            }
            if (GeyserWaypoint.uses26_10WaypointPacket(session)) {
                session.getWaypointCache().addEntity(entity);
            }
        }
    }

    public boolean cacheEntity(Entity entity) {
        lock.writeLock().lock();
        try {
            // Check to see if the entity exists, otherwise we can end up with duplicated mobs
            if (!entityIdTranslations.containsKey(entity.getEntityId())) {
                entityIdTranslations.put(entity.getEntityId(), entity.geyserId());
                entities.put(entity.geyserId(), entity);
                if (entity.uuid() != null) {
                    entityUuidTranslations.put(entity.uuid(), entity.geyserId());
                }
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeEntity(Entity entity) {
        if (entity == null) {
            return;
        }

        if (entity == session.getSpectatedEntity()) {
            EntitySpectateHelper.stop(session);
        }

        if (entity instanceof PlayerEntity player) {
            session.getPlayerWithCustomHeads().remove(player.uuid());
        }

        if (entity.isValid()) {
            entity.despawnEntity();
        }

        // Keep the three map removals atomic relative to off-thread readers, but avoid holding the
        // write lock across despawnEntity()/scoreboard callbacks above and below.
        lock.writeLock().lock();
        try {
            entities.remove(entityIdTranslations.remove(entity.getEntityId()));
            if (entity.uuid() != null) {
                entityUuidTranslations.removeLong(entity.uuid());
            }
        } finally {
            lock.writeLock().unlock();
        }

        // don't track the entity anymore, now that it's removed
        session.getWorldCache().getScoreboard().entityRemoved(entity);

        if (entity instanceof Tickable) {
            tickableEntities.remove(entity);
        }

        if (GeyserWaypoint.uses26_10WaypointPacket(session)) {
            session.getWaypointCache().removeEntity(entity);
        }

        dirtyEntities.remove(entity);
    }

    public void markDirty(Entity entity) {
        dirtyEntities.add(entity);
    }

    public void removeAllEntities() {
        session.getWorldBorder().clearCollision();

        // removeEntity() calls below re-enter the write lock, which the ReentrantReadWriteLock permits.
        lock.writeLock().lock();
        try {
            List<Entity> entities = new ArrayList<>(this.entities.values());
            for (Entity entity : entities) {
                removeEntity(entity);
            }
        } finally {
            lock.writeLock().unlock();
        }

        session.getPlayerWithCustomHeads().clear();
    }

    /**
     * Safe to call from any thread. When called off the session's event loop thread, the lookup is guarded by
     * the read lock; on the event loop it reads directly since it is sequential with all writes there.
     */
    public Entity getEntityByGeyserId(long geyserId) {
        if (session.getTickEventLoop().inEventLoop()) {
            return getEntityByGeyserId0(geyserId);
        }
        lock.readLock().lock();
        try {
            return getEntityByGeyserId0(geyserId);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Entity getEntityByGeyserId0(long geyserId) {
        return entities.get(geyserId);
    }

    /**
     * Safe to call from any thread. When called off the session's event loop thread, the lookup is guarded by
     * the read lock; on the event loop it reads directly since it is sequential with all writes there.
     */
    public Entity getEntityByJavaId(int javaId) {
        if (javaId == session.getPlayerEntity().getEntityId()) {
            return session.getPlayerEntity();
        }
        if (session.getTickEventLoop().inEventLoop()) {
            return getEntityByJavaId0(javaId);
        }
        lock.readLock().lock();
        try {
            return getEntityByJavaId0(javaId);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Entity getEntityByJavaId0(int javaId) {
        // Both map reads must happen under the same read-lock acquisition, so the pair is atomic relative to writers.
        return entities.get(entityIdTranslations.get(javaId));
    }

    /**
     * Safe to call from any thread. When called off the session's event loop thread, the lookup is guarded by
     * the read lock; on the event loop it reads directly since it is sequential with all writes there.
     */
    public Entity getEntityByUuid(UUID uuid) {
        if (Objects.equals(uuid, session.getPlayerEntity().uuid())) {
            return session.getPlayerEntity();
        }
        if (session.getTickEventLoop().inEventLoop()) {
            return getEntityByUuid0(uuid);
        }
        lock.readLock().lock();
        try {
            return getEntityByUuid0(uuid);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Entity getEntityByUuid0(UUID uuid) {
        // Both map reads must happen under the same read-lock acquisition
        return entities.get(entityUuidTranslations.getLong(uuid));
    }

    public void addPlayerEntity(PlayerEntity entity) {
        synchronized (playerEntities) {
            // putIfAbsent matches the behavior of playerInfoMap in Java as of 1.19.3
            boolean exists = playerEntities.putIfAbsent(entity.uuid(), entity) != null;
            if (exists) {
                return;
            }
        }

        // notify scoreboard for new entity
        var scoreboard = session.getWorldCache().getScoreboard();
        scoreboard.playerRegistered(entity);
    }

    public PlayerEntity getPlayerEntity(UUID uuid) {
        synchronized (playerEntities) {
            return playerEntities.get(uuid);
        }
    }

    public List<PlayerEntity> getPlayersByName(String name) {
        var list = new ArrayList<PlayerEntity>();
        synchronized (playerEntities) {
            for (PlayerEntity player : playerEntities.values()) {
                if (name.equals(player.getUsername())) {
                    list.add(player);
                }
            }
        }
        return list;
    }

    public PlayerEntity removePlayerEntity(UUID uuid) {
        PlayerEntity player;
        synchronized (playerEntities) {
            player = playerEntities.remove(uuid);
        }

        if (player != null) {
            // notify scoreboard
            session.getWorldCache().getScoreboard().playerRemoved(player);
        }
        return player;
    }

    /**
     * Run a specific bit of code for each cached player entity.
     * As usual with synchronized, try to minimize the amount of work you because you block the PlayerList collection.
     */
    public void forEachPlayerEntity(Consumer<PlayerEntity> player) {
        synchronized (playerEntities) {
            playerEntities.values().forEach(player);
        }
    }

    public void removeAllPlayerEntities() {
        synchronized (playerEntities) {
            playerEntities.clear();
        }
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

    public void removeAllBossBars() {
        bossBars.values().forEach(BossBar::removeBossBar);
        bossBars.clear();
    }
}
