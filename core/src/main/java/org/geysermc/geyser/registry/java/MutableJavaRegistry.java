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
 * <p>This registry can be in one of 3 states, in the following order:</p>
 *
 * <ol>
 *     <li>In its initial state, it is mutable. New entries can be registered, but entries cannot be queried, and the registry cannot be iterated over.
 *     {@link JavaRegistry#size()} is still available, however. The value of a registry entry does not have to be known at the time of registering:
 *     it is fine to register an entry only by its {@code networkId} and {@link Key}. This can be useful when needing to refer to other
 *     registries during the binding step (see state 3).</li>
 *     <li>Once all entries are registered, the registry must be frozen, by calling {@link MutableJavaRegistry#freeze()}.
 *     After freezing the registry, entries can no longer be registered. Entries can be queried however,
 *     and if all entries were "known" at the time of registering, this registry automatically considers itself bound.</li>
 *     <li>If not all entries were "known" at the time of registering, a registry must be bound by calling {@link MutableJavaRegistry#bind(IntFunction)}.
 *     After the registry has been bound, it is fully functional. To reset the registry back to its initial, mutable state, call {@link MutableJavaRegistry#clear()}.</li>
 * </ol>
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
     * @throws IllegalStateException when the registry was frozen
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
     * @throws IllegalStateException when the registry was frozen
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
     * @throws IllegalStateException when the registry was frozen
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
     * @throws IllegalStateException when the registry was frozen
     * @throws NullPointerException when the {@code value} was null
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
     * @throws IllegalStateException when the registry was frozen
     */
    void register(RegistryEntryData<T> entry);

    /**
     * Clears this registry. All entries will be wiped, and the registry will be mutable again, if it was frozen.
     */
    void clear();

    /**
     * Freezes this registry, if it was not frozen already.
     *
     * <p>If all entries were bound at the time of registering, then {@link MutableJavaRegistry#bind(IntFunction)} is automatically
     * called with a no-op binder, ensuring this registry is bound and fully functional.</p>
     *
     * <p>If the registry was already frozen, this method does nothing.</p>
     *
     * @see MutableJavaRegistry#bind(IntFunction)
     */
    void freeze();

    /**
     * Binds this registry, if there were any unbound entries. The registry must be frozen before binding it.
     *
     * <p>This method uses the given {@link IntFunction} to bind all unbound registry entries. As a result, after calling this method, all registry entries should be bound.</p>
     *
     * <p>The {@link IntFunction} is given a network ID, and it is expected to return a {@link T} for that respective network ID. If it returns {@code null}, a {@link NullPointerException} is thrown.</p>
     *
     * <p>If the registry was already bound, this method does nothing. If the binder throws a {@link RuntimeException}, the exception will be caught, and this method will silently return.
     * The registry will be left unbound.</p>
     *
     * @param binder the function used to bind unbound registry entries
     * @throws IllegalStateException when the registry is not frozen
     * @throws NullPointerException when the binder returned null for an ID
     * @see MutableJavaRegistry#freeze()
     */
    void bind(IntFunction<T> binder);
}
