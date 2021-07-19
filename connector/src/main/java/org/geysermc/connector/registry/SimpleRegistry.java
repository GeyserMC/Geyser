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

package org.geysermc.connector.registry;

import org.geysermc.connector.registry.loader.RegistryLoader;

import java.util.function.Supplier;

public class SimpleRegistry<M> extends Registry<M> {
    private <I> SimpleRegistry(I input, RegistryLoader<I, M> registryLoader) {
        super(input, registryLoader);
    }

    public static <I, M> SimpleRegistry<M> create(Supplier<RegistryLoader<I, M>> registryLoader) {
        return new SimpleRegistry<>(null, registryLoader.get());
    }

    public static <I, M> SimpleRegistry<M> create(I input, Supplier<RegistryLoader<I, M>> registryLoader) {
        return new SimpleRegistry<>(input, registryLoader.get());
    }

    public static <I, M> SimpleRegistry<M> create(RegistryLoader<I, M> registryLoader) {
        return new SimpleRegistry<>(null, registryLoader);
    }

    public static <I, M> SimpleRegistry<M> create(I input, RegistryLoader<I, M> registryLoader) {
        return new SimpleRegistry<>(input, registryLoader);
    }
}