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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public abstract class BaseSessionSharedCache<K, V> extends SessionSharedCache<K, V> {
    private final Object2ObjectOpenHashMap<K, V> baseValues = new Object2ObjectOpenHashMap<>();
    private boolean frozen = false;

    public void putBase(K key, V value) {
        if (frozen) {
            throw new IllegalStateException("Tried to register base value for " + this + " but was frozen already!");
        }
        baseValues.put(key, value);
    }

    public void freeze() {
        frozen = true;
    }

    public Map<K, V> getBase() {
        return Collections.unmodifiableMap(baseValues);
    }

    public Map<K, V> getForSession(UUID session) {
        if (!frozen) {
            throw new IllegalStateException("Tried to get session-patched map for " + this + " but base wasn't frozen yet!");
        }
        return Collections.unmodifiableMap(getForSession(session, new Object2ObjectOpenHashMap<>(baseValues)));
    }
}
