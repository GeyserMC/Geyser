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

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.registry.loader.CollisionRegistryLoader;
import org.geysermc.geyser.registry.loader.RegistryLoaders;
import org.geysermc.geyser.registry.populator.BlockRegistryPopulator;
import org.geysermc.geyser.registry.populator.CustomBlockRegistryPopulator;
import org.geysermc.geyser.registry.populator.CustomSkullRegistryPopulator;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.translator.collision.BlockCollision;

import java.util.BitSet;

import java.util.Set;

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
    public static final ArrayRegistry<BlockMapping> JAVA_BLOCKS = ArrayRegistry.create(RegistryLoaders.uninitialized());

    /**
     * A mapped registry containing which holds block IDs to its {@link BlockCollision}.
     */
    public static final IntMappedRegistry<BlockCollision> COLLISIONS;

    /**
     * A mapped registry containing the Java identifiers to IDs.
     */
    public static final MappedRegistry<String, Integer, Object2IntMap<String>> JAVA_IDENTIFIER_TO_ID = MappedRegistry.create(RegistryLoaders.empty(Object2IntOpenHashMap::new));

    /**
     * A registry which stores unique Java IDs to its clean identifier
     * This is used in the statistics form.
     */
    public static final ArrayRegistry<String> CLEAN_JAVA_IDENTIFIERS = ArrayRegistry.create(RegistryLoaders.uninitialized());

    /**
     * A registry containing all the waterlogged blockstates.
     */
    public static final SimpleRegistry<BitSet> WATERLOGGED = SimpleRegistry.create(RegistryLoaders.empty(BitSet::new));

    /**
     * A registry containing all blockstates which are always interactive.
     */
    public static final SimpleRegistry<BitSet> INTERACTIVE = SimpleRegistry.create(RegistryLoaders.uninitialized());

    /**
     * A registry containing all blockstates which are interactive if the player has the may build permission.
     */
    public static final SimpleRegistry<BitSet> INTERACTIVE_MAY_BUILD = SimpleRegistry.create(RegistryLoaders.uninitialized());

    /**
     * A registry containing all the custom blocks.
     */
    public static final ArrayRegistry<CustomBlockData> CUSTOM_BLOCKS = ArrayRegistry.create(RegistryLoaders.empty(() -> new CustomBlockData[] {}));

    /**
     * A registry which stores Java Ids and the custom block state it should be replaced with.
     */
    public static final MappedRegistry<Integer, CustomBlockState, Int2ObjectMap<CustomBlockState>> CUSTOM_BLOCK_STATE_OVERRIDES = MappedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    /**
     * A registry which stores non vanilla java blockstates and the custom block state it should be replaced with.
     */
    public static final SimpleMappedRegistry<JavaBlockState, CustomBlockState> NON_VANILLA_BLOCK_STATE_OVERRIDES = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    /**
     * A registry which stores clean Java Ids and the custom block it should be replaced with in the context of items.
     */
    public static final SimpleMappedRegistry<String, CustomBlockData> CUSTOM_BLOCK_ITEM_OVERRIDES = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    /**
     * A registry which stores Custom Block Data for extended collision boxes and the Java IDs of blocks that will have said extended collision boxes placed above them.
     */
    public static final SimpleMappedRegistry<CustomBlockData, Set<Integer>> EXTENDED_COLLISION_BOXES = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    /**
     * A registry which stores skin texture hashes to custom skull blocks.
     */
    public static final SimpleMappedRegistry<String, CustomSkull> CUSTOM_SKULLS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    static {
        CustomSkullRegistryPopulator.populate();
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.PRE_INIT);
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.DEFINITION);
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.NON_VANILLA_REGISTRATION);
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.INIT_JAVA);
        COLLISIONS = IntMappedRegistry.create(Pair.of("org.geysermc.geyser.translator.collision.CollisionRemapper", "mappings/collision.json"), CollisionRegistryLoader::new);
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.VANILLA_REGISTRATION);
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.CUSTOM_REGISTRATION);
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.INIT_BEDROCK);
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.POST_INIT);
    }

    public static void init() {
        // no-op
    }

}