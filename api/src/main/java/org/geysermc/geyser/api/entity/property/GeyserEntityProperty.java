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
import org.geysermc.geyser.api.util.Identifier;

/**
 * Represents a property that can be attached to an entity.
 * <p>
 * Entity properties are used to describe metadata about an entity, such as
 * integers, floats, booleans, or enums.
 * @see <a href="https://learn.microsoft.com/en-us/minecraft/creator/documents/introductiontoentityproperties?view=minecraft-bedrock-stable#number-of-entity-properties-per-entity-type">
 *     Official documentation for info</a>
 *
 * @param <T> the type of value stored by this property
 *
 * @since 2.9.0
 */
public interface GeyserEntityProperty<T> {

    /**
     * Gets the unique name of this property.
     * Custom properties cannot use the vanilla namespace
     * to avoid collisions with vanilla entity properties.
     *
     * @return the property identifier
     * @since 2.9.0
     */
    @NonNull
    Identifier identifier();

    /**
     * Gets the default value of this property which
     * is set upon spawning entities.
     *
     * @return the default value of this property
     * @since 2.9.0
     */
    @NonNull
    T defaultValue();
}
