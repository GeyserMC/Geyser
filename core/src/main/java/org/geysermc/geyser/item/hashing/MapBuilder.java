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

package org.geysermc.geyser.item.hashing;

import java.util.function.UnaryOperator;

/**
 * {@link MapBuilder}s can be used to define map-like structures to encode a {@link Type} using a {@link MapHasher}.
 *
 * @param <Type> the type to encode.
 */
@FunctionalInterface
public interface MapBuilder<Type> extends UnaryOperator<MapHasher<Type>> {

    /**
     * Casts this map builder to a {@link Casted}. This cast is done unsafely, only use this if you are sure the object being encoded is of the type being cast to!
     *
     * @param <Casted> the type to cast to.
     */
    default <Casted> MapBuilder<Casted> cast() {
        return builder -> builder.accept(this, casted -> (Type) casted);
    }

    /**
     * Returns a map builder that doesn't contain anything.
     *
     * @param <Type> the type to encode.
     */
    static <Type> MapBuilder<Type> empty() {
        return builder -> builder;
    }
}
