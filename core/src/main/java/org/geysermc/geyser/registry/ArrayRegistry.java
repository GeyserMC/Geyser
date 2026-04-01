/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import java.util.function.Supplier;


public class ArrayRegistry<M> extends Registry<M[]> {

    
    protected <I> ArrayRegistry(I input, RegistryLoader<I, M[]> registryLoader) {
        super(input, registryLoader);
    }

    
    @Nullable
    public M get(int index) {
        if (index >= this.mappings.length) {
            return null;
        }

        return this.mappings[index];
    }

    
    public M getOrDefault(int index, M defaultValue) {
        M value = this.get(index);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    
    public M register(int index, M value) {
        return this.mappings[index] = value;
    }

    
    public static <I, M> ArrayRegistry<M> create(Supplier<RegistryLoader<I, M[]>> registryLoader) {
        return new ArrayRegistry<>(null, registryLoader.get());
    }

    
    public static <I, M> ArrayRegistry<M> create(I input, Supplier<RegistryLoader<I, M[]>> registryLoader) {
        return new ArrayRegistry<>(input, registryLoader.get());
    }

    
    public static <I, M> ArrayRegistry<M> create(RegistryLoader<I, M[]> registryLoader) {
        return new ArrayRegistry<>(null, registryLoader);
    }

    
    public static <I, M> ArrayRegistry<M> create(I input, RegistryLoader<I, M[]> registryLoader) {
        return new ArrayRegistry<>(input, registryLoader);
    }
}
