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

package org.geysermc.geyser.registry;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.registry.loader.RegistryLoaders;
import org.geysermc.geyser.registry.populator.BlockRegistryPopulator;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.util.collection.Object2IntBiMap;

/**
 * Holds all the block registries in Geyser.
 */
public class BlockRegistries {
    /**
     * A versioned registry which holds {@link BlockMappings} for each version. These block mappings contain
     * primarily Bedrock version-specific data.
     */
    public static final VersionedRegistry<BlockMappings> BLOCKS = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    /**
     * A mapped registry which stores Java to Bedrock block identifiers.
     */
    public static final SimpleMappedRegistry<String, String> JAVA_TO_BEDROCK_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    /**
     * A registry which stores Java IDs to {@link BlockMapping}, containing miscellaneous information about
     * blocks and their behavior in many cases.
     */
    public static final ArrayRegistry<BlockMapping> JAVA_BLOCKS = ArrayRegistry.create(RegistryLoaders.empty(() -> new BlockMapping[] {}));

    /**
     * A (bi)mapped registry containing the Java IDs to identifiers.
     */
    public static final MappedRegistry<String, Integer, Object2IntBiMap<String>> JAVA_IDENTIFIERS = MappedRegistry.create(RegistryLoaders.empty(Object2IntBiMap::new));

    /**
     * A registry containing all the waterlogged blockstates.
     */
    public static final SimpleRegistry<IntSet> WATERLOGGED = SimpleRegistry.create(RegistryLoaders.empty(IntOpenHashSet::new));

    static {
        BlockRegistryPopulator.populate();
    }

    public static void init() {
        // no-op
    }
}