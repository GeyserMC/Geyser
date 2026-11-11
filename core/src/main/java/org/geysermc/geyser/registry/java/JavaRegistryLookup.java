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
import org.geysermc.geyser.session.GeyserSession;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Provides basic access to a Java registry as defined in {@link JavaRegistries}. Instances of this interface
 * should be used to get access to one or multiple entries in the registry. For iterating over the entire registry,
 * see the extensions provided by {@link JavaRegistry}.
 *
 * <p>Instances of this interface can be obtained through {@link JavaRegistryProvider} (usually provided by {@link GeyserSession#javaRegistries()}),
 * or by using the constants in {@link BuiltInJavaRegistries}.</p>
 *
 * @param <T> the type of the registry
 * @see JavaRegistry
 * @see MutableJavaRegistry
 * @see JavaRegistries
 * @see BuiltInJavaRegistries
 * @see JavaRegistryProvider
 */
public interface JavaRegistryLookup<T> {

    /**
     * Gets the network ID for the given {@code object} in this registry, or throws if it didn't exist.
     *
     * @param object the object to look up
     * @return the network ID correlating to the object
     * @throws IllegalStateException when the {@code object} was not present in this registry
     */
    default int getIdOrThrow(T object) {
        return getId(object).orElseThrow(() -> constructMissingObjectException(object));
    }

    /**
     * Gets the network ID for the given {@code object} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param object the object to look up
     * @return the network ID correlating to the object, or an empty optional if it didn't exist
     */
    default OptionalInt getId(T object) {
        return getByValue(object).map(entry -> OptionalInt.of(entry.id())).orElseGet(OptionalInt::empty);
    }

    /**
     * Gets the network ID for the given {@code key} in this registry, or throws if it didn't exist.
     *
     * @param key the key to look up
     * @return the network ID correlating to the key
     * @throws IllegalStateException when the {@code key} was not present in this registry
     */
    default int getIdOrThrow(Key key) {
        return getId(key).orElseThrow(() -> constructMissingKeyException(key));
    }

    /**
     * Gets the network ID for the given {@code key} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param key the key to look up
     * @return the network ID correlating to the key, or an empty optional if it didn't exist
     */
    default OptionalInt getId(Key key) {
        return getByKey(key).map(entry -> OptionalInt.of(entry.id())).orElseGet(OptionalInt::empty);
    }

    /**
     * Gets the {@link Key} for the given {@code object} in this registry, or throws if it didn't exist.
     *
     * @param object the object to look up
     * @return the {@link Key} correlating to the object
     * @throws IllegalStateException when the {@code object} was not present in this registry
     */
    default Key getKeyOrThrow(T object) {
        return getKey(object).orElseThrow(() -> constructMissingObjectException(object));
    }

    /**
     * Gets the {@link Key} for the given {@code object} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param object the object to look up
     * @return the {@link Key} correlating to the object, or an empty optional if it didn't exist
     */
    default Optional<Key> getKey(T object) {
        return getByValue(object).map(RegistryEntryData::key);
    }

    /**
     * Gets the {@link Key} for the given {@code networkId} in this registry, or throws if it didn't exist.
     *
     * @param networkId the network ID to look up
     * @return the {@link Key} correlating to the network ID
     * @throws IllegalStateException when the {@code networkId} was not present in this registry
     */
    default Key getKeyOrThrow(int networkId) {
        return getKey(networkId).orElseThrow(() -> constructMissingIdException(networkId));
    }

    /**
     * Gets the {@link Key} for the given {@code networkId} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param networkId the network ID to look up
     * @return the {@link Key} correlating to the network ID, or an empty optional if it didn't exist
     */
    default Optional<Key> getKey(int networkId) {
        return getById(networkId).map(RegistryEntryData::key);
    }

    /**
     * Gets the {@link T} for the given {@code networkId} in this registry, or throws if it didn't exist.
     *
     * @param networkId the network ID to look up
     * @return the {@link T} correlating to the network ID
     * @throws IllegalStateException when the {@code networkId} was not present in this registry
     */
    default T getOrThrow(int networkId) {
        return get(networkId).orElseThrow(() -> constructMissingIdException(networkId));
    }

    /**
     * Gets the {@link T} for the given {@code networkId} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param networkId the network ID to look up
     * @return the {@link T} correlating to the network ID, or an empty optional if it didn't exist
     */
    default Optional<T> get(int networkId) {
        return getById(networkId).map(RegistryEntryData::data);
    }

    /**
     * Gets the {@link T} for the given {@code key} in this registry, or throws if it didn't exist.
     *
     * @param key the key to look up
     * @return the {@link T} correlating to the key
     * @throws IllegalStateException when the {@code key} was not present in this registry
     */
    default T getOrThrow(Key key) {
        return get(key).orElseThrow(() -> constructMissingKeyException(key));
    }

    /**
     * Gets the {@link T} for the given {@code key} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param key the key to look up
     * @return the {@link T} correlating to the key, or an empty optional if it didn't exist
     */
    default Optional<T> get(Key key) {
        return getByKey(key).map(RegistryEntryData::data);
    }

    /**
     * Gets the {@link RegistryEntryData} for the given {@code networkId} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param networkId the network ID to look up
     * @return the {@link RegistryEntryData} correlating to the network ID, or an empty optional if it didn't exist
     */
    Optional<RegistryEntryData<T>> getById(int networkId);

    /**
     * Gets the {@link RegistryEntryData} for the given {@code key} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param key the key to look up
     * @return the {@link RegistryEntryData} correlating to the key, or an empty optional if it didn't exist
     */
    Optional<RegistryEntryData<T>> getByKey(Key key);

    /**
     * Gets the {@link RegistryEntryData} for the given {@code object} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param object the object to look up
     * @return the {@link RegistryEntryData} correlating to the object, or an empty optional if it didn't exist
     */
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
