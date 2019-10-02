/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

import lombok.Getter;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Each session has its own EntityCache in the occasion that an entity packet is sent specifically
 * for that player (e.g. seeing vanished players from /vanish)
 */
public class EntityCache {
    private GeyserSession session;

    @Getter
    private Map<Long, Entity> entities = new HashMap<>();
    private Map<Long, Long> entityIdTranslations = new HashMap<>();
    private Map<UUID, PlayerEntity> playerEntities = new HashMap<>();
    private Map<UUID, Long> bossbars = new HashMap<>();

    @Getter
    private AtomicLong nextEntityId = new AtomicLong(2L);

    public EntityCache(GeyserSession session) {
        this.session = session;
    }

    public void spawnEntity(Entity entity) {
        entity.moveAbsolute(entity.getPosition(), entity.getRotation().getX(), entity.getRotation().getY());
        entityIdTranslations.put(entity.getEntityId(), entity.getGeyserId());
        entities.put(entity.getGeyserId(), entity);
        entity.spawnEntity(session);
    }

    public void removeEntity(Entity entity) {
        if (entity == null || !entity.isValid()) return;

        Long geyserId = entityIdTranslations.remove(entity.getEntityId());
        if (geyserId != null) {
            entities.remove(geyserId);
            if (entity.is(PlayerEntity.class)) {
                playerEntities.remove(entity.as(PlayerEntity.class).getUuid());
            }
        }
        entity.despawnEntity(session);
    }

    public Entity getEntityByGeyserId(long geyserId) {
        return entities.get(geyserId);
    }

    public Entity getEntityByJavaId(long javaId) {
        return entities.get(entityIdTranslations.get(javaId));
    }

    public void addPlayerEntity(PlayerEntity entity) {
        playerEntities.put(entity.getUuid(), entity);
    }

    public PlayerEntity getPlayerEntity(UUID uuid) {
        return playerEntities.get(uuid);
    }

    public void removePlayerEntity(UUID uuid) {
        playerEntities.remove(uuid);
    }

    public long addBossBar(UUID uuid) {
        long entityId = getNextEntityId().incrementAndGet();
        bossbars.put(uuid, entityId);
        return entityId;
    }

    public long getBossBar(UUID uuid) {
        return bossbars.containsKey(uuid) ? bossbars.get(uuid) : -1;
    }

    public long removeBossBar(UUID uuid) {
        return bossbars.remove(uuid);
    }
}
