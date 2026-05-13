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

package org.geysermc.geyser.api.entity.data;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.entity.type.GeyserEntity;

/**
 * Represents a type of entity data that can be sent for an entity.
 * <p>
 * Entity data types define the kind of value stored for a particular piece of metadata,
 * such as a {@code Byte}, {@code Integer}, {@code Float}; and the name associated with them.
 * <p>
 * Unlike custom items or blocks, it is possible to update entity metadata at runtime,
 * which can be done using {@link GeyserEntity#update(GeyserEntityDataType, Object)}.
 *
 * @param <T> the value type associated with this entity data type
 */
public interface GeyserEntityDataType<T> {

    /**
     * Gets the Java class representing the value type associated with this data type.
     *
     * @return the class of the value used by this entity data type
     */
    Class<T> typeClass();

    /**
     * Gets the unique name of this data type.
     * <p>
     * The name is used internally to identify and register the data type so it can be
     * referenced when reading or writing entity metadata.
     *
     * @return the name of this entity data type
     */
    String name();

    /**
     * For API usage only; use the types defined in {@link GeyserEntityDataTypes}
     */
    static <T> GeyserEntityDataType<T> of(Class<T> typeClass, String name) {
        return GeyserApi.api().provider(GeyserEntityDataType.class, typeClass, name);
    }
}
