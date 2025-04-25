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
import org.geysermc.geyser.session.GeyserSession;

/**
 * Defines a Java registry, which can be hardcoded or data-driven. This class doesn't store registry contents itself, that is handled by {@link org.geysermc.geyser.session.cache.RegistryCache} in the case of
 * data-driven registries and other classes in the case of hardcoded registries.
 *
 * <p>This class is used when, for a Java registry, data-driven objects and/or tags need to be loaded. Only one instance of this class should be created for each Java registry. Instances of this
 * class are kept in {@link JavaRegistries}, which also has useful methods for creating instances of this class.</p>
 *
 * @param registryKey the registry key, as it appears on Java.
 * @param networkSerializer a method that converts an object in this registry to its network ID.
 * @param networkDeserializer a method that converts a network ID to an object in this registry.
 * @param networkIdentifier a method that converts a network ID to its respective key in this registry.
 * @param <T> the object type this registry holds.
 */
public record JavaRegistryKey<T>(Key registryKey, NetworkSerializer<T> networkSerializer, NetworkDeserializer<T> networkDeserializer, NetworkIdentifier<T> networkIdentifier) {

    /**
     * Converts an object in this registry to its network ID.
     */
    public int toNetworkId(GeyserSession session, T object) {
        return networkSerializer.toNetworkId(session, this, object);
    }

    /**
     * Converts a network ID to an object in this registry.
     */
    public T fromNetworkId(GeyserSession session, int networkId) {
        return networkDeserializer.fromNetworkId(session, this, networkId);
    }

    /**
     * Converts a network ID to the key it's registered under in this registry.
     */
    public Key keyFromNetworkId(GeyserSession session, int networkId) {
        return networkIdentifier.keyFromNetworkId(session, this, networkId);
    }

    @FunctionalInterface
    public interface NetworkSerializer<T> {

        int toNetworkId(GeyserSession session, JavaRegistryKey<T> registry, T object);
    }

    @FunctionalInterface
    public interface NetworkDeserializer<T> {

        T fromNetworkId(GeyserSession session, JavaRegistryKey<T> registry, int networkId);
    }

    @FunctionalInterface
    public interface NetworkIdentifier<T> {

        Key keyFromNetworkId(GeyserSession session, JavaRegistryKey<T> registry, int networkId);
    }
}
