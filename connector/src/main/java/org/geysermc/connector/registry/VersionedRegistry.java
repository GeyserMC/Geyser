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

package org.geysermc.connector.registry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.geysermc.connector.registry.loader.RegistryLoader;

import java.util.function.Supplier;

public class VersionedRegistry<V> extends AbstractMappedRegistry<Integer, V, Int2ObjectMap<V>> {
    protected <I> VersionedRegistry(I input, RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        super(input, registryLoader);
    }

    public V forVersion(int version) {
        V value = null;
        for (Int2ObjectMap.Entry<V> entry : this.mappings.int2ObjectEntrySet()) {
            if (version < entry.getIntKey()) {
                continue;
            }
            if (version == entry.getIntKey()) {
                return entry.getValue();
            }
            value = entry.getValue();
        }
        return value;
    }

    public static <I, V> VersionedRegistry<V> create(RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        return new VersionedRegistry<>(null, registryLoader);
    }

    public static <I, V> VersionedRegistry<V> create(I input, RegistryLoader<I, Int2ObjectMap<V>> registryLoader) {
        return new VersionedRegistry<>(input, registryLoader);
    }

    public static <I, V> VersionedRegistry< V> create(Supplier<RegistryLoader<I, Int2ObjectMap<V>>> registryLoader) {
        return new VersionedRegistry<>(null, registryLoader.get());
    }

    public static <I, V> VersionedRegistry< V> create(I input, Supplier<RegistryLoader<I, Int2ObjectMap<V>>> registryLoader) {
        return new VersionedRegistry<>(input, registryLoader.get());
    }
}