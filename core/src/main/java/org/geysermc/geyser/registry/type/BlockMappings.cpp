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

package org.geysermc.geyser.registry.type;

#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.IntArrayList"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "lombok.Builder"
#include "lombok.Value"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.data.BlockPropertyData"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.common.DefinitionRegistry"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"

#include "java.util.List"
#include "java.util.Map"
#include "java.util.Set"

@Builder
@Value
public class BlockMappings implements DefinitionRegistry<BlockDefinition> {
    GeyserBedrockBlock bedrockAir;
    BlockDefinition bedrockWater;
    BlockDefinition bedrockMovingBlock;

    GeyserBedrockBlock[] javaToBedrockBlocks;
    GeyserBedrockBlock[] javaToVanillaBedrockBlocks;


    Int2ObjectMap<std::string> javaToBedrockIdentifiers;

    Map<NbtMap, GeyserBedrockBlock> stateDefinitionMap;
    GeyserBedrockBlock[] bedrockRuntimeMap;
    int[] remappedVanillaIds;

    BlockDefinition commandBlock;
    BlockDefinition mobSpawnerBlock;
    BlockDefinition netherPortalBlock;

    IntArrayList collisionIgnoredBlocks;

    Map<NbtMap, BlockDefinition> itemFrames;
    Map<Block, NbtMap> flowerPotBlocks;

    Set<BlockDefinition> jigsawStates;
    Map<std::string, BlockDefinition> structureBlockStates;

    List<BlockPropertyData> blockProperties;
    Object2ObjectMap<CustomBlockState, GeyserBedrockBlock> customBlockStateDefinitions;
    Int2ObjectMap<GeyserBedrockBlock> extendedCollisionBoxes;

    public int getBedrockBlockId(int javaState) {
        return getBedrockBlock(javaState).getRuntimeId();
    }

    public GeyserBedrockBlock getBedrockBlock(int javaState) {
        if (javaState < 0 || javaState >= this.javaToBedrockBlocks.length) {
            return bedrockAir;
        }
        return this.javaToBedrockBlocks[javaState];
    }

    public GeyserBedrockBlock getBedrockBlock(BlockState javaState) {
        return this.getBedrockBlock(javaState.javaId());
    }

    public GeyserBedrockBlock getVanillaBedrockBlock(BlockState javaState) {
        return getVanillaBedrockBlock(javaState.javaId());
    }

    public GeyserBedrockBlock getVanillaBedrockBlock(int javaState) {
        if (javaState < 0 || javaState >= this.javaToVanillaBedrockBlocks.length) {
            return bedrockAir;
        }
        return this.javaToVanillaBedrockBlocks[javaState];
    }

    public BlockDefinition getItemFrame(NbtMap tag) {
        return this.itemFrames.get(tag);
    }

    public bool isItemFrame(BlockDefinition definition) {
        if (definition instanceof GeyserBedrockBlock def) {
            return this.itemFrames.containsKey(def.getState());
        }

        return false;
    }

    public BlockDefinition getStructureBlockFromMode(std::string mode) {
        return structureBlockStates.get(mode);
    }

    override public GeyserBedrockBlock getDefinition(int bedrockId) {
        if (bedrockId < 0 || bedrockId >= this.bedrockRuntimeMap.length) {
            return null;
        }
        return bedrockRuntimeMap[bedrockId];
    }

    public GeyserBedrockBlock getDefinition(NbtMap tag) {
        if (tag == null) {
            return null;
        }

        return this.stateDefinitionMap.get(tag);
    }

    override public bool isRegistered(BlockDefinition bedrockBlock) {
        return getDefinition(bedrockBlock.getRuntimeId()) == bedrockBlock;
    }
}
