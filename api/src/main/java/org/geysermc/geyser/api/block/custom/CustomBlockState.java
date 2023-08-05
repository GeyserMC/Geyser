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

package org.geysermc.geyser.api.block.custom;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;

/**
 * This class is used to store a custom block state, which contains CustomBlockData 
 * tied to defined properties and values
 */
public interface CustomBlockState {
    /**
     * Gets the custom block data associated with the state
     *
     * @return The custom block data for the state.
     */
    @NonNull CustomBlockData block();

    /**
     * Gets the name of the state
     *
     * @return The name of the state.
     */
    @NonNull String name();

    /**
     * Gets the given property for the state
     *
     * @param propertyName the property name
     * @return the boolean, int, or string property.
     */
    @NonNull <T> T property(@NonNull String propertyName);

    /**
     * Gets a map of the properties for the state
     *
     * @return The properties for the state.
     */
    @NonNull Map<String, Object> properties();

    interface Builder {
        Builder booleanProperty(@NonNull String propertyName, boolean value);

        Builder intProperty(@NonNull String propertyName, int value);

        Builder stringProperty(@NonNull String propertyName, @NonNull String value);

        CustomBlockState build();
    }
}
