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
import org.geysermc.geyser.api.entity.data.types.Hitbox;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.impl.entity.HitboxImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // The vertical offset lives on the entity itself rather than as metadata, but it
        // still exposes a value (the effective offset) and an override (the API-set offset, if any).
        behaviors.put(GeyserEntityDataTypes.VERTICAL_OFFSET, new EntityDataBehavior<Float>() {
            @Override
            public void set(@NonNull Entity entity, @Nullable Float value) {
                entity.setOffsetOverride(value);
            }

            @Override
            public Float value(@NonNull Entity entity) {
                return entity.getOffset();
            }

            @Override
            public @Nullable Float override(@NonNull Entity entity) {
                return entity.getOffsetOverride();
            }
        });

        // Hitboxes are stored in the metadata manager as an NbtMap
        tracked.add(EntityDataTypes.HITBOX);
        behaviors.put(GeyserEntityDataTypes.HITBOXES, new EntityDataBehavior<List<Hitbox>>() {
            @Override
            public void set(@NonNull Entity entity, @Nullable List<Hitbox> value) {
                entity.getMetadata().updateOverride(EntityDataTypes.HITBOX, HitboxImpl.toNbtMap(value));
            }

            @Override
            public @Nullable List<Hitbox> value(@NonNull Entity entity) {
                return HitboxImpl.fromMetaData(entity.getMetadata().value(EntityDataTypes.HITBOX));
            }

            @Override
            public @Nullable List<Hitbox> override(@NonNull Entity entity) {
                return HitboxImpl.fromMetaData(entity.getMetadata().override(EntityDataTypes.HITBOX));
            }
        });

        TRACKED_ENTITY_DATA = Collections.unmodifiableSet(tracked);
        BEHAVIORS = Collections.unmodifiableMap(behaviors);
    }

    /**
     * Registers a data type whose value and override are stored in the entity's
     * {@link GeyserEntityDataManager} under the given Bedrock data type.
     */
    private static <T> void register(Map<GeyserEntityDataType<?>, EntityDataBehavior<?>> map,
                                     Set<EntityDataType<?>> tracked,
                                     GeyserEntityDataType<T> type,
                                     EntityDataType<T> bedrockType) {
        map.put(type, new EntityDataBehavior<T>() {
            @Override
            public void set(@NonNull Entity entity, @Nullable T value) {
                entity.getMetadata().updateOverride(bedrockType, value);
            }

            @Override
            public @Nullable T value(@NonNull Entity entity) {
                return entity.getMetadata().value(bedrockType);
            }

            @Override
            public @Nullable T override(@NonNull Entity entity) {
                return entity.getMetadata().override(bedrockType);
            }
        });
        tracked.add(bedrockType);
    }

    public static <T> void update(@NonNull Entity entity, @NonNull GeyserEntityDataType<T> type, @Nullable T value) {
        EntityDataBehavior<T> behavior = behavior(type);
        GeyserSession session = entity.getSession();
        if (logger.isDebug()) {
            logger.debug(session, "Custom entity API: Updating %s to %s", type.identifier(), value);
        }

        behavior.set(entity, value);
    }

    /**
     * Reads the value currently shown to the Bedrock client: the override if one is set, otherwise the base value.
     */
    public static <T> @Nullable T value(@NonNull Entity entity, @NonNull GeyserEntityDataType<T> type) {
        return behavior(type).value(entity);
    }

    /**
     * Reads only the active API override, or {@code null} if none has been set.
     */
    public static <T> @Nullable T override(@NonNull Entity entity, @NonNull GeyserEntityDataType<T> type) {
        return behavior(type).override(entity);
    }

    @SuppressWarnings("unchecked")
    private static <T> EntityDataBehavior<T> behavior(@NonNull GeyserEntityDataType<T> type) {
        EntityDataBehavior<T> behavior = (EntityDataBehavior<T>) BEHAVIORS.get(type);
        if (behavior == null) {
            throw new IllegalArgumentException("Unknown entity data type: " + type.identifier()
                + "; only types defined in GeyserEntityDataTypes are supported");
        }
        return behavior;
    }

    private EntityDataBehaviorRegistry() {}

    /**
     * Encapsulates how a single {@link GeyserEntityDataType} is stored and read for an entity
     */
    private interface EntityDataBehavior<T> {
        /**
         * Applies the API override for this data type, or clears it if {@code value} is {@code null}.
         */
        void set(@NonNull Entity entity, @Nullable T value);

        /**
         * The value currently shown to the Bedrock client: the override if set, otherwise the base value.
         */
        @Nullable T value(@NonNull Entity entity);

        /**
         * The active API override, or {@code null} if none has been set.
         */
        @Nullable T override(@NonNull Entity entity);
    }
}
