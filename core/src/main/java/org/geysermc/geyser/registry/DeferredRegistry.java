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

import org.geysermc.geyser.registry.loader.RegistryLoader;
import org.geysermc.geyser.registry.loader.RegistryLoaders;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A deferred registry is a registry that is not loaded until it is needed.
 * This is useful for registries that are not needed until after other parts
 * of the lifecycle have been completed.
 * <p>
 * This class is slightly different from other registries in that it acts as
 * a wrapper around another registry. This is to allow for any kind of registry
 * type to be deferred.
 *
 * @param <M> the value being held by the registry
 */
public final class DeferredRegistry<M> implements IRegistry<M> {
    private final Registry<M> backingRegistry;
    private final Supplier<M> loader;

    private boolean loaded;

    private <I> DeferredRegistry(Function<RegistryLoader<I, M>, Registry<M>> registryLoader, RegistryLoader<I, M> deferredLoader) {
        this.backingRegistry = registryLoader.apply(RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.load(null);
    }

    private <I> DeferredRegistry(Function<RegistryLoader<I, M>, Registry<M>> registryLoader, Supplier<RegistryLoader<I, M>> deferredLoader) {
        this.backingRegistry = registryLoader.apply(RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.get().load(null);
    }

    private <I> DeferredRegistry(I input, RegistryInitializer<M> registryInitializer, RegistryLoader<I, M> deferredLoader) {
        this.backingRegistry = registryInitializer.initialize(input, RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.load(input);
    }

    private <I> DeferredRegistry(I input, RegistryInitializer<M> registryInitializer, Supplier<RegistryLoader<I, M>> deferredLoader) {
        this.backingRegistry = registryInitializer.initialize(input, RegistryLoaders.uninitialized());
        this.loader = () -> deferredLoader.get().load(input);
    }

    /**
     * Gets the underlying value held by this registry.
     *
     * @return the underlying value held by this registry
     * @throws IllegalStateException if this deferred registry has not been loaded yet
     */
    @Override
    public M get() {
        if (!this.loaded) {
            throw new IllegalStateException("Registry has not been loaded yet!");
        }

        return this.backingRegistry.get();
    }

    @Override
    public void set(M mappings) {
        this.backingRegistry.set(mappings);
    }

    /**
     * Registers what is specified in the given {@link Consumer} into the underlying value.
     *
     * @param consumer the consumer
     * @throws IllegalStateException if this deferred registry has not been loaded yet
     */
    @Override
    public void register(Consumer<M> consumer) {
        if (!this.loaded) {
            throw new IllegalStateException("Registry has not been loaded yet!");
        }

        this.backingRegistry.register(consumer);
    }

    /**
     * Loads the registry.
     */
    public void load() {
        this.backingRegistry.set(this.loader.get());
        this.loaded = true;
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @param <M> the registry type
     * @return the new deferred registry
     */
    public static <I, M> DeferredRegistry<M> create(Function<RegistryLoader<I, M>, Registry<M>> registryLoader, RegistryLoader<I, M> deferredLoader) {
        return new DeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryLoader the registry loader
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @param <M> the registry type
     * @return the new deferred registry
     */
    public static <I, M> DeferredRegistry<M> create(Function<RegistryLoader<I, M>, Registry<M>> registryLoader, Supplier<RegistryLoader<I, M>> deferredLoader) {
        return new DeferredRegistry<>(registryLoader, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @param <M> the registry type
     * @return the new deferred registry
     */
    public static <I, M> DeferredRegistry<M> create(I input, RegistryInitializer<M> registryInitializer, RegistryLoader<I, M> deferredLoader) {
        return new DeferredRegistry<>(input, registryInitializer, deferredLoader);
    }

    /**
     * Creates a new deferred registry.
     *
     * @param registryInitializer the registry initializer
     * @param deferredLoader the deferred loader
     * @param <I> the input type
     * @param <M> the registry type
     * @return the new deferred registry
     */
    public static <I, M> DeferredRegistry<M> create(I input, RegistryInitializer<M> registryInitializer, Supplier<RegistryLoader<I, M>> deferredLoader) {
        return new DeferredRegistry<>(input, registryInitializer, deferredLoader);
    }

    /**
     * A registry initializer.
     *
     * @param <M> the registry type
     */
    interface RegistryInitializer<M> {

        /**
         * Initializes the registry.
         *
         * @param input the input
         * @param registryLoader the registry loader
         * @param <I> the input type
         * @return the initialized registry
         */
        <I> Registry<M> initialize(I input, RegistryLoader<I, M> registryLoader);
    }
}
