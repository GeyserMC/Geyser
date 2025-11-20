/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.property.BatchPropertyUpdater;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;

import java.util.function.Consumer;

/**
 * Represents a unique instance of an entity. Each {@link GeyserConnection}
 * have their own sets of entities - no two instances will share the same GeyserEntity instance.
 */
public interface GeyserEntity {
    /**
     * @return the entity ID that the server has assigned to this entity.
     */
    @NonNegative
    int javaId();

    /**
     * Updates an entity property with a new value.
     * If the new value is null, the property is reset to the default value.
     *
     * @param property a {@link GeyserEntityProperty} registered for this type in the {@link GeyserDefineEntityPropertiesEvent}
     * @param value the new property value
     * @param <T> the type of the value
     * @since 2.9.0
     */
    default <T> void updateProperty(@NonNull GeyserEntityProperty<T> property, @Nullable T value) {
        this.updatePropertiesBatched(consumer -> consumer.update(property, value));
    }

    /**
     * Updates multiple properties with just one update packet.
     * @see BatchPropertyUpdater
     *
     * @param consumer a batch updater
     * @since 2.9.0
     */
    default void updatePropertiesBatched(Consumer<BatchPropertyUpdater> consumer) {
        this.updatePropertiesBatched(consumer, false);
    }

    /**
     * Updates multiple properties with just one update packet, which can be sent immediately to the client.
     * Usually, sending updates immediately is not required except for specific situations where packet batching
     * would result in update order issues.
     * @see BatchPropertyUpdater
     *
     * @param consumer a batch updater
     * @param immediate whether this update should be sent immediately
     * @since 2.9.1
     */
    void updatePropertiesBatched(Consumer<BatchPropertyUpdater> consumer, boolean immediate);
}
