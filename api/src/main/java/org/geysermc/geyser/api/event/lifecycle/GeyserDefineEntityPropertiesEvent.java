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
import org.geysermc.event.Event;

/**
 * Called on Geyser's startup when looking for user-defined entity properties.
 * This event can be used to e.g. register player properties.
 */
public interface GeyserDefineEntityPropertiesEvent extends Event {

    /**
     * @return the identifier of the entity currently being registered
     */
    String entityIdentifier();

    /**
     * Registers a float entity property with the given attributes.
     *
     * @param name the name of the property
     * @param min the minimum value that can be assigned to the property
     * @param max the maximum value that can be assigned to the property
     * @param defaultValue the default value of the property
     */
    void registerFloatProperty(@NonNull String name, float min, float max, float defaultValue);

    /**
     * Registers an int entity property with the given attributes.
     *
     * @param name the name of the property
     * @param min the minimum value that can be assigned to the property
     * @param max the maximum value that can be assigned to the property
     * @param defaultValue the default value of the property
     */
    void registerIntegerProperty(@NonNull String name, int min, int max, int defaultValue);

    /**
     * Registers a boolean entity property with the given name.
     *
     * @param name the name of the property
     * @param defaultValue the default value of the property
     */
    void registerBooleanProperty(@NonNull String name, boolean defaultValue);

    /**
     * Registers a boolean entity property with the given name
     * and the default value set to "false"
     *
     * @param name the name of the property
     */
    default void registerBooleanProperty(@NonNull String name) {
        registerBooleanProperty(name, false);
    }

    /**
     * Registers an int entity property with the given attributes.
     *
     * @param name the name of the property
     * @param enumClass the enum class
     * @param defaultValue the default enum value of the property
     */
    <E extends Enum<E>> void registerEnumProperty(@NonNull String name, @NonNull Class<E> enumClass, @NonNull E defaultValue);
}
