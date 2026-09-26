/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.java;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class SimpleJavaRegistry<T> implements MutableJavaRegistry<T> {
    private final Int2ObjectSortedMap<RegistryEntryData<T>> byId = new Int2ObjectLinkedOpenHashMap<>();
    private final Map<Key, RegistryEntryData<T>> byKey = new Object2ObjectOpenHashMap<>();
    private final Map<T, RegistryEntryData<T>> byValue = new Object2ObjectOpenHashMap<>();
    private boolean frozen = false;

    @Override
    public Optional<RegistryEntryData<T>> getById(int networkId) {
        assertFrozen();
        return Optional.ofNullable(byId.get(networkId));
    }

    @Override
    public Optional<RegistryEntryData<T>> getByKey(Key key) {
        assertFrozen();
        return Optional.ofNullable(byKey.get(key));
    }

    @Override
    public Optional<RegistryEntryData<T>> getByValue(T object) {
        assertFrozen();
        return Optional.ofNullable(byValue.get(object));
    }

    @Override
    public Collection<Key> keys() {
        assertFrozen();
        return byKey.keySet();
    }

    @Override
    public Collection<T> values() {
        assertFrozen();
        return byValue.keySet();
    }

    @Override
    public int size() {
        return byId.size();
    }

    @Override
    public void forEachEntry(Consumer<RegistryEntryData<T>> action) {
        byId.forEach((id, entry) -> action.accept(entry));
    }

    @Override
    public void register(RegistryEntryData<T> entry) {
        assertNotFrozen();
        byId.put(entry.id(), entry);
        byKey.put(entry.key(), entry);
        if (entry.isBound()) {
            byValue.put(entry.data(), entry);
        }
    }

    @Override
    public void clear() {
        byId.clear();
        byKey.clear();
        byValue.clear();
        frozen = false;
    }

    @Override
    public void freeze(IntFunction<T> binder) {
        if (frozen) {
            return;
        }
        frozen = true;
        byId.forEach((id, entry) -> {
            if (!entry.isBound()) {
                entry.bind(binder.apply(id));
            }
        });
    }

    @Override
    public Iterator<T> iterator() {
        IntIterator idIterator = byId.keySet().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return idIterator.hasNext();
            }

            @Override
            public T next() {
                return byId.get(idIterator.nextInt()).data();
            }
        };
    }

    private void assertFrozen() {
        assert frozen : "Registry must be frozen";
    }

    private void assertNotFrozen() {
        assert !frozen : "Registry must not be frozen to register new entries";
    }
}
