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

package org.geysermc.geyser.api.event.lifecycle;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.entity.EntityData;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserBooleanEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserEnumEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserStringEnumProperty;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lifecycle event fired during Geyser's startup to allow custom entity properties
 * to be registered for a specific entity type.
 * <p>
 * Listeners can add new properties for any entity by passing the target entity's
 * identifier (e.g., {@code Identifier.of("player")}) to the registration methods.
 * The returned {@link GeyserEntityProperty} is used to identify the properties and to
 * update the value of a specific entity instance.
 *
 * <h2>Example usage</h2>
 * <pre>{@code
 * public void onDefine(GeyserDefineEntityPropertiesEvent event) {
 *     Identifier player = Identifier.of("player");
 *     GeyserFloatEntityProperty ANIMATION_SPEED =
 *         event.registerFloatProperty(player, Identifier.of("my_group:animation_speed"), 0.0f, 1.0f, 0.1f);
 *     GeyserBooleanEntityProperty SHOW_SHORTS =
 *         event.registerBooleanProperty(player, Identifier.of("my_group:show_shorts"), false);
 * }
 * }</pre>
 *
 * Retrieving entity instances is possible with the {@link EntityData#entityByJavaId(int)} method, or
 * {@link EntityData#playerEntity()} for the connection player entity.
 * To update the value of a property on a specific entity, use {@link GeyserEntity#updateProperty(GeyserEntityProperty, Object)},
 * or {@link GeyserEntity#updatePropertiesBatched(Consumer)} to update multiple properties efficiently at once.
 *
 * <p><b>Notes:</b>
 * <ul>
 *   <li>Default values must fall within the provided bounds.</li>
 *   <li>There cannot be more than 32 properties registered per entity type in total</li>
 *   <li>{@link #properties(Identifier)} returns properties registered for the given entity
 *       (including those added earlier in the same callback), including vanilla properties.</li>
 * </ul>
 * 
 * @since 2.9.0
 */
public interface GeyserDefineEntityPropertiesEvent extends Event {

    /**
     * Returns an <em>unmodifiable</em> view of all properties that have been registered
     * so far for the given entity type. This includes entity properties used for vanilla gameplay,
     * such as those used for creaking animations.
     *
     * @param entityType the Java edition entity type identifier
     * @return an unmodifiable collection of registered properties
     * 
     * @since 2.9.0
     */
    Collection<GeyserEntityProperty<?>> properties(@NonNull Identifier entityType);

    /**
     * Registers a {@code float}-backed entity property.
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param defaultValue the default value assigned initially on entity spawn - if null, it will be the minimum value
     * @return the created float property
     * 
     * @since 2.9.0
     */
    GeyserFloatEntityProperty registerFloatProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, float min, float max, @Nullable Float defaultValue);

    /**
     * Registers a {@code float}-backed entity property with a default value set to the minimum value.
     * @see #registerFloatProperty(Identifier, Identifier, float, float, Float)
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return the created float property
     * 
     * @since 2.9.0
     */
    default GeyserFloatEntityProperty registerFloatProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, float min, float max) {
        return registerFloatProperty(entityType, propertyIdentifier, min, max, null);
    }

    /**
     * Registers an {@code int}-backed entity property.
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param defaultValue the default value assigned initially on entity spawn - if null, it will be the minimum value
     * @return the created int property
     * 
     * @since 2.9.0
     */
    GeyserIntEntityProperty registerIntegerProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, int min, int max, @Nullable Integer defaultValue);

    /**
     * Registers an {@code int}-backed entity property with a default value set to the minimum value.
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return the created int property
     * 
     * @since 2.9.0
     */
    default GeyserIntEntityProperty registerIntegerProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, int min, int max) {
        return registerIntegerProperty(entityType, propertyIdentifier, min, max, null);
    }

    /**
     * Registers a {@code boolean}-backed entity property.
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param defaultValue the default boolean value
     * @return the created boolean property handle
     * 
     * @since 2.9.0
     */
    GeyserBooleanEntityProperty registerBooleanProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, boolean defaultValue);

    /**
     * Registers a {@code boolean}-backed entity property with a default of {@code false}.
     * @see #registerBooleanProperty(Identifier, Identifier, boolean)
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @return the created boolean property
     * @since 2.9.0
     */
    default GeyserBooleanEntityProperty registerBooleanProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier) {
        return registerBooleanProperty(entityType, propertyIdentifier, false);
    }

    /**
     * Registers a typed {@linkplain Enum enum}-backed entity property.
     * <p>
     * The enum constants define the allowed values. If {@code defaultValue} is {@code null},
     * the first enum value is set as the default.
     * @see GeyserEnumEntityProperty for further limitations
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param enumClass the enum class that defines allowed values
     * @param defaultValue the default enum value, or {@code null} for the first enum value to be the default
     * @param <E> the enum type
     * @return the created enum property
     * 
     * @since 2.9.0
     */
    <E extends Enum<E>> GeyserEnumEntityProperty<E> registerEnumProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, @NonNull Class<E> enumClass, @Nullable E defaultValue);

    /**
     * Registers a typed {@linkplain Enum enum}-backed entity property with the first value set as the default.
     * @see #registerEnumProperty(Identifier, Identifier, Class, Enum)
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param enumClass the enum class that defines allowed values
     * @param <E> the enum type
     * @return the created enum property
     * 
     * @since 2.9.0
     */
    default <E extends Enum<E>> GeyserEnumEntityProperty<E> registerEnumProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, @NonNull Class<E> enumClass) {
        return registerEnumProperty(entityType, propertyIdentifier, enumClass, null);
    }

    /**
     * Registers a string-backed "enum-like" entity property where the set of allowed values
     * is defined by the provided list. If {@code defaultValue} is {@code null}, the first value is used as the default
     * on entity spawn. The default must be one of the values in {@code values}.
     * @see GeyserStringEnumProperty
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param values the allowed string values
     * @param defaultValue the default string value, or {@code null} for the first value to be used
     * @return the created string-enum property
     * 
     * @since 2.9.0
     */
    GeyserStringEnumProperty registerEnumProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, @NonNull List<String> values, @Nullable String defaultValue);

    /**
     * Registers a string-backed "enum-like" entity property with the first value as the default.
     * @see #registerEnumProperty(Identifier, Identifier, List, String)
     *
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param values the allowed string values
     * @return the created string-enum property handle
     * 
     * @since 2.9.0
     */
    default GeyserStringEnumProperty registerEnumProperty(@NonNull Identifier entityType, @NonNull Identifier propertyIdentifier, @NonNull List<String> values) {
        return registerEnumProperty(entityType, propertyIdentifier, values, null);
    }
}
