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

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.GeyserImpl;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class SimpleJavaRegistry<T> implements MutableJavaRegistry<T> {
    private final JavaRegistryKey<T> registryKey;
    // Using an AVL tree here because we care mostly about lookup performance and much less about register performance, since that only happens once
    private final Int2ObjectAVLTreeMap<RegistryEntryData<T>> byId = new Int2ObjectAVLTreeMap<>(Comparator.naturalOrder());
    // Doesn't need to be linked because Keys aren't necessarily sorted in any order and are always unique
    private final Object2ObjectOpenHashMap<Key, RegistryEntryData<T>> byKey = new Object2ObjectOpenHashMap<>();
    // Intentionally holding references here for performance and to avoid issues on duplicate values
    private final Reference2ObjectOpenHashMap<T, RegistryEntryData<T>> byValue = new Reference2ObjectOpenHashMap<>();
    private boolean frozen = false;
    private boolean allowsUnsafeAccess = false;

    public SimpleJavaRegistry(JavaRegistryKey<T> registryKey) {
        this.registryKey = registryKey;
    }

    /**
     * Allows access the entries of this registry before it is frozen.
     *
     * @deprecated should be used as little as possible, and not at all when writing new code
     */
    @Deprecated
    void allowUnsafeAccess() {
        allowsUnsafeAccess = true;
    }

    @Override
    public Optional<RegistryEntryData<T>> getById(int networkId) {
        ensureFrozen(true);
        return Optional.ofNullable(byId.get(networkId));
    }

    @Override
    public Optional<RegistryEntryData<T>> getByKey(Key key) {
        ensureFrozen(true);
        return Optional.ofNullable(byKey.get(key));
    }

    // TODO ideally this lookup wouldn't be supported on RegistryUnit registries
    @Override
    public Optional<RegistryEntryData<T>> getByValue(T object) {
        // We can never allow this access here, as this map is only populated after freezing
        ensureFrozen(false);
        return Optional.ofNullable(byValue.get(object));
    }

    @Override
    public Collection<Key> keys() {
        ensureFrozen(true);
        return byKey.keySet();
    }

    @Override
    public int size() {
        return byId.size();
    }

    @Override
    public void register(RegistryEntryData<T> entry) {
        ensureMutable();
        checkForDuplicates(entry, byId.get(entry.id()));
        checkForDuplicates(entry, byKey.get(entry.key()));

        byId.put(entry.id(), entry);
        byKey.put(entry.key(), entry);
    }

    private void checkForDuplicates(RegistryEntryData<T> entry, RegistryEntryData<T> candidate) {
        if (candidate != null) {
            throw new IllegalStateException("Duplicate entry registered to " + this + ": existing: " + candidate + ", new: " + entry);
        }
    }

    @Override
    public void clear() {
        // Don't trim maps here yet - we expect a similarly sized registry at the next population,
        // and the next freeze call will trim them if necessary
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
        try {
            byId.forEach((id, entry) -> {
                if (!entry.isBound()) {
                    entry.bind(binder.apply(id));
                }
                // Only populate byValue at this stage, and only once - this ensures that the byValue map is sorted by network ID
                byValue.putIfAbsent(entry.data(), entry);
            });
        } catch (RuntimeException exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to freeze " + this + "! This will result in issues. Report this over at our bugtracker!");
            // Clear the half-populated byValue map, just in case
            byValue.clear();
            throw exception;
        }
        frozen = true;

        // Trim maps at the end of freezing - now that the registry is frozen, we don't expect the size to change anymore
        byKey.trim();
        byValue.trim();
    }

    @Override
    public void forEachEntry(Consumer<RegistryEntryData<T>> action) {
        ensureFrozen(true);
        byId.forEach((id, entry) -> action.accept(entry));
    }

    @Override
    public Iterator<T> iterator() {
        ensureFrozen(true);
        // Using byId map here (instead of byValue.keySet) to ensure duplicate values are correctly passed multiple items
        ObjectBidirectionalIterator<Int2ObjectMap.Entry<RegistryEntryData<T>>> iterator = byId.int2ObjectEntrySet().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next().getValue().data();
            }
        };
    }

    @Override
    public String toString() {
        return "Simple " + registryKey;
    }

    private void ensureFrozen(boolean allowUnsafe) {
        if (!(allowsUnsafeAccess && allowUnsafe) && !frozen) {
            throw new IllegalStateException("Registry must be frozen to access its entries");
        }
    }

    private void ensureMutable() {
        if (frozen) {
            throw new IllegalStateException("Registry must not be frozen to register new entries");
        }
    }
}
