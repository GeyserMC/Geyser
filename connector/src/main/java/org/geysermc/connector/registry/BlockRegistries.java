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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.registry.loader.RegistryLoaders;
import org.geysermc.connector.registry.populator.BlockRegistryPopulator;
import org.geysermc.connector.registry.type.BlockMapping;
import org.geysermc.connector.registry.type.BlockMappings;
import org.geysermc.connector.utils.Object2IntBiMap;

public class BlockRegistries {
    public static final VersionedRegistry<BlockMappings> BLOCKS = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    public static final SimpleMappedRegistry<String, String> JAVA_TO_BEDROCK_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    public static final SimpleMappedRegistry<Integer, BlockMapping> JAVA_BLOCKS = SimpleMappedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    public static final MappedRegistry<String, Integer, Object2IntBiMap<String>> JAVA_IDENTIFIERS = MappedRegistry.create(RegistryLoaders.empty(Object2IntBiMap::new));

    public static final SimpleMappedRegistry<Integer, String> JAVA_CLEAN_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    public static final SimpleRegistry<IntSet> WATERLOGGED = SimpleRegistry.create(RegistryLoaders.empty(IntOpenHashSet::new));

    static {
        BlockRegistryPopulator.populate();
    }

    public static void init() {
        // no-op
    }
}