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

package org.geysermc.geyser.session.shared;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

// IntelliJ likes to pretend fastutil's maps never return null
@SuppressWarnings("ConstantValue")
public abstract class SessionSharedCache<K, V> {
    private final Object2ObjectOpenHashMap<K, Value<V>> values = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, Set<K>> sessionHolders = new Object2ObjectOpenHashMap<>();

    SessionSharedCache() {}

    public V getOrCompute(UUID session, K key, Supplier<V> computer) {
        Value<V> value = values.get(key);
        if (value != null) {
            trackKey(session, key);
            value.retain(session);
            return value.value;
        }
        value = new Value<>(computer.get());
        trackKey(session, key);
        value.retain(session);
        values.put(key, value);
        return value.value;
    }

    public void release(UUID session, K key) {
        Value<V> value = values.get(key);
        // if release returns true, we can remove this value and let it collected by GC
        if (value != null && value.release(session)) {
            values.remove(key);
        }
        untrackKey(session, key);
    }

    public void releaseAll(UUID session) {
        Set<K> keys = sessionHolders.get(session);
        if (keys != null) {
            // Remove the set from the holders map before releasing all keys, to prevent a concurrent modification exception
            sessionHolders.remove(session);
            keys.forEach(key -> release(session, key));
        }
    }

    private void trackKey(UUID session, K key) {
        sessionHolders.compute(session, ($, set) -> {
            if (set == null) {
                set = new ObjectOpenHashSet<>();
            }
            set.add(key);
            return set;
        });
    }

    private void untrackKey(UUID session, K key) {
        Set<K> keys = sessionHolders.get(session);
        if (keys != null) {
            keys.remove(key);
        }
    }

    private static class Value<V> {
        private final V value;
        private final Set<UUID> references = new ObjectOpenHashSet<>();

        private Value(V value) {
            this.value = value;
        }

        private void retain(UUID session) {
            references.add(session);
        }

        private boolean release(UUID session) {
            references.remove(session);
            return references.isEmpty();
        }
    }
}
