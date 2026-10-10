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
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntFunction;

/**
 * This record is essentially a wrapper around a {@link Key} for a Java registry, be it built-in or networked, as defined in {@link JavaRegistries}.
 * The purpose of this record is to provide a static reference to a registry, as well as to add a generic {@link T}-variable to the {@link Key},
 * though this record also provides shorthand methods for accessing a registry's contents using a {@link JavaRegistryProvider}.
 *
 * <p>All instances of this record must be kept in {@link JavaRegistries}.</p>
 *
 * @param registryKey the {@link Key} of this registry
 * @param <T> the type of this registry
 * @see JavaRegistries
 * @see BuiltInJavaRegistries
 * @see JavaRegistryReaders
 * @see JavaRegistryLookup
 * @see JavaRegistry
 * @see MutableJavaRegistry
 */
public record JavaRegistryKey<T>(Key registryKey) {

    // The following methods mirror those in JavaRegistryLookup, for convenience:

    /**
     * Gets the network ID for the given {@code object} in this registry, or throws if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param object the object to look up
     * @return the network ID correlating to the object
     * @throws IllegalStateException when the {@code object} was not present in this registry
     */
    public int getIdOrThrow(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getIdOrThrow(object);
    }

    /**
     * Gets the network ID for the given {@code object} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param object the object to look up
     * @return the network ID correlating to the object, or an empty optional if it didn't exist
     */
    public OptionalInt getId(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getId(object);
    }

    /**
     * Gets the network ID for the given {@code key} in this registry, or throws if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param key the key to look up
     * @return the network ID correlating to the key
     * @throws IllegalStateException when the {@code key} was not present in this registry
     */
    public int getIdOrThrow(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).getIdOrThrow(key);
    }

    /**
     * Gets the network ID for the given {@code key} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param key the key to look up
     * @return the network ID correlating to the key, or an empty optional if it didn't exist
     */
    public OptionalInt getId(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).getId(key);
    }

    /**
     * Gets the {@link Key} for the given {@code object} in this registry, or throws if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param object the object to look up
     * @return the {@link Key} correlating to the object
     * @throws IllegalStateException when the {@code object} was not present in this registry
     */
    public Key getKeyOrThrow(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getKeyOrThrow(object);
    }

    /**
     * Gets the {@link Key} for the given {@code object} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param object the object to look up
     * @return the {@link Key} correlating to the object, or an empty optional if it didn't exist
     */
    public Optional<Key> getKey(JavaRegistryProvider registries, T object) {
        return registries.registry(this).getKey(object);
    }

    /**
     * Gets the {@link Key} for the given {@code networkId} in this registry, or throws if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param networkId the network ID to look up
     * @return the {@link Key} correlating to the network ID
     * @throws IllegalStateException when the {@code networkId} was not present in this registry
     */
    public Key getKeyOrThrow(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).getKeyOrThrow(networkId);
    }

    /**
     * Gets the {@link Key} for the given {@code networkId} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param networkId the network ID to look up
     * @return the {@link Key} correlating to the network ID, or an empty optional if it didn't exist
     */
    public Optional<Key> getKey(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).getKey(networkId);
    }

    /**
     * Gets the {@link T} for the given {@code networkId} in this registry, or throws if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param networkId the network ID to look up
     * @return the {@link T} correlating to the network ID
     * @throws IllegalStateException when the {@code networkId} was not present in this registry
     */
    public T getOrThrow(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).getOrThrow(networkId);
    }

    /**
     * Gets the {@link T} for the given {@code networkId} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param networkId the network ID to look up
     * @return the {@link T} correlating to the network ID, or an empty optional if it didn't exist
     */
    public Optional<T> get(JavaRegistryProvider registries, int networkId) {
        return registries.registry(this).get(networkId);
    }

    /**
     * Gets the {@link T} for the given {@code key} in this registry, or throws if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param key the key to look up
     * @return the {@link T} correlating to the key
     * @throws IllegalStateException when the {@code key} was not present in this registry
     */
    public T getOrThrow(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).getOrThrow(key);
    }

    /**
     * Gets the {@link T} for the given {@code key} in this registry, or returns an empty optional if it didn't exist.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param key the key to look up
     * @return the {@link T} correlating to the key, or an empty optional if it didn't exist
     */
    public Optional<T> get(JavaRegistryProvider registries, Key key) {
        return registries.registry(this).get(key);
    }

    // These methods are unique to this record:


    /**
     * Shorthand method for {@link JavaRegistryKey#resolver(JavaRegistryProvider)}.
     *
     * @param session the {@link GeyserSession}
     * @return an {@link IntFunction} that turns a network ID of this registry into a {@link T}
     * @see JavaRegistryKey#resolver(JavaRegistryProvider)
     */
    public IntFunction<T> resolver(GeyserSession session) {
        return resolver(session.javaRegistries());
    }

    /**
     * Returns an {@link IntFunction} that turns a network ID of this registry into a {@link T}. Useful to combine with {@link Holder#getOrCompute(IntFunction)}.
     *
     * <p>Note that the returned function will use {@link JavaRegistryKey#getOrThrow(JavaRegistryProvider, int)} and as such throw an {@link IllegalStateException}
     * on missing IDs.</p>
     *
     * @param registries the {@link JavaRegistryProvider}
     * @return an {@link IntFunction} that turns a network ID of this registry into a {@link T}
     */
    public IntFunction<T> resolver(JavaRegistryProvider registries) {
        return id -> getOrThrow(registries, id);
    }

    /**
     * Shorthand method for {@link JavaRegistryKey#wrapOrThrow(JavaRegistryProvider, Key)}.
     *
     * @param session the {@link GeyserSession}
     * @param key the key to wrap into a {@link Holder}
     * @param <H> the type of the holder
     * @return a holder representing the key
     * @throws IllegalStateException when the {@code key} was not present in this registry
     * @see JavaRegistryKey#wrapOrThrow(JavaRegistryProvider, Key)
     */
    public <H> Holder<H> wrapOrThrow(GeyserSession session, Key key) {
        return wrapOrThrow(session.javaRegistries(), key);
    }

    /**
     * Gets the network ID of the given {@code key}, and wraps it into a {@link Holder} using {@link Holder#ofId(int)}.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param key the key to wrap into a {@link Holder}
     * @param <H> the type of the holder
     * @return a holder representing the key
     * @throws IllegalStateException when the {@code key} was not present in this registry
     * @see JavaRegistryKey#wrapOrThrow(GeyserSession, Object)
     */
    public <H> Holder<H> wrapOrThrow(JavaRegistryProvider registries, Key key) {
        return Holder.ofId(getIdOrThrow(registries, key));
    }

    /**
     * Shorthand method for {@link JavaRegistryKey#wrapOrThrow(JavaRegistryProvider, Object)}.
     *
     * @param session the {@link GeyserSession}
     * @param object the object to wrap into a {@link Holder}
     * @param <H> the type of the holder
     * @return a holder representing the object
     * @throws IllegalStateException when the {@code object} was not present in this registry
     * @see JavaRegistryKey#wrapOrThrow(JavaRegistryProvider, Object)
     */
    public <H> Holder<H> wrapOrThrow(GeyserSession session, T object) {
        return wrapOrThrow(session.javaRegistries(), object);
    }

    /**
     * Gets the network ID of the given {@code object}, and wraps it into a {@link Holder} using {@link Holder#ofId(int)}.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param object the object to wrap into a {@link Holder}
     * @param <H> the type of the holder
     * @return a holder representing the object
     * @throws IllegalStateException when the {@code object} was not present in this registry
     * @see JavaRegistryKey#wrapOrThrow(GeyserSession, Key)
     */
    public <H> Holder<H> wrapOrThrow(JavaRegistryProvider registries, T object) {
        return Holder.ofId(getIdOrThrow(registries, object));
    }

    /**
     * Shorthand method for {@link JavaRegistryKey#wrap(JavaRegistryProvider, Object)}.
     *
     * @param session the {@link GeyserSession}
     * @param object the object to wrap into a {@link Holder}
     * @return a holder representing the object
     */
    public Holder<T> wrap(GeyserSession session, T object) {
        return wrap(session.javaRegistries(), object);
    }

    /**
     * Tries to get the network ID of the given {@code object}, wrapping it into a {@link Holder} using {@link Holder#ofId(int)}.
     * If the {@code object} was not present in this registry, then it is wrapped into a {@link Holder} using {@link Holder#ofCustom(Object)}.
     *
     * @param registries the {@link JavaRegistryProvider}
     * @param object the object to wrap into a {@link Holder}
     * @return a holder representing the object
     */
    public Holder<T> wrap(JavaRegistryProvider registries, T object) {
        return getId(registries, object).stream()
            .<Holder<T>>mapToObj(Holder::ofId)
            .findFirst()
            .orElseGet(() -> Holder.ofCustom(object));
    }

    @Override
    public String toString() {
        return "Java registry: " + registryKey;
    }
}
