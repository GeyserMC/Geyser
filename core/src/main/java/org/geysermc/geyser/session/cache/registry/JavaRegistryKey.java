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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Optional;

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
 */
public record JavaRegistryKey<T>(Key registryKey, RegistryLookup<T> lookup) {

    /**
     * Converts an object to its network ID, or -1 if it is not registered.
     */
    public int networkId(GeyserSession session, T object) {
        return networkId(session.getRegistryCache(), object);
    }

    /**
     * Converts an object to its network ID, or -1 if it is not registered.
     */
    public int networkId(JavaRegistryProvider registries, T object) {
        return entry(registries, object).map(RegistryEntryData::id).orElse(-1);
    }

    /**
     * Converts a registered key to its network ID, or -1 if it is not registered.
     */
    public int networkId(GeyserSession session, Key key) {
        return networkId(session.getRegistryCache(), key);
    }

    /**
     * Converts a registered key to its network ID, or -1 if it is not registered.
     */
    public int networkId(JavaRegistryProvider registries, Key key) {
        return entry(registries, key).map(RegistryEntryData::id).orElse(-1);
    }

    /**
     * Converts an object to its registered key, or null if it is not registered.
     */
    public @Nullable Key key(GeyserSession session, T object) {
        return key(session.getRegistryCache(), object);
    }

    /**
     * Converts an object to its registered key, or null if it is not registered.
     */
    public @Nullable Key key(JavaRegistryProvider registries, T object) {
        return entry(registries, object).map(RegistryEntryData::key).orElse(null);
    }

    /**
     * Converts a network ID to its registered key, or null if it is not registered.
     */
    public @Nullable Key key(GeyserSession session, int networkId) {
        return key(session.getRegistryCache(), networkId);
    }

    /**
     * Converts a network ID to its registered key, or null if it is not registered.
     */
    public @Nullable Key key(JavaRegistryProvider registries, int networkId) {
        return entry(registries, networkId).map(RegistryEntryData::key).orElse(null);
    }

    /**
     * Converts a network ID to an object in this registry, or null if it is not registered.
     */
    public @Nullable T value(GeyserSession session, int networkId) {
        return value(session.getRegistryCache(), networkId);
    }

    /**
     * Converts a network ID to an object in this registry, or null if it is not registered.
     */
    public @Nullable T value(JavaRegistryProvider registries, int networkId) {
        return entry(registries, networkId).map(RegistryEntryData::data).orElse(null);
    }

    /**
     * Converts a key to an object in this registry, or null if it is not registered.
     */
    public @Nullable T value(GeyserSession session, Key key) {
        return value(session.getRegistryCache(), key);
    }

    /**
     * Converts a key to an object in this registry, or null if it is not registered.
     */
    public @Nullable T value(JavaRegistryProvider registries, Key key) {
        return entry(registries, key).map(RegistryEntryData::data).orElse(null);
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
}
