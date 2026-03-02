/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache.registry;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A wrapper for a list, holding Java registry values.
 */
public interface JavaRegistry<T> {

    /**
     * Looks up a registry entry by its ID. The object can be null, or not present.
     */
    default @Nullable T byId(@NonNegative int id) {
        return entryById(id).map(RegistryEntryData::data).orElse(null);
    }

    /**
     * Looks up a registry entry by its ID, and returns it wrapped in {@link RegistryEntryData} so that its registered key is also known. The object can be null, or not present.
     */
    default Optional<RegistryEntryData<T>> entryById(@NonNegative int id) {
        List<RegistryEntryData<T>> entries = entries();
        if (id < 0 || id >= entries.size()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(id));
    }

    /**
     * Looks up a registry entry by its key. The object can be null, or not present.
     */
    default @Nullable T byKey(Key key) {
        return entryByKey(key).map(RegistryEntryData::data).orElse(null);
    }

    /**
     * Looks up a registry entry by its key, and returns it wrapped in {@link RegistryEntryData}. The object can be null, or not present.
     */
    default Optional<RegistryEntryData<T>> entryByKey(Key key) {
        List<RegistryEntryData<T>> entries = entries();
        for (RegistryEntryData<T> entry : entries) {
            if (entry.key().equals(key)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Reverse looks-up an object to return its network ID, or -1.
     */
    default int byValue(T value) {
        return entryByValue(value).map(RegistryEntryData::id).orElse(-1);
    }

    /**
     * Reverse looks-up an object to return it wrapped in {@link RegistryEntryData}, or null.
     */
    default Optional<RegistryEntryData<T>> entryByValue(T value) {
        List<RegistryEntryData<T>> entries = entries();
        for (RegistryEntryData<T> entry : entries) {
            if (entry.data().equals(value)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * All keys of this registry, as a list.
     */
    default List<Key> keys() {
        return entries().stream().map(RegistryEntryData::key).toList();
    }

    /**
     * All values of this registry, as a list.
     */
    default List<T> values() {
        return entries().stream().map(RegistryEntryData::data).toList();
    }

    /**
     * The amount of values registered in this registry.
     */
    default int size() {
        return entries().size();
    }

    /**
     * All entries of this registry, as a list.
     */
    List<RegistryEntryData<T>> entries();
}
