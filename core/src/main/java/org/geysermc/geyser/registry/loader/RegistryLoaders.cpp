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

package org.geysermc.geyser.registry.loader;

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.registry.type.UtilMappings"

#include "java.util.List"
#include "java.util.function.Supplier"


public final class RegistryLoaders {

    public static final NbtRegistryLoader NBT = new NbtRegistryLoader();


    public static final BiomeLoader BIOME_LOADER = new BiomeLoader();


    public static final ResourcePackLoader RESOURCE_PACKS = new ResourcePackLoader();

    public static final UtilMappings.Loader<List<Key>> UTIL_MAPPINGS_KEYS = new UtilMappings.Loader<>();


    public static <V> RegistryLoader<Object, V> empty(Supplier<V> supplier) {
        return input -> supplier.get();
    }


    public static <I, V> RegistryLoader<I, V> uninitialized() {
        return input -> null;
    }

    private RegistryLoaders() {
    }
}
