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
package org.geysermc.geyser.api.event.lifecycle

import org.geysermc.event.Event
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty
import org.geysermc.geyser.api.entity.property.type.*
import org.geysermc.geyser.api.entity.type.GeyserEntity
import org.geysermc.geyser.api.util.Identifier

/**
 * Lifecycle event fired during Geyser's startup to allow custom entity properties
 * to be registered for a specific entity type.
 * 
 * 
 * Listeners can add new properties for any entity by passing the target entity's
 * identifier (e.g., `Identifier.of("player")`) to the registration methods.
 * The returned [GeyserEntityProperty] is used to identify the properties and to
 * update the value of a specific entity instance.
 * 
 * <h2>Example usage</h2>
 * <pre>`public void onDefine(GeyserDefineEntityPropertiesEvent event) {     Identifier player = Identifier.of("player");     GeyserFloatEntityProperty ANIMATION_SPEED =         event.registerFloatProperty(player, Identifier.of("my_group:animation_speed"), 0.0f, 1.0f, 0.1f);     GeyserBooleanEntityProperty SHOW_SHORTS =         event.registerBooleanProperty(player, Identifier.of("my_group:show_shorts"), false); } `</pre>
 * 
 * Retrieving entity instances is possible with the [EntityData.entityByJavaId] method, or
 * [EntityData.playerEntity] for the connection player entity.
 * To update the value of a property on a specific entity, use [GeyserEntity.updateProperty],
 * or [GeyserEntity.updatePropertiesBatched] to update multiple properties efficiently at once.
 * 
 * 
 * **Notes:**
 * 
 *  * Default values must fall within the provided bounds.
 *  * There cannot be more than 32 properties registered per entity type in total
 *  * [.properties] returns properties registered for the given entity
 * (including those added earlier in the same callback), including vanilla properties.
 * 
 * 
 * @since 2.9.0
 */
interface GeyserDefineEntityPropertiesEvent : Event {
    /**
     * Returns an *unmodifiable* view of all properties that have been registered
     * so far for the given entity type. This includes entity properties used for vanilla gameplay,
     * such as those used for creaking animations.
     * 
     * @param entityType the Java edition entity type identifier
     * @return an unmodifiable collection of registered properties
     * 
     * @since 2.9.0
     */
    fun properties(entityType: Identifier): MutableCollection<GeyserEntityProperty<*>?>?

    /**
     * Registers a `float`-backed entity property.
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
    fun registerFloatProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        min: Float,
        max: Float,
        defaultValue: Float?
    ): GeyserFloatEntityProperty?

    /**
     * Registers a `float`-backed entity property with a default value set to the minimum value.
     * @see .registerFloatProperty
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return the created float property
     * 
     * @since 2.9.0
     */
    fun registerFloatProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        min: Float,
        max: Float
    ): GeyserFloatEntityProperty? {
        return registerFloatProperty(entityType, propertyIdentifier, min, max, null)
    }

    /**
     * Registers an `int`-backed entity property.
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
    fun registerIntegerProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        min: Int,
        max: Int,
        defaultValue: Int?
    ): GeyserIntEntityProperty?

    /**
     * Registers an `int`-backed entity property with a default value set to the minimum value.
     * 
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return the created int property
     * 
     * @since 2.9.0
     */
    fun registerIntegerProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        min: Int,
        max: Int
    ): GeyserIntEntityProperty? {
        return registerIntegerProperty(entityType, propertyIdentifier, min, max, null)
    }

    /**
     * Registers a `boolean`-backed entity property.
     * 
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param defaultValue the default boolean value
     * @return the created boolean property handle
     * 
     * @since 2.9.0
     */
    fun registerBooleanProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        defaultValue: Boolean
    ): GeyserBooleanEntityProperty?

    /**
     * Registers a `boolean`-backed entity property with a default of `false`.
     * @see .registerBooleanProperty
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @return the created boolean property
     * @since 2.9.0
     */
    fun registerBooleanProperty(entityType: Identifier, propertyIdentifier: Identifier): GeyserBooleanEntityProperty? {
        return registerBooleanProperty(entityType, propertyIdentifier, false)
    }

    /**
     * Registers a typed [enum][Enum]-backed entity property.
     * 
     * 
     * The enum constants define the allowed values. If `defaultValue` is `null`,
     * the first enum value is set as the default.
     * @see GeyserEnumEntityProperty for further limitations
     * 
     * 
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param enumClass the enum class that defines allowed values
     * @param defaultValue the default enum value, or `null` for the first enum value to be the default
     * @param <E> the enum type
     * @return the created enum property
     * 
     * @since 2.9.0
    </E> */
    fun <E : Enum<E?>?> registerEnumProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        enumClass: Class<E?>,
        defaultValue: E?
    ): GeyserEnumEntityProperty<E?>?

    /**
     * Registers a typed [enum][Enum]-backed entity property with the first value set as the default.
     * @see .registerEnumProperty
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param enumClass the enum class that defines allowed values
     * @param <E> the enum type
     * @return the created enum property
     * 
     * @since 2.9.0
    </E> */
    fun <E : Enum<E?>?> registerEnumProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        enumClass: Class<E?>
    ): GeyserEnumEntityProperty<E?>? {
        return registerEnumProperty<E?>(entityType, propertyIdentifier, enumClass, null)
    }

    /**
     * Registers a string-backed "enum-like" entity property where the set of allowed values
     * is defined by the provided list. If `defaultValue` is `null`, the first value is used as the default
     * on entity spawn. The default must be one of the values in `values`.
     * @see GeyserStringEnumProperty
     * 
     * 
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param values the allowed string values
     * @param defaultValue the default string value, or `null` for the first value to be used
     * @return the created string-enum property
     * 
     * @since 2.9.0
     */
    fun registerEnumProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        values: MutableList<String?>,
        defaultValue: String?
    ): GeyserStringEnumProperty?

    /**
     * Registers a string-backed "enum-like" entity property with the first value as the default.
     * @see .registerEnumProperty
     * @param entityType the Java edition entity type identifier
     * @param propertyIdentifier the unique property identifier
     * @param values the allowed string values
     * @return the created string-enum property handle
     * 
     * @since 2.9.0
     */
    fun registerEnumProperty(
        entityType: Identifier,
        propertyIdentifier: Identifier,
        values: MutableList<String?>
    ): GeyserStringEnumProperty? {
        return registerEnumProperty(entityType, propertyIdentifier, values, null)
    }
}
