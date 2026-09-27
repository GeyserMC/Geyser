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

package org.geysermc.geyser.registry.java;

import net.kyori.adventure.key.Key;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Extends {@link JavaRegistryLookup} with methods to iterate over a registry's contents.
 *
 * <p>This interface extends {@link Iterable}, making it usable in {@code for}-loops and with {@link Iterable#forEach(Consumer)}.
 * The iterator provided by instances is sorted by network ID.</p>
 *
 * @param <T> the type of this registry
 * @see JavaRegistryLookup
 * @see MutableJavaRegistry
 * @see JavaRegistries
 * @see BuiltInJavaRegistries
 * @see JavaRegistryProvider
 */
public interface JavaRegistry<T> extends JavaRegistryLookup<T>, Iterable<T> {

    /**
     * Returns a collection of all keys in this registry.
     *
     * <p>Note that this collection may not necessarily be sorted by network ID.</p>
     *
     * @return a collection of all keys in this registry
     */
    Collection<Key> keys();

    /**
     * Returns a collection of all values in this registry.
     *
     * <p>Note that this collection may not necessarily be sorted by network ID.</p>
     *
     * @return a collection of all values in this registry
     */
    Collection<T> values();

    /**
     * @return the amount of entries in this registry
     */
    int size();

    /**
     * Iterates {@code action} for each {@link RegistryEntryData} of this registry.
     *
     * <p>The action is executed in order of network ID.</p>
     *
     * @param action the action to execute for each {@link RegistryEntryData}
     */
    void forEachEntry(Consumer<RegistryEntryData<T>> action);
}
