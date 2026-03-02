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

package org.geysermc.geyser.api.entity.property;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;

/**
 * Collects property changes to be applied as a single, batched update to an entity.
 * <p>
 * Notes:
 * <ul>
 *     <li>Passing {@code null} as a value resets the property to its default.</li>
 *     <li>Numeric properties must be within declared ranges; enum properties must use an allowed value.</li>
 *     <li>Multiple updates to the same property within a single batch will result in the last value being applied.</li>
 *     <li>The updater is short-lived and should not be retained outside the batching callback.</li>
 * </ul>
 *
 * <pre>{@code
 * entity.updatePropertiesBatched(updater -> {
 *     updater.update(SOME_FLOAT_PROPERTY, 0.15f);
 *     updater.update(SOME_BOOLEAN_PROPERTY, true);
 *     updater.update(SOME_INT_PROPERTY, null); // reset to default
 * });
 * }</pre>
 *
 * @since 2.9.0
 */
@FunctionalInterface
public interface BatchPropertyUpdater {

    /**
     * Queues an update for the given property within the current batch.
     * <p>
     * If {@code value} is {@code null}, the property will be reset to its default value
     * as declared when the property was registered during the {@link GeyserDefineEntityPropertiesEvent}.
     *
     * @param property a {@link GeyserEntityProperty} registered for the target entity type
     * @param value    the new value, or {@code null} to reset to the default
     * @param <T>      the property's value type
     *
     * @since 2.9.0
     */
    <T> void update(@NonNull GeyserEntityProperty<T> property, @Nullable T value);
}
