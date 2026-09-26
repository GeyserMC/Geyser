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

package org.geysermc.geyser.session.cache.registry;

import net.kyori.adventure.key.Key;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Implementations should look up an element in the given registry by its value, network ID, or registered key. Return an empty optional if it does not exist.
 */
public interface JavaRegistryLookup<T> {

    default int getIdOrThrow(T object) {
        return getId(object).orElseThrow(() -> constructMissingObjectException(object));
    }

    /**
     * Converts an object to its network ID, or -1 if it is not registered.
     */
    default OptionalInt getId(T object) {
        return getByValue(object).stream().mapToInt(RegistryEntryData::id).findAny();
    }

    default int getIdOrThrow(Key key) {
        return getId(key).orElseThrow(() -> constructMissingKeyException(key));
    }

    /**
     * Converts a registered key to its network ID, or -1 if it is not registered.
     */
    default OptionalInt getId(Key key) {
        return getByKey(key).stream().mapToInt(RegistryEntryData::id).findAny();
    }

    default Key getKeyOrThrow(T object) {
        return getKey(object).orElseThrow(() -> constructMissingObjectException(object));
    }

    /**
     * Converts an object to its registered key, or null if it is not registered.
     */
    default Optional<Key> getKey(T object) {
        return getByValue(object).map(RegistryEntryData::key);
    }

    default Key getKeyOrThrow(int networkId) {
        return getKey(networkId).orElseThrow(() -> constructMissingIdException(networkId));
    }

    /**
     * Converts a network ID to its registered key, or null if it is not registered.
     */
    default Optional<Key> getKey(int networkId) {
        return getById(networkId).map(RegistryEntryData::key);
    }

    default T getOrThrow(int networkId) {
        return get(networkId).orElseThrow(() -> constructMissingIdException(networkId));
    }

    /**
     * Converts a network ID to an object in this registry, or null if it is not registered.
     */
    default Optional<T> get(int networkId) {
        return getById(networkId).map(RegistryEntryData::data);
    }

    default T getOrThrow(Key key) {
        return get(key).orElseThrow(() -> constructMissingKeyException(key));
    }

    /**
     * Converts a key to an object in this registry, or null if it is not registered.
     */
    default Optional<T> get(Key key) {
        return getByKey(key).map(RegistryEntryData::data);
    }

    Optional<RegistryEntryData<T>> getById(int networkId);

    Optional<RegistryEntryData<T>> getByKey(Key key);

    Optional<RegistryEntryData<T>> getByValue(T object);

    private IllegalStateException constructMissingObjectException(T object) {
        return constructMissingException("object " + object);
    }

    private IllegalStateException constructMissingKeyException(Key key) {
        return constructMissingException("key " + key);
    }

    private IllegalStateException constructMissingIdException(int id) {
        return constructMissingException("ID " + id);
    }

    private IllegalStateException constructMissingException(String missing) {
        String message = "Missing " + missing + " in Java registry " + this;
        return new IllegalStateException(message);
    }
}
