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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.registry.loader.RegistryLoader;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ListDeferredRegistry<V> extends DeferredRegistry<List<V>, ListRegistry<V>> {
    protected <I> ListDeferredRegistry(Function<RegistryLoader<I, List<V>>, ListRegistry<V>> registryLoader, RegistryLoader<I, List<V>> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> ListDeferredRegistry(Function<RegistryLoader<I, List<V>>, ListRegistry<V>> registryLoader, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> ListDeferredRegistry(I input, RegistryInitializer<List<V>, ListRegistry<V>> registryInitializer, RegistryLoader<I, List<V>> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
    }

    protected <I> ListDeferredRegistry(I input, RegistryInitializer<List<V>, ListRegistry<V>> registryInitializer, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
    }
    /**
     * Returns the value registered by the given index.
     *
     * @param index the index
     * @return the value registered by the given index.
     */
    @Nullable
    public V get(int index) {
        return backingRegistry().get(index);
    }

    /**
     * Returns the value registered by the given index or the default value
     * specified if null.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value registered by the given key or the default value
     *         specified if null.
     */
    public V getOrDefault(int index, V defaultValue) {
        return backingRegistry().getOrDefault(index, defaultValue);
    }

    /**
     * Registers a new value into this registry with the given index.
     *
     * @param index the index
     * @param value the value
     * @return a new value into this registry with the given index.
     */
    public V register(int index, V value) {
        return backingRegistry().register(index, value);
    }

    /**
     * Registers a new value into this registry with the given index, even if this value would normally be outside
     * the range of a list.
     *
     * @param index the index
     * @param value the value
     * @param defaultValue the default value to fill empty spaces in the registry with.
     * @return a new value into this registry with the given index.
     */
    public V registerWithAnyIndex(int index, V value, V defaultValue) {
        return backingRegistry().registerWithAnyIndex(index, value, defaultValue);
    }

    /**
     * Mark this registry as unsuitable for new additions. The backing list will then be optimized for storage.
     */
    public void freeze() {
        backingRegistry().freeze();
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> ListDeferredRegistry<V> create(Function<RegistryLoader<I, List<V>>, ListRegistry<V>> registryLoader, RegistryLoader<I, List<V>> deferredLoader) {
        return new ListDeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> ListDeferredRegistry<V> create(Function<RegistryLoader<I, List<V>>, ListRegistry<V>> registryLoader, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        return new ListDeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> ListDeferredRegistry<V> create(I input, RegistryInitializer<List<V>, ListRegistry<V>> registryInitializer, RegistryLoader<I, List<V>> deferredLoader) {
        return new ListDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> ListDeferredRegistry<V> create(I input, RegistryInitializer<List<V>, ListRegistry<V>> registryInitializer, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        return new ListDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> ListDeferredRegistry<V> create(I input, RegistryLoader<I, List<V>> deferredLoader) {
        return create(input, ListRegistry::create, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @return the new deferred registry
     */
    public static <I, V> ListDeferredRegistry<V> create(I input, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        return create(input, ListRegistry::create, deferredLoader);
    }
}
