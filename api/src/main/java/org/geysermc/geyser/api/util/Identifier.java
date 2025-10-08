/*
 * Copyright (c) 2024-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;

/**
 * An identifying object for representing unique objects.
 * This identifier consists of two parts:
 * <ul>
 *     <li>
 *         a namespace, which is usually a name identifying your work
 *     </li>
 *     <li>
 *         a path, which holds a value.
 *     </li>
 * </ul>
 *
 * Examples of identifiers:
 * <ul>
 *     <li>{@code minecraft:fox}</li>
 *     <li>{@code geysermc:one_fun_example}</li>
 * </ul>
 *
 * If this identifier is referencing anything not in the
 * vanilla Minecraft game, the namespace cannot be "minecraft".
 * Further, paths cannot contain colons ({@code :}).
 *
 * @since 2.9.0
 */
public interface Identifier {

    /**
     * The namespace for Minecraft.
     * @since 2.9.0
     */
    String DEFAULT_NAMESPACE = "minecraft";

    /**
     * Attempts to create a new identifier from a namespace and path.
     * 
     * @return the identifier for this namespace and path
     * @throws IllegalArgumentException if either namespace or path are invalid.
     * @since 2.9.0
     */
    static Identifier of(@NonNull String namespace, @NonNull String path) {
        return GeyserApi.api().provider(Identifier.class, namespace, path);
    }

    /**
     * Attempts to create a new identifier from a string representation.
     *
     * @return the identifier for this namespace and path
     * @throws IllegalArgumentException if either the namespace or path are invalid
     * @since 2.9.0
     */
    static Identifier of(String identifier) {
        String[] split = identifier.split(":");
        String namespace;
        String path;
        if (split.length == 1) {
            namespace = DEFAULT_NAMESPACE;
            path = split[0];
        } else if (split.length == 2) {
            namespace = split[0];
            path = split[1];
        } else {
            throw new IllegalArgumentException("':' in identifier path: " + identifier);
        }
        return of(namespace, path);
    }

    /**
     * @return the namespace of this identifier.
     * @since 2.9.0
     */
    String namespace();

    /**
     * @return the path of this identifier.
     * @since 2.9.0
     */
    String path();

    /**
     * Checks whether this identifier is using the "minecraft" namespace.
     * @since 2.9.0
     */
    default boolean vanilla() {
        return namespace().equals(DEFAULT_NAMESPACE);
    }
}
