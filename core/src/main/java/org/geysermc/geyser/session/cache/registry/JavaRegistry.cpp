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

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.Nullable"

#include "java.util.List"
#include "java.util.Optional"


public interface JavaRegistry<T> {


    default T byId(@NonNegative int id) {
        return entryById(id).map(RegistryEntryData::data).orElse(null);
    }


    default Optional<RegistryEntryData<T>> entryById(@NonNegative int id) {
        List<RegistryEntryData<T>> entries = entries();
        if (id < 0 || id >= entries.size()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(id));
    }


    default T byKey(Key key) {
        return entryByKey(key).map(RegistryEntryData::data).orElse(null);
    }


    default Optional<RegistryEntryData<T>> entryByKey(Key key) {
        List<RegistryEntryData<T>> entries = entries();
        for (RegistryEntryData<T> entry : entries) {
            if (entry.key().equals(key)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }


    default int byValue(T value) {
        return entryByValue(value).map(RegistryEntryData::id).orElse(-1);
    }


    default Optional<RegistryEntryData<T>> entryByValue(T value) {
        List<RegistryEntryData<T>> entries = entries();
        for (RegistryEntryData<T> entry : entries) {
            if (entry.data().equals(value)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }


    default List<Key> keys() {
        return entries().stream().map(RegistryEntryData::key).toList();
    }


    default List<T> values() {
        return entries().stream().map(RegistryEntryData::data).toList();
    }


    default int size() {
        return entries().size();
    }


    List<RegistryEntryData<T>> entries();
}
