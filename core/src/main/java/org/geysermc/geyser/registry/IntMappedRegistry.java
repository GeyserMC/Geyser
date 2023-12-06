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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.registry.loader.RegistryLoader;

import java.util.function.Supplier;

/**
 * A mapped registry with an integer as the key. This class is designed to minimize the need for boxing/unboxing keys.
 *
 * @param <V> the value
 */
public class IntMappedRegistry<V> extends AbstractMappedRegistry<Integer, V, Int2ObjectMap<V>> {
    protected <I> IntMappedRegistry(I input, RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        super(input, registryLoader);
    }

    /**
     * Returns the value registered by the given integer.
     *
     * @param i the integer
     * @return the value registered by the given integer.
     */
    public V get(int i) {
        return this.mappings.get(i);
    }

    @Nullable
    @Override
    @Deprecated
    public V get(Integer key) {
        return super.get(key);
    }

    /**
     * Returns the value registered by the given key or the default value
     * specified if null.
     *
     * @param i the key
     * @param defaultValue the default value
     * @return the value registered by the given key or the default value
     *         specified if null.
     */
    public V getOrDefault(int i, V defaultValue) {
        return this.mappings.getOrDefault(i, defaultValue);
    }

    @Override
    @Deprecated
    public V getOrDefault(Integer key, V defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    /**
     * Registers a new value into this registry with the given key.
     *
     * @param i the key
     * @param value the value
     * @return a new value into this registry with the given key.
     */
    public V register(int i, V value) {
        return this.mappings.put(i, value);
    }

    @Override
    @Deprecated
    public V register(Integer key, V value) {
        return super.register(key, value);
    }

    /**
     * Creates a new integer mapped registry with the given {@link RegistryLoader}. The
     * input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader
     */
    public static <I, V> IntMappedRegistry<V> create(RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        return new IntMappedRegistry<>(null, registryLoader);
    }

    /**
     * Creates a new integer mapped registry with the given {@link RegistryLoader} and input.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, V> IntMappedRegistry<V> create(I input, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> registryLoader) {
        return new IntMappedRegistry<>(input, registryLoader.get());
    }
}
