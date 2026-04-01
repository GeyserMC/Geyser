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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.registry.loader.RegistryLoader"

#include "java.util.List"
#include "java.util.function.Function"
#include "java.util.function.Supplier"

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


    public V get(int index) {
        return backingRegistry().get(index);
    }


    public V getOrDefault(int index, V defaultValue) {
        return backingRegistry().getOrDefault(index, defaultValue);
    }


    public V register(int index, V value) {
        return backingRegistry().register(index, value);
    }


    public V registerWithAnyIndex(int index, V value, V defaultValue) {
        return backingRegistry().registerWithAnyIndex(index, value, defaultValue);
    }


    public void freeze() {
        backingRegistry().freeze();
    }


    public static <I, V> ListDeferredRegistry<V> create(Function<RegistryLoader<I, List<V>>, ListRegistry<V>> registryLoader, RegistryLoader<I, List<V>> deferredLoader) {
        return new ListDeferredRegistry<>(registryLoader, deferredLoader);
    }


    public static <I, V> ListDeferredRegistry<V> create(Function<RegistryLoader<I, List<V>>, ListRegistry<V>> registryLoader, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        return new ListDeferredRegistry<>(registryLoader, deferredLoader);
    }


    public static <I, V> ListDeferredRegistry<V> create(I input, RegistryInitializer<List<V>, ListRegistry<V>> registryInitializer, RegistryLoader<I, List<V>> deferredLoader) {
        return new ListDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }


    public static <I, V> ListDeferredRegistry<V> create(I input, RegistryInitializer<List<V>, ListRegistry<V>> registryInitializer, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        return new ListDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }


    public static <I, V> ListDeferredRegistry<V> create(I input, RegistryLoader<I, List<V>> deferredLoader) {
        return create(input, ListRegistry::create, deferredLoader);
    }


    public static <I, V> ListDeferredRegistry<V> create(I input, Supplier<RegistryLoader<I, List<V>>> deferredLoader) {
        return create(input, ListRegistry::create, deferredLoader);
    }
}
