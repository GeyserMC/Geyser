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

package org.geysermc.geyser.registry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.registry.loader.RegistryLoader;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A versioned, mapped registry. Like {@link SimpleMappedRegistry}, the {@link Map} interface is
 * not able to be specified here, but unlike it, it does not have support for specialized
 * instances, and ONLY supports {@link Int2ObjectMap} for optimal performance to prevent boxing
 * of integers.
 *
 * @param <V> the value
 */
public class VersionedRegistry<V> extends AbstractMappedRegistry<Integer, V, Int2ObjectMap<V>> {
    protected <I> VersionedRegistry(I input, RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        super(input, registryLoader);
    }

    /**
     * Gets the closest value for the specified version. Only
     * returns versions higher up than the specified if one
     * does not exist for the given one. Useful in the event
     * that you want to get a resource which is guaranteed for
     * older versions, but not on newer ones.
     *
     * @param version the version
     * @return the closest value for the specified version
     * @throws IllegalArgumentException if no values exist at or above the given version
     */
    @NonNull
    public V forVersion(int version) {
        Int2ObjectMap.Entry<V> current = null;
        for (Int2ObjectMap.Entry<V> entry : this.mappings.int2ObjectEntrySet()) {
            int currentVersion = entry.getIntKey();
            if (version < currentVersion) {
                continue;
            }
            if (version == currentVersion) {
                return entry.getValue();
            }
            if (current == null || current.getIntKey() < currentVersion) {
                // This version is newer and should be prioritized
                current = entry;
            }
        }
        if (current == null) {
            throw new IllegalArgumentException("No appropriate value for version: " + version);
        }
        return current.getValue();
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader}. The
     * input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader
     */
    public static <I, V> VersionedRegistry<V> create(RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        return new VersionedRegistry<>(null, registryLoader);
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader} and input.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader
     */
    public static <I, V> VersionedRegistry<V> create(I input, RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        return new VersionedRegistry<>(input, registryLoader);
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader} supplier.
     * The input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, V> VersionedRegistry< V> create(Supplier<RegistryLoader<I, Int2ObjectMap<V>>> registryLoader) {
        return new VersionedRegistry<>(null, registryLoader.get());
    }

    /**
     * Creates a new versioned registry with the given {@link RegistryLoader} supplier and input.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, V> VersionedRegistry< V> create(I input, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> registryLoader) {
        return new VersionedRegistry<>(input, registryLoader.get());
    }
}