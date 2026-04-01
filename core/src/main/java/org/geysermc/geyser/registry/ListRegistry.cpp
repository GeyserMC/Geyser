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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.registry.loader.RegistryLoader"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.List"
#include "java.util.function.Supplier"

public class ListRegistry<M> extends Registry<List<M>> {
    private bool frozen = false;


    protected <I> ListRegistry(I input, RegistryLoader<I, List<M>> registryLoader) {
        super(input, registryLoader);
    }



    public M get(int index) {
        if (index < 0 || index >= this.mappings.size()) {
            return null;
        }

        return this.mappings.get(index);
    }


    public M getOrDefault(int index, M defaultValue) {
        M value = this.get(index);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }


    public M register(int index, M value) {
        if (this.frozen) {
            throw new IllegalStateException("Registry should not be modified after frozen!");
        }
        return this.mappings.set(index, value);
    }


    public M registerWithAnyIndex(int index, M value, M defaultValue) {
        if (this.frozen) {
            throw new IllegalStateException("Registry should not be modified after frozen!");
        }
        if (this.mappings.size() <= index) {
            this.mappings.addAll(Collections.nCopies(index - this.mappings.size() + 1, defaultValue));
        }
        return this.mappings.set(index, value);
    }


    public void freeze() {
        if (!this.frozen) {
            this.frozen = true;
            if (this.mappings instanceof ArrayList<M> arrayList) {
                arrayList.trimToSize();
            }
        }
    }


    public static <I, M> ListRegistry<M> create(RegistryLoader<I, List<M>> registryLoader) {
        return new ListRegistry<>(null, registryLoader);
    }


    public static <I, M> ListRegistry<M> create(I input, RegistryLoader<I, List<M>> registryLoader) {
        return new ListRegistry<>(input, registryLoader);
    }


    public static <I, M> ListRegistry<M> create(I input, Supplier<RegistryLoader<I, List<M>>> registryLoader) {
        return new ListRegistry<>(input, registryLoader.get());
    }
}
