/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntFunction;

/**
 * Defines a Java registry, which can be hardcoded or data-driven. This class doesn't store registry contents itself, that is handled by {@link org.geysermc.geyser.session.cache.RegistryCache} in the case of
 * data-driven registries and other classes in the case of hardcoded registries.
 *
 * <p>This class is used when, for a Java registry, data-driven objects and/or tags need to be loaded. Only one instance of this class should be created for each Java registry. Instances of this
 * class are kept in {@link JavaRegistries}, which also has useful methods for creating instances of this class.</p>
 *
 * <p>This class has a few handy utility methods to convert between the various representations of an object in a registry (network ID, resource location/key, value).</p>
 *
 * @param registryKey the registry key, as it appears on Java.
 * @param lookup an implementation of {@link RegistryLookup} that converts an object in this registry to its respective network ID or key, and back.
 * @param <T> the object type this registry holds.
 * @see JavaRegistryProvider
 * @see GeyserSession#javaRegistries()
 */
public record JavaRegistryKey<T>(Key registryKey, RegistryLookup<T> lookup) {

    public int getIdOrThrow(GeyserSession session, T object) {
        return getIdOrThrow(session.javaRegistries(), object);
    }

    /**
     * Converts an object to its network ID, or -1 if it is not registered.
     */
    public OptionalInt getId(GeyserSession session, T object) {
        return getId(session.javaRegistries(), object);
    }

    public int getIdOrThrow(JavaRegistryProvider registries, T object) {
        return getId(registries, object).orElseThrow(() -> constructMissingObjectException(registries, object));
    }

    /**
     * Converts an object to its network ID, or -1 if it is not registered.
     */
    public OptionalInt getId(JavaRegistryProvider registries, T object) {
        return entry(registries, object).stream().mapToInt(RegistryEntryData::id).findAny();
    }

    public int getIdOrThrow(GeyserSession session, Key key) {
        return getIdOrThrow(session.javaRegistries(), key);
    }

    /**
     * Converts a registered key to its network ID, or -1 if it is not registered.
     */
    public OptionalInt getId(GeyserSession session, Key key) {
        return getId(session.javaRegistries(), key);
    }

    public int getIdOrThrow(JavaRegistryProvider registries, Key key) {
        return getId(registries, key).orElseThrow(() -> constructMissingKeyException(registries, key));
    }

    /**
     * Converts a registered key to its network ID, or -1 if it is not registered.
     */
    public OptionalInt getId(JavaRegistryProvider registries, Key key) {
        return entry(registries, key).stream().mapToInt(RegistryEntryData::id).findAny();
    }

    public Key getKeyOrThrow(GeyserSession session, T object) {
        return getKeyOrThrow(session.javaRegistries(), object);
    }

    /**
     * Converts an object to its registered key, or null if it is not registered.
     */
    public Optional<Key> getKey(GeyserSession session, T object) {
        return getKey(session.javaRegistries(), object);
    }

    public Key getKeyOrThrow(JavaRegistryProvider registries, T object) {
        return getKey(registries, object).orElseThrow(() -> constructMissingObjectException(registries, object));
    }

    /**
     * Converts an object to its registered key, or null if it is not registered.
     */
    public Optional<Key> getKey(JavaRegistryProvider registries, T object) {
        return entry(registries, object).map(RegistryEntryData::key);
    }

    public Key getKeyOrThrow(GeyserSession session, int networkId) {
        return getKeyOrThrow(session.javaRegistries(), networkId);
    }

    /**
     * Converts a network ID to its registered key, or null if it is not registered.
     */
    public Optional<Key> getKey(GeyserSession session, int networkId) {
        return getKey(session.javaRegistries(), networkId);
    }

    public Key getKeyOrThrow(JavaRegistryProvider registries, int networkId) {
        return getKey(registries, networkId).orElseThrow(() -> constructMissingIdException(registries, networkId));
    }

    /**
     * Converts a network ID to its registered key, or null if it is not registered.
     */
    public Optional<Key> getKey(JavaRegistryProvider registries, int networkId) {
        return entry(registries, networkId).map(RegistryEntryData::key);
    }

    public T getOrThrow(GeyserSession session, int networkId) {
        return getOrThrow(session.javaRegistries(), networkId);
    }

    /**
     * Converts a network ID to an object in this registry, or null if it is not registered.
     */
    public Optional<T> get(GeyserSession session, int networkId) {
        return get(session.javaRegistries(), networkId);
    }

    public T getOrThrow(JavaRegistryProvider registries, int networkId) {
        return get(registries, networkId).orElseThrow(() -> constructMissingIdException(registries, networkId));
    }

    /**
     * Converts a network ID to an object in this registry, or null if it is not registered.
     */
    public Optional<T> get(JavaRegistryProvider registries, int networkId) {
        return entry(registries, networkId).map(RegistryEntryData::data);
    }

    public T getOrThrow(GeyserSession session, Key key) {
        return getOrThrow(session.javaRegistries(), key);
    }

    /**
     * Converts a key to an object in this registry, or null if it is not registered.
     */
    public Optional<T> get(GeyserSession session, Key key) {
        return get(session.javaRegistries(), key);
    }

    public T getOrThrow(JavaRegistryProvider registries, Key key) {
        return get(registries, key).orElseThrow(() -> constructMissingKeyException(registries, key));
    }

    /**
     * Converts a key to an object in this registry, or null if it is not registered.
     */
    public Optional<T> get(JavaRegistryProvider registries, Key key) {
        return entry(registries, key).map(RegistryEntryData::data);
    }

    public IntFunction<T> resolver(GeyserSession session) {
        return resolver(session.javaRegistries());
    }

    public IntFunction<T> resolver(JavaRegistryProvider registries) {
        return id -> getOrThrow(registries, id);
    }

    private Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, T object) {
        return lookup.entry(registries, this, object);
    }

    private Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, int networkId) {
        return lookup.entry(registries, this, networkId);
    }

    private Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, Key key) {
        return lookup.entry(registries, this, key);
    }

    /**
     * Implementations should look up an element in the given registry by its value, network ID, or registered key. Return an empty optional if it does not exist.
     */
    public interface RegistryLookup<T> {

        Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registry, int networkId);

        Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registry, Key key);

        Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registry, T object);
    }

    @Override
    public @NonNull String toString() {
        return "Java registry: " + registryKey;
    }

    private IllegalStateException constructMissingObjectException(JavaRegistryProvider registries, T object) {
        return constructMissingException(registries, "object " + object);
    }

    private IllegalStateException constructMissingKeyException(JavaRegistryProvider registries, Key key) {
        return constructMissingException(registries, "key " + key);
    }

    private IllegalStateException constructMissingIdException(JavaRegistryProvider registries, int id) {
        return constructMissingException(registries, "ID " + id);
    }

    private IllegalStateException constructMissingException(JavaRegistryProvider registries, String missing) {
        String message = "Missing " + missing + " in Java registry " + registryKey;
        if (isDebug()) {
            message += "\nProvider: " + registries + " | registry data: " + registries.registry(this).entries();
        }
        return new IllegalStateException(message);
    }

    private static boolean isDebug() {
        return GeyserImpl.getInstance().config().debugMode();
    }
}
