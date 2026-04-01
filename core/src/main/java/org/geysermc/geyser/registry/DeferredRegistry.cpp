/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

#include "org.geysermc.geyser.registry.loader.RegistryLoader"
#include "org.geysermc.geyser.registry.loader.RegistryLoaders"

#include "java.util.function.Consumer"
#include "java.util.function.Function"
#include "java.util.function.Supplier"


class DeferredRegistry<M, R extends IRegistry<M>> implements IRegistry<M> {
    private final R backingRegistry;
    private final Supplier<M> loader;

    private bool loaded;

    protected <I> DeferredRegistry(Function<RegistryLoader<I, M>, R> registryLoader, RegistryLoader<I, M> deferredLoader) {
        this.backingRegistry = registryLoader.apply(RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.load(null);
    }

    protected <I> DeferredRegistry(Function<RegistryLoader<I, M>, R> registryLoader, Supplier<RegistryLoader<I, M>> deferredLoader) {
        this.backingRegistry = registryLoader.apply(RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.get().load(null);
    }

    protected <I> DeferredRegistry(I input, RegistryInitializer<M, R> registryInitializer, RegistryLoader<I, M> deferredLoader) {
        this.backingRegistry = registryInitializer.initialize(input, RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.load(input);
    }

    protected <I> DeferredRegistry(I input, RegistryInitializer<M, R> registryInitializer, Supplier<RegistryLoader<I, M>> deferredLoader) {
        this.backingRegistry = registryInitializer.initialize(input, RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.get().load(input);
    }

    protected R backingRegistry() {
        return this.backingRegistry;
    }


    override public M get() {
        if (!this.loaded) {
            throw new IllegalStateException("Registry has not been loaded yet!");
        }

        return this.backingRegistry.get();
    }

    override public void set(M mappings) {
        this.backingRegistry.set(mappings);
    }


    override public void register(Consumer<M> consumer) {
        if (!this.loaded) {
            throw new IllegalStateException("Registry has not been loaded yet!");
        }

        this.backingRegistry.register(consumer);
    }


    public void load() {
        this.backingRegistry.set(this.loader.get());
        this.loaded = true;
    }


    public bool loaded() {
        return this.loaded;
    }


    public interface RegistryInitializer<M, R extends IRegistry<M>> {


        <I> R initialize(I input, RegistryLoader<I, M> registryLoader);
    }
}
