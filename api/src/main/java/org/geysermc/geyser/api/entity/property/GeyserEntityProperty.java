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
import org.geysermc.geyser.api.GeyserApi;

/**
 * Represents a property that can be attached to an entity.
 * <p>
 * Entity properties are used to describe metadata about an entity, such as
 * integers, floats, booleans, or enums.
 *
 * @param <T> the type of value stored by this property
 */
public interface GeyserEntityProperty<T> {

    /**
     * Gets the unique name of this property.
     *
     * @return the property's name
     */
    @NonNull String name();

    /**
     * Gets the value stored by this property.
     *
     * @return the value of this property
     */
    @NonNull T value();

    /**
     * Creates a new entity property that stores an integer value.
     *
     * @param name  the name of the property
     * @param value the integer value to store
     * @return a new integer property
     */
    static GeyserEntityProperty<Integer> intValue(@NonNull String name, int value) {
        return GeyserApi.api().provider(GeyserEntityProperty.class, name, value);
    }

    /**
     * Creates a new entity property that stores a floating-point value.
     *
     * @param name  the name of the property
     * @param value the float value to store
     * @return a new float property
     */
    static GeyserEntityProperty<Float> floatValue(@NonNull String name, float value) {
        return GeyserApi.api().provider(GeyserEntityProperty.class, name, value);
    }

    /**
     * Creates a new entity property that stores a boolean value.
     *
     * @param name  the name of the property
     * @param value the boolean value to store
     * @return a new boolean property
     */
    static GeyserEntityProperty<Boolean> booleanValue(@NonNull String name, boolean value) {
        return GeyserApi.api().provider(GeyserEntityProperty.class, name, value);
    }

    /**
     * Creates a new entity property that stores an enum value.
     * Due to Bedrock edition limitations, there cannot be more than 16 values, and
     * each value cannot exceed 32 characters. The first character must be alphabetical,
     * all following can be alphabetical, numerical, or an underscore.
     * See <a href="https://learn.microsoft.com/en-us/minecraft/creator/documents/introductiontoentityproperties?view=minecraft-bedrock-stable#limitations-of-entity-properties">here</a>
     * for more information.
     *
     * @param name  the name of the property
     * @param value the enum value to store
     * @return a new enum property
     */
    static GeyserEntityProperty<Enum<?>> enumValue(@NonNull String name, Enum<?> value) {
        return GeyserApi.api().provider(GeyserEntityProperty.class, name, value);
    }
}
