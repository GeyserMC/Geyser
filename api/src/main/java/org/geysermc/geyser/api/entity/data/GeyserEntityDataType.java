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

import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Represents a type of entity data that can be sent for an entity.
 * <p>
 * Entity data types define the values stored for a particular piece of metadata,
 * such as a {@code Byte}, {@code Integer}, {@code Float}; and the identifier associated with the types.
 * <p>
 * Unlike properties of custom items or blocks, it is possible to update entity metadata at runtime,
 * which can be done using {@link GeyserEntity#override(GeyserEntityDataType, Object)}.
 * <p>
 * Only the built-in types in {@link GeyserEntityDataTypes} are supported. Attempting to use a type
 * not provided by Geyser will throw an {@link IllegalArgumentException} at runtime.
 *
 * @param <T> the value type associated with this entity data type
 * @since 2.11.0
 */
@ApiStatus.Experimental
public abstract sealed class GeyserEntityDataType<T> permits GeyserEntityDataType.SimpleType, GeyserListEntityDataType {

    private final Identifier identifier;
    private final Class<T> typeClass;

    @ApiStatus.Internal
    GeyserEntityDataType(Identifier identifier, Class<T> typeClass) {
        this.identifier = Objects.requireNonNull(identifier);
        this.typeClass = Objects.requireNonNull(typeClass);
    }

    /**
     * Gets the identifier associated with this entity data type.
     *
     * @return the identifier for this type
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public final Identifier identifier() {
        return identifier;
    }

    /**
     * Gets the Java class representing the value type associated with this data type.
     *
     * @return the class of the value used by this entity data type
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public final Class<T> typeClass() {
        return typeClass;
    }

    @Override
    @ApiStatus.Experimental
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeyserEntityDataType<?> that)) return false;
        return identifier.equals(that.identifier);
    }

    @Override
    @ApiStatus.Experimental
    public final int hashCode() {
        return identifier.hashCode();
    }

    @Override
    @ApiStatus.Experimental
    public String toString() {
        return "GeyserEntityDataType{" + identifier + ", " + typeClass.getSimpleName() + "}";
    }

    @ApiStatus.Internal
    static <T> GeyserEntityDataType<T> create(Identifier identifier, Class<T> typeClass) {
        return new SimpleType<>(identifier, typeClass);
    }

    @ApiStatus.Internal
    static final class SimpleType<T> extends GeyserEntityDataType<T> {
        @ApiStatus.Internal
        SimpleType(Identifier identifier, Class<T> typeClass) {
            super(identifier, typeClass);
        }
    }
}
