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

#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.registry.loader.RegistryLoader"

#include "java.util.function.Function"
#include "java.util.function.Supplier"

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



    public V forVersion(int version) {
        return backingRegistry().forVersion(version);
    }

    public static <I, V> VersionedDeferredRegistry<V> create(Function<RegistryLoader<I, Int2ObjectMap<V>>, VersionedRegistry<V>> registryLoader, RegistryLoader<I, Int2ObjectMap<V>> deferredLoader) {
        return new VersionedDeferredRegistry<>(registryLoader, deferredLoader);
    }


    public static <I, V> VersionedDeferredRegistry<V> create(Function<RegistryLoader<I, Int2ObjectMap<V>>, VersionedRegistry<V>> registryLoader, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> deferredLoader) {
        return new VersionedDeferredRegistry<>(registryLoader, deferredLoader);
    }


    public static <I, V> VersionedDeferredRegistry<V> create(I input, RegistryInitializer<Int2ObjectMap<V>, VersionedRegistry<V>> registryInitializer, RegistryLoader<I, Int2ObjectMap<V>> deferredLoader) {
        return new VersionedDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }


    public static <I, V> VersionedDeferredRegistry<V> create(I input, RegistryInitializer<Int2ObjectMap<V>, VersionedRegistry<V>> registryInitializer, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> deferredLoader) {
        return new VersionedDeferredRegistry<>(input, registryInitializer, deferredLoader);
    }
}
