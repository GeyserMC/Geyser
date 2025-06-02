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

/**
 * Defines a Java registry, which can be hardcoded or data-driven. This class doesn't store registry contents itself, that is handled by {@link org.geysermc.geyser.session.cache.RegistryCache} in the case of
 * data-driven registries and other classes in the case of hardcoded registries.
 *
 * <p>This class is used when, for a Java registry, data-driven objects and/or tags need to be loaded. Only one instance of this class should be created for each Java registry. Instances of this
 * class are kept in {@link JavaRegistries}, which also has useful methods for creating instances of this class.</p>
 *
 * @param registryKey the registry key, as it appears on Java.
 * @param registryLookup an implementation of {@link RegistryLookup} that converts an object in this registry to its respective network ID or key, and back.
 * @param <T> the object type this registry holds.
 */
public record JavaRegistryKey<T>(Key registryKey, RegistryLookup<T> registryLookup) {

    /**
     * Converts an object in this registry to its network ID, or -1 if it is not registered.
     */
    public int toNetworkId(GeyserSession session, T object) {
        return registryLookup.toNetworkId(session, this, object);
    }

    /**
     * Converts a network ID to an object in this registry, or null if it is not registered.
     */
    public @Nullable T fromNetworkId(GeyserSession session, int networkId) {
        return registryLookup.fromNetworkId(session, this, networkId);
    }

    /**
     * Converts a key registered under this registry to its network ID, or -1 if it is not registered.
     */
    public int keyToNetworkId(GeyserSession session, Key key) {
        return registryLookup.keyToNetworkId(session, this, key);
    }

    /**
     * Converts a network ID to the key it is registered under in this registry, or null if it is not registered.
     */
    public @Nullable Key keyFromNetworkId(GeyserSession session, int networkId) {
        return registryLookup.keyFromNetworkId(session, this, networkId);
    }

    public interface RegistryLookup<T> {

        /**
         * Implementations should return the network ID of the registered object, or -1 if it is not registered.
         */
        int toNetworkId(GeyserSession session, JavaRegistryKey<T> registry, T object);

        /**
         * Implementations should return the object that is registered under the given network ID, or null if it is not registered.
         */
        @Nullable
        T fromNetworkId(GeyserSession session, JavaRegistryKey<T> registry, int networkId);

        /**
         * Implementations should return the network ID that corresponds to the given registered key, or -1 if it is not registered.
         */
        int keyToNetworkId(GeyserSession session, JavaRegistryKey<T> registry, Key key);

        /**
         * Implementations should return the key that corresponds to the registered network ID, or null if it is not registered.
         */
        @Nullable
        Key keyFromNetworkId(GeyserSession session, JavaRegistryKey<T> registry, int networkId);
    }

    @Override
    public @NonNull String toString() {
        return "Java registry: " + registryKey;
    }
}
