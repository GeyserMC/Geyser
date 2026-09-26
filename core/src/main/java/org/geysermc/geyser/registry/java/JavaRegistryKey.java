/*
 * Copyright (c) 2024-2026 GeyserMC. http://geysermc.org
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.RegistryCache;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntFunction;

/**
 * Defines a Java registry, which can be hardcoded or data-driven. This class doesn't store registry contents itself, that is handled by {@link RegistryCache} in the case of
 * data-driven registries and other classes in the case of hardcoded registries.
 *
 * <p>This class is used when, for a Java registry, data-driven objects and/or tags need to be loaded. Only one instance of this class should be created for each Java registry. Instances of this
 * class are kept in {@link JavaRegistries}, which also has useful methods for creating instances of this class.</p>
 *
 * <p>This class has a few handy utility methods to convert between the various representations of an object in a registry (network ID, resource location/key, value).</p>
 *
 * @param registryKey the registry key, as it appears on Java.
 * @param <T> the object type this registry holds.
 * @see JavaRegistryProvider
 * @see GeyserSession#javaRegistries()
 */
public record JavaRegistryKey<T>(Key registryKey) {

    public int getIdOrThrow(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getIdOrThrow(object);
    }

    /**
     * Converts an object to its network ID, or -1 if it is not registered.
     */
    public OptionalInt getId(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getId(object);
    }

    public int getIdOrThrow(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).getIdOrThrow(key);
    }

    /**
     * Converts a registered key to its network ID, or -1 if it is not registered.
     */
    public OptionalInt getId(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).getId(key);
    }

    public Key getKeyOrThrow(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getKeyOrThrow(object);
    }

    /**
     * Converts an object to its registered key, or null if it is not registered.
     */
    public Optional<Key> getKey(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getKey(object);
    }

    public Key getKeyOrThrow(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).getKeyOrThrow(networkId);
    }

    /**
     * Converts a network ID to its registered key, or null if it is not registered.
     */
    public Optional<Key> getKey(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).getKey(networkId);
    }

    public T getOrThrow(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).getOrThrow(networkId);
    }

    /**
     * Converts a network ID to an object in this registry, or null if it is not registered.
     */
    public Optional<T> get(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).get(networkId);
    }

    public T getOrThrow(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).getOrThrow(key);
    }

    /**
     * Converts a key to an object in this registry, or null if it is not registered.
     */
    public Optional<T> get(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).get(key);
    }

    public IntFunction<T> resolver(GeyserSession session) {
        return resolver(session.javaRegistries());
    }

    public IntFunction<T> resolver(JavaRegistryProvider registries) {
        return id -> getOrThrow(registries, id);
    }

    public <H> Holder<H> wrapOrThrow(GeyserSession session, Key key) {
        return wrapOrThrow(session.javaRegistries(), key);
    }

    public <H> Holder<H> wrapOrThrow(JavaRegistryProvider registries, Key key) {
        return Holder.ofId(getIdOrThrow(registries, key));
    }

    public <H> Holder<H> wrapOrThrow(GeyserSession session, T object) {
        return wrapOrThrow(session.javaRegistries(), object);
    }

    public <H> Holder<H> wrapOrThrow(JavaRegistryProvider registries, T object) {
        return Holder.ofId(getIdOrThrow(registries, object));
    }

    // TODO introduce
    public Holder<T> wrap(GeyserSession session, T object) {
        return wrap(session.javaRegistries(), object);
    }

    public Holder<T> wrap(JavaRegistryProvider registries, T object) {
        return getId(registries, object).stream()
            .<Holder<T>>mapToObj(Holder::ofId)
            .findFirst()
            .orElseGet(() -> Holder.ofCustom(object));
    }

    @Override
    public @NonNull String toString() {
        return "Java registry: " + registryKey;
    }
}
