/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.entity.type;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataType;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.property.BatchPropertyUpdater;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a unique entity instance as seen by a specific {@link GeyserConnection}.
 * Each connection maintains its own set of entity objects; no two connections share the same
 * {@code GeyserEntity} instance.
 */
public interface GeyserEntity {

    /**
     * @return the entity ID that the server has assigned to this entity, or -1 if none is present
     */
    @NonNegative
    int javaId();

    /**
     * The entity id used by Geyser to identify this entity with the Bedrock client.
     *
     * @return the Geyser entity id that the Bedrock client sees
     * @since 2.11.0
     */
    @Positive
    @ApiStatus.Experimental
    long geyserId();

    /**
     * The entity's UUID, only null if the server has not spawned this entity.
     *
     * @return the entity UUID that the server has assigned to this entity,
     * or null if this entity isn't known to the Java server
     * @since 2.11.0
     */
    @Nullable
    @ApiStatus.Experimental
    UUID uuid();

    /**
     * The Bedrock entity definition for this entity, which can also be a {@link CustomEntityDefinition}.
     *
     * @return the Bedrock entity definition
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    GeyserEntityDefinition definition();

    /**
     * The position of this entity as reported by the Java server, without any Bedrock vertical
     * offset applied.
     *
     * @return the position of the entity as known to the Java server
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Vector3f position();

    /**
     * The vehicle this entity is currently mounted on.
     *
     * @return the vehicle of this entity, or {@code null} if this entity is not a passenger
     * @since 2.11.0
     */
    @Nullable
    @ApiStatus.Experimental
    GeyserEntity vehicle();

    /**
     * The passengers currently riding this entity.
     *
     * @return an immutable snapshot of this entity's passengers, or an empty list if none
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    List<GeyserEntity> passengers();

    /**
     * Reads the current value of a given {@link GeyserEntityDataType} which
     * would currently be sent to players - either the {@link #override(GeyserEntityDataType)}, or the
     * base value, if one is sent by default.
     *
     * @see GeyserEntityDataTypes
     * @param dataType the entity data type to query
     * @param <T> the type of the value
     * @return the current value, or {@code null} if none is known
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    <T> @Nullable T value(GeyserEntityDataType<T> dataType);

    /**
     * Reads the current override of a given {@link GeyserEntityDataType}, or
     * null if no override was defined.
     *
     * @see GeyserEntityDataTypes
     * @param dataType the entity data type override to query
     * @param <T> the type of the value
     * @return the current override value, or {@code null} if no override has been set
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    <T> @Nullable T override(GeyserEntityDataType<T> dataType);

    /**
     * Overrides an entity data value for this entity.
     * If {@code value} is {@code null}, the override is cleared and the default value
     * (from the Java server or entity type definition) is restored.
     *
     * @param dataType an entity data type, such as a constant from {@link GeyserEntityDataTypes}
     * @param value the new value, or {@code null} to reset the override
     * @param <T> the type of the value
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    <T> void override(GeyserEntityDataType<T> dataType, @Nullable T value);

    /**
     * Convenience method to update a single entity property.
     * Equivalent to {@link #updatePropertiesBatched(Consumer, boolean) updatePropertiesBatched}
     * with a single-property consumer and {@code immediate = false}.
     *
     * @param property a {@link GeyserEntityProperty} registered for this type in the {@link GeyserDefineEntityPropertiesEvent}
     * @param value the new property value, or {@code null} to reset
     * @param <T> the type of the value
     * @since 2.9.0
     */
    default <T> void updateProperty(GeyserEntityProperty<T> property, @Nullable T value) {
        this.updatePropertiesBatched(consumer -> consumer.update(property, value));
    }

    /**
     * @deprecated use {@link #updateProperty(GeyserEntityProperty, Object)} instead
     * @since 2.9.0
     */
    @Deprecated(since = "2.11.0")
    default void updatePropertiesBatched(Consumer<BatchPropertyUpdater> consumer) {
        this.updatePropertiesBatched(consumer, false);
    }

    /**
     * Updates multiple entity properties with a single update, optionally sending it immediately.
     * <p>
     * Usually, sending immediately is unnecessary; batched updates are flushed once per tick.
     * Pass {@code immediate = true} only when packet ordering requires the properties to be
     * applied before the next tick (e.g., to avoid a visual flicker on the client).
     *
     * @param consumer a {@link BatchPropertyUpdater} callback that applies one or more property changes
     * @param immediate {@code true} to send the packet immediately rather than at the next tick
     * @see BatchPropertyUpdater
     * @since 2.9.1
     */
    void updatePropertiesBatched(Consumer<BatchPropertyUpdater> consumer, boolean immediate);
}
