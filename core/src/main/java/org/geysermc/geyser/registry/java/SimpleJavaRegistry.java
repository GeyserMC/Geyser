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
    private boolean bound = false;
    private boolean hasUnboundEntries = false;
    private boolean allowsUnsafeAccess = false;

    public SimpleJavaRegistry(JavaRegistryKey<T> registryKey) {
        this.registryKey = registryKey;
    }

    /**
     * Allows accessing the entries of this registry before it is frozen.
     *
     * @deprecated should be used as little as possible, and not at all when writing new code
     */
    @Deprecated
    void allowUnsafeAccess() {
        allowsUnsafeAccess = true;
    }

    // Overriding these 2 with an additional ensureBound call, which will result in a more descriptive error message when registry is not bound
    @Override
    public Optional<T> get(int networkId) {
        ensureBound(true);
        return MutableJavaRegistry.super.get(networkId);
    }

    @Override
    public Optional<T> get(Key key) {
        ensureBound(true);
        return MutableJavaRegistry.super.get(key);
    }

    @Override
    public Optional<RegistryEntryData<T>> getById(int networkId) {
        ensureFrozen();
        return Optional.ofNullable(byId.get(networkId));
    }

    @Override
    public Optional<RegistryEntryData<T>> getByKey(Key key) {
        ensureFrozen();
        return Optional.ofNullable(byKey.get(key));
    }

    // TODO ideally this lookup wouldn't be supported on RegistryUnit registries
    @Override
    public Optional<RegistryEntryData<T>> getByValue(T object) {
        // byValue is only populated after registry is bound, so we absolutely cannot allow unsafe access here
        ensureBound(false);
        return Optional.ofNullable(byValue.get(object));
    }

    @Override
    public Collection<Key> keys() {
        ensureFrozen();
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
        if (!entry.isBound()) {
            hasUnboundEntries = true;
        }
    }

    private void checkForDuplicates(RegistryEntryData<T> entry, RegistryEntryData<T> candidate) {
        if (candidate != null) {
            throw new IllegalStateException("Duplicate entry registered to " + this + ": existing: " + candidate + ", new: " + entry);
        }
    }

    @Override
    public void clear() {
        // Don't trim maps here yet - we expect a similarly sized registry at the next population,
        // and the next freeze/bind calls will trim them if necessary
        byId.clear();
        byKey.clear();
        byValue.clear();
        frozen = false;
        bound = false;
        hasUnboundEntries = false;
    }

    @Override
    public void freeze() {
        if (frozen) {
            return;
        }
        frozen = true;

        // Trim map after freezing - now that the registry is frozen, we don't expect the size to change anymore
        byKey.trim();

        // Automatically call bind if all entries are bound
        // This ensures byValue is populated properly
        if (!hasUnboundEntries) {
            bind(id -> {
                throw new AssertionError("Value requested for " + id + " of " + this + ", but expected all entries to be bound");
            });
        }
    }

    @Override
    public void bind(IntFunction<T> binder) {
        if (bound) {
            return;
        }

        ensureFrozen();
        try {
            byId.forEach((id, entry) -> {
                if (!entry.isBound()) {
                    entry.bind(binder.apply(id));
                }
                // Only populate byValue at this stage, and only once - this ensures that the byValue map is sorted by earliest network ID for a given object
                byValue.putIfAbsent(entry.data(), entry);
            });
        } catch (RuntimeException exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to bind " + this + "! This will result in issues. Report this over at our bugtracker!", exception);
            // Clear the half-populated byValue map, just in case
            byValue.clear();

            // Silently consume the exception - errors will occur later, but this allows the code to move on with an unbound registry
            return;
        }

        // Trim map after binding - registry is fully bound, we don't expect the size to change anymore
        byValue.trim();
        bound = true;
        hasUnboundEntries = false;
    }

    @Override
    public void forEachEntry(Consumer<RegistryEntryData<T>> action) {
        ensureFrozen();
        byId.forEach((id, entry) -> action.accept(entry));
    }

    @Override
    public Iterator<T> iterator() {
        // Iterates over registry values - as such needs the registry to be bound, unlike forEachEntry
        ensureBound(true);
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

    private void ensureFrozen() {
        if (!allowsUnsafeAccess && !frozen) {
            throw new IllegalStateException("Registry " + this + " must be frozen to access its entries");
        }
    }

    // Parameter is a little annoying, let's get rid of that soon (2026-10-16)
    private void ensureBound(boolean allowsUnsafeAccess) {
        ensureFrozen();
        allowsUnsafeAccess = allowsUnsafeAccess && this.allowsUnsafeAccess;
        if ((!allowsUnsafeAccess || hasUnboundEntries) && !bound) {
            throw new IllegalStateException("Registry " + this + " has unbound entries");
        }
    }

    private void ensureMutable() {
        if (frozen) {
            throw new IllegalStateException("Registry " + this + " must not be frozen to register new entries");
        }
    }
}
