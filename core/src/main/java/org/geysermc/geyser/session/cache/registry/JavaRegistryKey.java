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

import javax.annotation.Nullable;

public record JavaRegistryKey<T>(Key registryKey, @Nullable NetworkSerializer<T> networkSerializer, @Nullable NetworkDeserializer<T> networkDeserializer) {

    public int toNetworkId(GeyserSession session, T object) {
        if (networkSerializer == null) {
            throw new UnsupportedOperationException("Registry does not hava a network serializer");
        }
        return networkSerializer.toNetworkId(session, object);
    }

    public T fromNetworkId(GeyserSession session, int networkId) {
        if (networkDeserializer == null) {
            throw new UnsupportedOperationException("Registry does not hava a network deserializer");
        }
        return networkDeserializer.fromNetworkId(session, networkId);
    }

    @FunctionalInterface
    public interface NetworkSerializer<T> {

        int toNetworkId(GeyserSession session, T object);
    }

    @FunctionalInterface
    public interface NetworkDeserializer<T> {

        T fromNetworkId(GeyserSession session, int networkId);
    }
}
