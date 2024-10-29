/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import java.util.function.Function;
import java.util.function.Supplier;

public class VersionedDeferredRegistry<V> extends AbstractMappedDeferredRegistry<Integer, V, Int2ObjectMap<V>, VersionedRegistry<V>> {
    protected <I> VersionedDeferredRegistry(Function<RegistryLoader<I, Int2ObjectMap<V>>, VersionedRegistry<V>> registryLoader, RegistryLoader<I, Int2ObjectMap<V>> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> VersionedDeferredRegistry(Function<RegistryLoader<I, Int2ObjectMap<V>>, VersionedRegistry<V>> registryLoader, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> VersionedDeferredRegistry(I input, RegistryInitializer<Int2ObjectMap<V>, VersionedRegistry<V>> registryInitializer, RegistryLoader<I, Int2ObjectMap<V>> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
    }

    protected <I> VersionedDeferredRegistry(I input, RegistryInitializer<Int2ObjectMap<V>, VersionedRegistry<V>> registryInitializer, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
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
        return backingRegistry().forVersion(version);
    }
    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> VersionedDeferredRegistry<V> create(Function<RegistryLoader<I, Int2ObjectMap<V>>, VersionedRegistry<V>> registryLoader, RegistryLoader<I, Int2ObjectMap<V>> deferredLoader) {
        return new VersionedDeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> VersionedDeferredRegistry<V> create(Function<RegistryLoader<I, Int2ObjectMap<V>>, VersionedRegistry<V>> registryLoader, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> deferredLoader) {
        return new VersionedDeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> VersionedDeferredRegistry<V> create(I input, RegistryInitializer<Int2ObjectMap<V>, VersionedRegistry<V>> registryInitializer, RegistryLoader<I, Int2ObjectMap<V>> deferredLoader) {
        return new VersionedDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> VersionedDeferredRegistry<V> create(I input, RegistryInitializer<Int2ObjectMap<V>, VersionedRegistry<V>> registryInitializer, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> deferredLoader) {
        return new VersionedDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }
}
