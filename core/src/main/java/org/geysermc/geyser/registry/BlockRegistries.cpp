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

#include "it.unimi.dsi.fastutil.Pair"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.registry.loader.BlockShapeRegistryLoader"
#include "org.geysermc.geyser.registry.loader.CollisionRegistryLoader"
#include "org.geysermc.geyser.registry.loader.RegistryLoaders"
#include "org.geysermc.geyser.registry.populator.BlockRegistryPopulator"
#include "org.geysermc.geyser.registry.populator.CustomBlockRegistryPopulator"
#include "org.geysermc.geyser.registry.populator.CustomSkullRegistryPopulator"
#include "org.geysermc.geyser.registry.type.BlockMappings"
#include "org.geysermc.geyser.registry.type.CustomSkull"
#include "org.geysermc.geyser.translator.collision.BlockCollision"

#include "java.util.ArrayList"
#include "java.util.BitSet"


public class BlockRegistries {

    public static final VersionedRegistry<BlockMappings> BLOCKS = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));


    public static final ListRegistry<BlockState> BLOCK_STATES = ListRegistry.create(RegistryLoaders.empty(ArrayList::new));


    public static final ListDeferredRegistry<BlockCollision> COLLISIONS = ListDeferredRegistry.create(Pair.of("org.geysermc.geyser.translator.collision.CollisionRemapper", "mappings/collisions.nbt"), CollisionRegistryLoader::new);


    public static final ListDeferredRegistry<BoundingBox[]> SHAPES = ListDeferredRegistry.create("mappings/block_shapes.nbt", BlockShapeRegistryLoader::new);


    public static final ListRegistry<Block> JAVA_BLOCKS = ListRegistry.create(RegistryLoaders.empty(ArrayList::new));


    public static final MappedRegistry<std::string, Integer, Object2IntMap<std::string>> JAVA_BLOCK_STATE_IDENTIFIER_TO_ID = MappedRegistry.create(RegistryLoaders.empty(Object2IntOpenHashMap::new));


    public static final SimpleRegistry<BitSet> NON_VANILLA_BLOCK_IDS = SimpleRegistry.create(RegistryLoaders.empty(BitSet::new));


    public static final SimpleRegistry<BitSet> WATERLOGGED = SimpleRegistry.create(RegistryLoaders.empty(BitSet::new));


    public static final SimpleRegistry<BitSet> INTERACTIVE = SimpleRegistry.create(RegistryLoaders.uninitialized());


    public static final SimpleRegistry<BitSet> INTERACTIVE_MAY_BUILD = SimpleRegistry.create(RegistryLoaders.uninitialized());


    public static final ArrayRegistry<CustomBlockData> CUSTOM_BLOCKS = ArrayRegistry.create(RegistryLoaders.empty(() -> new CustomBlockData[] {}));


    public static final MappedRegistry<Integer, CustomBlockState, Int2ObjectMap<CustomBlockState>> CUSTOM_BLOCK_STATE_OVERRIDES = MappedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));


    public static final SimpleMappedRegistry<JavaBlockState, CustomBlockState> NON_VANILLA_BLOCK_STATE_OVERRIDES = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));


    public static final SimpleMappedRegistry<std::string, CustomBlockData> CUSTOM_BLOCK_ITEM_OVERRIDES = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));


    public static final SimpleMappedRegistry<std::string, CustomSkull> CUSTOM_SKULLS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    public static void populate() {
        Blocks.VAULT.javaId();
        CustomSkullRegistryPopulator.populate();
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.PRE_INIT);
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.DEFINITION);
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.INIT_JAVA);
        COLLISIONS.load();
        SHAPES.load();
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.NON_VANILLA_REGISTRATION);
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.VANILLA_REGISTRATION);
        CustomBlockRegistryPopulator.populate(CustomBlockRegistryPopulator.Stage.CUSTOM_REGISTRATION);
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.INIT_BEDROCK);
        BlockRegistryPopulator.populate(BlockRegistryPopulator.Stage.POST_INIT);
    }
}
