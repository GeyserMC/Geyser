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

import org.geysermc.geyser.registry.loader.RegistryLoader;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleMappedDeferredRegistry<K, V> extends AbstractMappedDeferredRegistry<K, V, Map<K, V>, SimpleMappedRegistry<K, V>> {
    protected <I> SimpleMappedDeferredRegistry(Function<RegistryLoader<I, Map<K, V>>, SimpleMappedRegistry<K, V>> registryLoader, RegistryLoader<I, Map<K, V>> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> SimpleMappedDeferredRegistry(Function<RegistryLoader<I, Map<K, V>>, SimpleMappedRegistry<K, V>> registryLoader, Supplier<RegistryLoader<I, Map<K, V>>> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> SimpleMappedDeferredRegistry(I input, RegistryInitializer<Map<K, V>, SimpleMappedRegistry<K, V>> registryInitializer, RegistryLoader<I, Map<K, V>> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
    }

    protected <I> SimpleMappedDeferredRegistry(I input, RegistryInitializer<Map<K, V>, SimpleMappedRegistry<K, V>> registryInitializer, Supplier<RegistryLoader<I, Map<K, V>>> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, K, V> SimpleMappedDeferredRegistry<K, V> create(Function<RegistryLoader<I, Map<K, V>>, SimpleMappedRegistry<K, V>> registryLoader, RegistryLoader<I, Map<K, V>> deferredLoader) {
        return new SimpleMappedDeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, K, V> SimpleMappedDeferredRegistry<K, V> create(Function<RegistryLoader<I, Map<K, V>>, SimpleMappedRegistry<K, V>> registryLoader, Supplier<RegistryLoader<I, Map<K, V>>> deferredLoader) {
        return new SimpleMappedDeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, K, V> SimpleMappedDeferredRegistry<K, V> create(I input, DeferredRegistry.RegistryInitializer<Map<K, V>, SimpleMappedRegistry<K, V>> registryInitializer, RegistryLoader<I, Map<K, V>> deferredLoader) {
        return new SimpleMappedDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, K, V> SimpleMappedDeferredRegistry<K, V> create(I input, DeferredRegistry.RegistryInitializer<Map<K, V>, SimpleMappedRegistry<K, V>> registryInitializer, Supplier<RegistryLoader<I, Map<K, V>>> deferredLoader) {
        return new SimpleMappedDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, K, V> SimpleMappedDeferredRegistry<K, V> create(I input, RegistryLoader<I, Map<K, V>> deferredLoader) {
        return create(input, SimpleMappedRegistry::create, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, K, V> SimpleMappedDeferredRegistry<K, V> create(I input, Supplier<RegistryLoader<I, Map<K, V>>> deferredLoader) {
        return create(input, SimpleMappedRegistry::create, deferredLoader);
    }
}
