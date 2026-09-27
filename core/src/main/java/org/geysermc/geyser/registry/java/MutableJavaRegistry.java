/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import net.kyori.adventure.key.Key;

import java.util.function.IntFunction;

/**
 * A {@link JavaRegistry} that accepts new entries, unless it is frozen.
 *
 * <p>This registry can be in one of 2 states: either it is mutable (the initial state), or it is frozen. In its mutable state,
 * registry entries cannot be queried, and this registry cannot be iterated over, though the {@link JavaRegistry#size()} method is still available.
 * New entries can be registered though, using one of the {@code register} methods.</p>
 *
 * <p>When registering entries, the value of the entry does not have to be known at the time of registering. Registry entries
 * whose value is unknown at the mutable stage are "unbound".</p>
 *
 * <p>You can freeze a registry by using {@link MutableJavaRegistry#freeze(IntFunction)}. When freezing a registry, all unbound entries have
 * to be bound by the {@link IntFunction} passed to the {@code freeze} method. During the binding process, you can access registry entries,
 * but trying to access their value will result in an exception when they're unbound.</p>
 *
 * <p>Calling {@link MutableJavaRegistry#clear()} will clear this registry and reset it back to its initial state.</p>
 *
 * @param <T> the type of the registry
 * @see JavaRegistryLookup
 * @see JavaRegistry
 * @see RegistryEntryData
 */
public interface MutableJavaRegistry<T> extends JavaRegistry<T> {

    /**
     * Adds a new entry to this registry under the given {@code key}. The network ID of this entry is set to the current size of the registry,
     * and the entry will be unbound.
     *
     * @param key the key to register the new entry under
     * @throws IllegalStateException when an entry with this network ID or key already exists
     */
    default void register(Key key) {
        register(size(), key);
    }

    /**
     * Adds a new entry to this registry under the given {@code id} and {@code key}. The entry will be unbound.
     *
     * @param id the network ID to register the new entry under
     * @param key the key to register the new entry under
     * @throws IllegalStateException when an entry with this network ID or key already exists
     */
    default void register(int id, Key key) {
        register(new RegistryEntryData<>(id, key));
    }

    /**
     * Adds a new entry to this registry under the given {@code key}. The network ID of this entry is set to the current size of the registry.
     *
     * @param key the key to register the new entry under
     * @param value the value of the new entry
     * @param <V> the type of the new entry
     * @return the value of the new entry
     * @throws IllegalStateException when an entry with this network ID or key already exists
     */
    default <V extends T> V register(Key key, V value) {
        return register(size(), key, value);
    }

    /**
     * Adds a new entry to this register under the given {@code id} and {@code key}.
     *
     * @param id the network ID to register the new entry under
     * @param key the key to register the new entry under
     * @param value the value of the new entry
     * @param <V> the type of the new entry
     * @return the value of the new entry
     * @throws IllegalStateException when an entry with this network ID or key already exists
     */
    default <V extends T> V register(int id, Key key, V value) {
        RegistryEntryData<T> entry = new RegistryEntryData<>(id, key);
        entry.bind(value);
        register(entry);
        return value;
    }

    /**
     * Adds a new entry to this registry.
     *
     * @param entry the entry to add
     * @throws IllegalStateException when an entry with this network ID or key already exists
     */
    void register(RegistryEntryData<T> entry);

    /**
     * Clears this registry. All entries will be wiped, and the registry will be mutable again, if it was frozen.
     */
    void clear();

    /**
     * Freezes this registry. This method uses the given {@link IntFunction} to bind all unbound registry entries. As a result, after calling this method, all registry entries should be bound.
     *
     * <p>The {@link IntFunction} is given a network ID, and it is expected to return a {@link T} for that respective network ID. If it returns {@code null}, a {@link NullPointerException} is thrown.</p>
     *
     * @param binder the function used to bind unbound registry entries
     */
    void freeze(IntFunction<T> binder);
}
