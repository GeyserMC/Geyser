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

import java.util.ArrayList;
import java.util.List;

/**
 * Called on Geyser's startup when looking for custom entity properties. Player properties must be registered through this event.
 */
public abstract class GeyserDefineEntityPropertiesEvent implements Event {
    /**
     * @return The identifier of the entity currently being registered.
     */
    public abstract String entityIdentifier();

    /**
     * Registers a float entity property with the given attributes.
     * @param name The name of the property.
     * @param min The minimum value that can be assigned to the property.
     * @param max The maximum value that can be assigned to the property.
     */
    public abstract void registerFloatProperty(@NonNull String name, float min, float max);

    public void registerFloatProperty(@NonNull String name) {
        registerFloatProperty(name, Float.MIN_VALUE, Float.MAX_VALUE);
    }

    /**
     * Registers an int entity property with the given attributes.
     * @param name The name of the property.
     * @param min The minimum value that can be assigned to the property.
     * @param max The maximum value that can be assigned to the property.
     */
    public abstract void registerIntegerProperty(@NonNull String name, int min, int max);

    public void registerIntegerProperty(@NonNull String name) {
        registerFloatProperty(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Registers a boolean entity property with the given name.
     * @param name The name of the property.
     */
    public abstract void registerBooleanProperty(@NonNull String name);

    /**
     * Registers an int entity property with the given attributes.
     * @param name The name of the property.
     * @param values The string values for the enum.
     */
    public abstract void registerEnumProperty(@NonNull String name, @NonNull List<String> values);


    /**
     * Registers an int entity property with the given attributes.
     * @param name The name of the property.
     * @param values The string values for the enum.
     */
    public abstract void registerEnumProperty(@NonNull String name, @NonNull String... values);
}
