/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import java.util.function.Supplier;

/**
 * A variant of {@link AbstractMappedRegistry} with {@link Map} as the defined type. Unlike
 * {@link MappedRegistry}, this registry does not support specifying your own Map class,
 * and only permits operations the {@link Map} interface does, unless you manually cast.
 *
 * @param <K> the key
 * @param <V> the value
 */
public class SimpleMappedRegistry<K, V> extends AbstractMappedRegistry<K, V, Map<K, V>> {
    protected <I> SimpleMappedRegistry(I input, RegistryLoader<I, Map<K, V>> registryLoader) {
        super(input, registryLoader);
    }

    /**
     * Creates a new mapped registry with the given {@link RegistryLoader}. The
     * input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <K> the map key
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader
     */
    public static <I, K, V> SimpleMappedRegistry<K, V> create(RegistryLoader<I, Map<K, V>> registryLoader) {
        return new SimpleMappedRegistry<>(null, registryLoader);
    }

    /**
     * Creates a new mapped registry with the given {@link RegistryLoader} and input.
     *
     * @param input the input
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <K> the map key
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader
     */
    public static <I, K, V> SimpleMappedRegistry<K, V> create(I input, RegistryLoader<I, Map<K, V>> registryLoader) {
        return new SimpleMappedRegistry<>(input, registryLoader);
    }

    /**
     * Creates a new mapped registry with the given {@link RegistryLoader} supplier.
     * The input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader supplier
     * @param <I> the input
     * @param <K> the map key
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, K, V> SimpleMappedRegistry<K, V> create(Supplier<RegistryLoader<I, Map<K, V>>> registryLoader) {
        return new SimpleMappedRegistry<>(null, registryLoader.get());
    }

    /**
     * Creates a new mapped registry with the given {@link RegistryLoader} and input.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <K> the map key
     * @param <V> the map value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, K, V> SimpleMappedRegistry<K, V> create(I input, Supplier<RegistryLoader<I, Map<K, V>>> registryLoader) {
        return new SimpleMappedRegistry<>(input, registryLoader.get());
    }
}