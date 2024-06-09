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

package org.geysermc.geyser.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.registry.loader.RegistryLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ListRegistry<M> extends Registry<List<M>> {
    private boolean frozen = false;

    /**
     * Creates a new instance of this class with the given input and
     * {@link RegistryLoader}. The input specified is what the registry
     * loader needs to take in.
     *
     * @param input          the input
     * @param registryLoader the registry loader
     */
    protected <I> ListRegistry(I input, RegistryLoader<I, List<M>> registryLoader) {
        super(input, registryLoader);
    }

    /**
     * Returns the value registered by the given index.
     *
     * @param index the index
     * @return the value registered by the given index.
     */
    @Nullable
    public M get(int index) {
        if (index >= this.mappings.size()) {
            return null;
        }

        return this.mappings.get(index);
    }

    /**
     * Returns the value registered by the given index or the default value
     * specified if null.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value registered by the given key or the default value
     *         specified if null.
     */
    public M getOrDefault(int index, M defaultValue) {
        M value = this.get(index);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    /**
     * Registers a new value into this registry with the given index.
     *
     * @param index the index
     * @param value the value
     * @return a new value into this registry with the given index.
     */
    public M register(int index, M value) {
        if (this.frozen) {
            throw new IllegalStateException("Registry should not be modified after frozen!");
        }
        return this.mappings.set(index, value);
    }

    /**
     * Registers a new value into this registry with the given index, even if this value would normally be outside
     * the range of a list.
     *
     * @param index the index
     * @param value the value
     * @param defaultValue the default value to fill empty spaces in the registry with.
     * @return a new value into this registry with the given index.
     */
    public M registerWithAnyIndex(int index, M value, M defaultValue) {
        if (this.frozen) {
            throw new IllegalStateException("Registry should not be modified after frozen!");
        }
        if (this.mappings.size() <= index) {
            this.mappings.addAll(Collections.nCopies(index - this.mappings.size() + 1, defaultValue));
        }
        return this.mappings.set(index, value);
    }

    /**
     * Mark this registry as unsuitable for new additions. The backing list will then be optimized for storage.
     */
    public void freeze() {
        if (!this.frozen) {
            this.frozen = true;
            if (this.mappings instanceof ArrayList<M> arrayList) {
                arrayList.trimToSize();
            }
        }
    }

    /**
     * Creates a new array registry with the given {@link RegistryLoader}. The
     * input type is not specified here, meaning the loader return type is either
     * predefined, or the registry is populated at a later point.
     *
     * @param registryLoader the registry loader
     * @param <I> the input type
     * @param <M> the returned mappings type
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, M> ListRegistry<M> create(RegistryLoader<I, List<M>> registryLoader) {
        return new ListRegistry<>(null, registryLoader);
    }

    /**
     * Creates a new integer mapped registry with the given {@link RegistryLoader} and input.
     *
     * @param registryLoader the registry loader
     * @param <I> the input
     * @param <M> the type value
     * @return a new registry with the given RegistryLoader supplier
     */
    public static <I, M> ListRegistry<M> create(I input, Supplier<RegistryLoader<I, List<M>>> registryLoader) {
        return new ListRegistry<>(input, registryLoader.get());
    }
}
