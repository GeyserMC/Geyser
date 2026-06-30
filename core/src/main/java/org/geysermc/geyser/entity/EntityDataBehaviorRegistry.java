/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataType;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import org.geysermc.geyser.api.entity.data.GeyserListEntityDataType;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.impl.entity.HitboxImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class EntityDataBehaviorRegistry {

    public static final Set<EntityDataType<?>> TRACKED_ENTITY_DATA;
    private static final Map<GeyserEntityDataType<?>, EntityDataBehavior<?>> BEHAVIORS;
    private static final GeyserLogger logger = GeyserImpl.getInstance().getLogger();

    static {
        Set<EntityDataType<?>> tracked = new ObjectOpenHashSet<>();
        Map<GeyserEntityDataType<?>, EntityDataBehavior<?>> behaviors = new Object2ObjectOpenHashMap<>();

        register(behaviors, tracked, GeyserEntityDataTypes.COLOR, EntityDataTypes.COLOR);
        register(behaviors, tracked, GeyserEntityDataTypes.VARIANT, EntityDataTypes.VARIANT);
        register(behaviors, tracked, GeyserEntityDataTypes.WIDTH, EntityDataTypes.WIDTH);
        register(behaviors, tracked, GeyserEntityDataTypes.HEIGHT, EntityDataTypes.HEIGHT);
        register(behaviors, tracked, GeyserEntityDataTypes.SCALE, EntityDataTypes.SCALE);
        register(behaviors, tracked, GeyserEntityDataTypes.SEAT_OFFSET, EntityDataTypes.SEAT_OFFSET);
        register(behaviors, tracked, GeyserEntityDataTypes.ROTATION_LOCKED_TO_VEHICLE, EntityDataTypes.SEAT_LOCK_RIDER_ROTATION);
        register(behaviors, tracked, GeyserEntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES, EntityDataTypes.SEAT_LOCK_RIDER_ROTATION_DEGREES);
        register(behaviors, tracked, GeyserEntityDataTypes.SEAT_HAS_ROTATION, EntityDataTypes.SEAT_HAS_ROTATION);
        register(behaviors, tracked, GeyserEntityDataTypes.ROTATE_RIDER_DEGREES, EntityDataTypes.SEAT_ROTATION_OFFSET_DEGREES);

        register(behaviors, GeyserEntityDataTypes.VERTICAL_OFFSET,
            (entity, v) -> entity.offset(v, true),
            Entity::getOffset);

        registerList(behaviors, tracked, GeyserEntityDataTypes.HITBOXES,
            (entity, hitboxes) -> entity.getDirtyMetadata().updateOverride(EntityDataTypes.HITBOX, HitboxImpl.toNbtMap(hitboxes)),
            entity -> HitboxImpl.fromMetaData(entity.getDirtyMetadata().value(EntityDataTypes.HITBOX)),
            EntityDataTypes.HITBOX);

        TRACKED_ENTITY_DATA = Collections.unmodifiableSet(tracked);
        BEHAVIORS = Collections.unmodifiableMap(behaviors);
    }

    private static <T> void register(Map<GeyserEntityDataType<?>, EntityDataBehavior<?>> map,
                                     Set<EntityDataType<?>> tracked,
                                     GeyserEntityDataType<T> type,
                                     EntityDataType<T> bedrockType) {
        map.put(type, new EntityDataBehavior<>(
            (entity, v) -> entity.getDirtyMetadata().updateOverride(bedrockType, v),
            entity -> entity.getDirtyMetadata().value(bedrockType)
        ));
        tracked.add(bedrockType);
    }

    private static <T> void register(Map<GeyserEntityDataType<?>, EntityDataBehavior<?>> map,
                                     GeyserEntityDataType<T> type,
                                     BiConsumer<Entity, T> setter,
                                     Function<Entity, T> getter) {
        map.put(type, new EntityDataBehavior<>(setter, getter));
    }

    private static <T> void registerList(Map<GeyserEntityDataType<?>, EntityDataBehavior<?>> map,
                                         Set<EntityDataType<?>> tracked,
                                         GeyserListEntityDataType<T> type,
                                         BiConsumer<Entity, List<T>> setter,
                                         Function<Entity, List<T>> getter,
                                         EntityDataType<?> bedrockType) {
        map.put(type, new EntityDataBehavior<>(setter, getter));
        tracked.add(bedrockType);
    }

    @SuppressWarnings("unchecked")
    public static <T> void update(@NonNull Entity entity, @NonNull GeyserEntityDataType<T> type, @Nullable T value) {
        var behavior = (EntityDataBehavior<T>) BEHAVIORS.get(type);
        if (behavior == null) {
            throw new IllegalArgumentException("Unknown entity data type: " + type.identifier()
                + "; only types defined in GeyserEntityDataTypes are supported");
        }
        GeyserSession session = entity.getSession();
        if (logger.isDebug()) {
            logger.debug(session, "Custom entity API: Updating %s to %s", type.identifier(), value);
        }

        behavior.setter().accept(entity, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable T get(@NonNull Entity entity, @NonNull GeyserEntityDataType<T> type) {
        var behavior = (EntityDataBehavior<T>) BEHAVIORS.get(type);
        if (behavior == null) {
            throw new IllegalArgumentException("Unknown entity data type: " + type.identifier()
                + "; only types defined in GeyserEntityDataTypes are supported");
        }
        return behavior.getter().apply(entity);
    }

    private EntityDataBehaviorRegistry() {}

    private record EntityDataBehavior<T>(
        @NonNull BiConsumer<Entity, T> setter,
        @NonNull Function<Entity, T> getter
    ) {}
}
