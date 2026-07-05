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

package org.geysermc.geyser.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wrapper for temporarily storing entity metadata that will be sent to Bedrock.
 */
public final class GeyserEntityDataManager {

    /**
     * Map storing all current values of tracked metadata that's made available in the API.
     */
    private final Map<EntityDataType<?>, Object> metadata = new ConcurrentHashMap<>();

    /**
     * Map storing the metadata updates until they're sent to Bedrock, then cleared.
     */
    private final Map<EntityDataType<?>, Object> dirtyMetadata = new Object2ObjectLinkedOpenHashMap<>();

    /**
     * Map storing currently overridden metadata via the {@link GeyserEntityDataType} API; readable from any thread.
     */
    private final Map<EntityDataType<?>, Object> overrides = new ConcurrentHashMap<>();

    public <T> void put(EntityDataType<T> entityData, T value) {
        if (EntityDataBehaviorRegistry.TRACKED_ENTITY_DATA.contains(entityData)) {
            // Track the original value
            metadata.put(entityData, value);
            // But use the override if it exists
            Object override = overrides.get(entityData);
            if (override != null) {
                //noinspection unchecked
                value = (T) override;
            }
        }
        dirtyMetadata.put(entityData, value);
    }

    public <T> void updateOverride(@NonNull EntityDataType<T> entityData, @Nullable T value) {
        if (!EntityDataBehaviorRegistry.TRACKED_ENTITY_DATA.contains(entityData)) {
            throw new IllegalArgumentException("Entity data type not tracked: " + entityData.getClass().getSimpleName());
        }

        if (value == null) {
            overrides.remove(entityData);
            Object currentValue = metadata.get(entityData);
            if (currentValue != null) {
                dirtyMetadata.put(entityData, currentValue);
            }
        } else {
            overrides.put(entityData, value);
            dirtyMetadata.put(entityData, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T override(EntityDataType<T> entityData) {
        if (!EntityDataBehaviorRegistry.TRACKED_ENTITY_DATA.contains(entityData)) {
            throw new IllegalArgumentException("Entity data type not tracked: " + entityData.getClass().getSimpleName());
        }
        return (T) overrides.get(entityData);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T value(EntityDataType<T> entityData) {
        if (!EntityDataBehaviorRegistry.TRACKED_ENTITY_DATA.contains(entityData)) {
            throw new IllegalArgumentException("Entity data type not tracked: " + entityData.getClass().getSimpleName());
        }
        Object override = overrides.get(entityData);
        return override != null ? (T) override : (T) metadata.get(entityData);
    }

    /**
     * Applies the contents of the dirty metadata into the input and clears the contents of our map.
     */
    public void apply(EntityDataMap map) {
        map.putAll(dirtyMetadata);
        dirtyMetadata.clear();
    }

    public boolean hasEntries() {
        return !dirtyMetadata.isEmpty();
    }

    /**
     * Intended for testing purposes only
     */
    public <T> T get(EntityDataType<T> entityData) {
        //noinspection unchecked
        return (T) dirtyMetadata.get(entityData);
    }

    @Override
    public String toString() {
        return dirtyMetadata.toString();
    }
}
