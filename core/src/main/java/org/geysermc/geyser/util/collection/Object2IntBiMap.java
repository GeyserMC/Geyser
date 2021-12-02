/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * A primitive int BiMap implementation built around fastutil to
 * reduce boxing and the memory footprint. Protocol has a
 * {@link com.nukkitx.protocol.util.Int2ObjectBiMap} class, but it
 * does not extend the Map interface making it difficult to utilize
 * it in for loops and the registry system.
 *
 * @param <T> the value
 */
public class Object2IntBiMap<T> implements Object2IntMap<T> {
    private final Object2IntMap<T> forwards;
    private final Int2ObjectMap<T> backwards;

    public Object2IntBiMap() {
        this(16);
    }

    public Object2IntBiMap(int expected) {
        this(expected, 0.75F);
    }

    public Object2IntBiMap(T defaultForwardsValue) {
        this(16, 0.75F, defaultForwardsValue, -1);
    }

    public Object2IntBiMap(int expected, float loadFactor) {
        this(expected, loadFactor, -1);
    }

    public Object2IntBiMap(int expected, float loadFactor, int defaultBackwardsValue) {
        this(expected, loadFactor, null, defaultBackwardsValue);
    }

    public Object2IntBiMap(int expected, float loadFactor, T defaultForwardsValue, int defaultBackwardsValue) {
        this.forwards = new Object2IntOpenHashMap<>(expected, loadFactor);
        this.backwards = new Int2ObjectOpenHashMap<>(expected, loadFactor);
        this.forwards.defaultReturnValue(defaultBackwardsValue);
        this.backwards.defaultReturnValue(defaultForwardsValue);
    }

    @Override
    public int size() {
        return this.forwards.size();
    }

    @Override
    public boolean isEmpty() {
        return this.forwards.isEmpty();
    }

    @Override
    public int getInt(Object o) {
        return this.forwards.getInt(o);
    }

    public T get(int key) {
        return this.backwards.get(key);
    }

    @Override
    public int getOrDefault(Object key, int defaultValue) {
        return this.forwards.getOrDefault(key, defaultValue);
    }

    public T getOrDefault(int key, T defaultValue) {
        return this.backwards.getOrDefault(key, defaultValue);
    }

    @Override
    public void defaultReturnValue(int i) {
        this.forwards.defaultReturnValue(i);
    }

    public void defaultReturnValue(T v) {
        this.backwards.defaultReturnValue(v);
    }

    @Override
    public int defaultReturnValue() {
        return this.forwards.defaultReturnValue();
    }

    public T backwardsDefaultReturnValue() {
        return this.backwards.defaultReturnValue();
    }

    @Override
    public ObjectSet<Entry<T>> object2IntEntrySet() {
        return ObjectSets.unmodifiable(this.forwards.object2IntEntrySet());
    }

    public ObjectSet<Int2ObjectMap.Entry<T>> int2ObjectEntrySet() {
        return ObjectSets.unmodifiable(this.backwards.int2ObjectEntrySet());
    }

    @Override
    public ObjectSet<T> keySet() {
        return this.forwards.keySet();
    }

    @Override
    public IntCollection values() {
        return this.forwards.values();
    }

    @Override
    public boolean containsKey(Object o) {
        return this.forwards.containsKey(o);
    }

    @Override
    public boolean containsValue(int i) {
        return this.backwards.containsKey(i);
    }

    @Override
    public int put(T key, int value) {
        this.backwards.put(value, key);
        return this.forwards.put(key, value);
    }

    @Override
    public void putAll(@NotNull Map<? extends T, ? extends Integer> m) {
        this.forwards.putAll(m);
        for (Map.Entry<? extends T, ? extends Integer> entry : m.entrySet()) {
            this.backwards.put((int) entry.getValue(), entry.getKey());
        }
    }

    @Override
    public int removeInt(Object key) {
        if (!this.forwards.containsKey(key)) {
            return this.defaultReturnValue();
        }

        int value = this.forwards.getInt(key);
        if (!this.backwards.containsKey(value)) {
            return this.defaultReturnValue();
        };
        this.backwards.remove(value);
        return this.forwards.removeInt(key);
    }

    @Override
    public int hashCode() {
        return this.forwards.hashCode();
    }

    @Override
    public String toString() {
        return this.forwards.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Object2IntBiMap<?> that = (Object2IntBiMap<?>) o;
        return Objects.equals(this.forwards, that.forwards) && Objects.equals(this.backwards, that.backwards);
    }
}
