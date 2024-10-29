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

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractMappedDeferredRegistry<K, V, M extends Map<K, V>, R extends AbstractMappedRegistry<K, V, M>> extends DeferredRegistry<M, R> {
    protected <I> AbstractMappedDeferredRegistry(Function<RegistryLoader<I, M>, R> registryLoader, RegistryLoader<I, M> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> AbstractMappedDeferredRegistry(Function<RegistryLoader<I, M>, R> registryLoader, Supplier<RegistryLoader<I, M>> deferredLoader) {
        super(registryLoader, deferredLoader);
    }

    protected <I> AbstractMappedDeferredRegistry(I input, RegistryInitializer<M, R> registryInitializer, RegistryLoader<I, M> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
    }

    protected <I> AbstractMappedDeferredRegistry(I input, RegistryInitializer<M, R> registryInitializer, Supplier<RegistryLoader<I, M>> deferredLoader) {
        super(input, registryInitializer, deferredLoader);
    }

    /**
     * Returns the value registered by the given key.
     *
     * @param key the key
     * @return the value registered by the given key.
     */
    public @Nullable V get(K key) {
        return get().get(key);
    }

    /**
     * Returns and maps the value by the given key if present.
     *
     * @param key the key
     * @param mapper the mapper
     * @param <U> the type
     * @return the mapped value from the given key if present
     */
    public <U> Optional<U> map(K key, Function<? super V, ? extends U> mapper) {
        V value = this.get(key);
        if (value == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(mapper.apply(value));
        }
    }

    /**
     * Returns the value registered by the given key or the default value
     * specified if null.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value registered by the given key or the default value
     *         specified if null.
     */
    public V getOrDefault(K key, V defaultValue) {
        return get().getOrDefault(key, defaultValue);
    }

    /**
     * Registers a new value into this registry with the given key.
     *
     * @param key the key
     * @param value the value
     * @return a new value into this registry with the given key.
     */
    public V register(K key, V value) {
        return get().put(key, value);
    }
}
