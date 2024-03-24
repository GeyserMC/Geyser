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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.Builder;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.geysermc.geyser.api.block.custom.CustomBlockState;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Builder
@Value
public class BlockMappings implements DefinitionRegistry<GeyserBedrockBlock> {
    GeyserBedrockBlock bedrockAir;
    BlockDefinition bedrockWater;
    BlockDefinition bedrockMovingBlock;

    GeyserBedrockBlock[] javaToBedrockBlocks;
    GeyserBedrockBlock[] javaToVanillaBedrockBlocks;

    Map<NbtMap, GeyserBedrockBlock> stateDefinitionMap;
    GeyserBedrockBlock[] bedrockRuntimeMap;
    int[] remappedVanillaIds;

    BlockDefinition commandBlock;
    BlockDefinition mobSpawnerBlock;

    Map<NbtMap, BlockDefinition> itemFrames;
    Map<String, NbtMap> flowerPotBlocks;

    Set<BlockDefinition> jigsawStates;

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

    public GeyserBedrockBlock getVanillaBedrockBlock(int javaState) {
        if (javaState < 0 || javaState >= this.javaToVanillaBedrockBlocks.length) {
            return bedrockAir;
        }
        return this.javaToVanillaBedrockBlocks[javaState];
    }

    public BlockDefinition getItemFrame(NbtMap tag) {
        return this.itemFrames.get(tag);
    }

    public boolean isItemFrame(BlockDefinition definition) {
        if (definition instanceof GeyserBedrockBlock def) {
            return this.itemFrames.containsKey(def.getState());
        }

        return false;
    }

    @Override
    public @Nullable GeyserBedrockBlock getDefinition(int bedrockId) {
        if (bedrockId < 0 || bedrockId >= this.bedrockRuntimeMap.length) {
            return null;
        }
        return bedrockRuntimeMap[bedrockId];
    }

    public @Nullable GeyserBedrockBlock getDefinition(NbtMap tag) {
        if (tag == null) {
            return null;
        }

        return this.stateDefinitionMap.get(tag);
    }

    @Override
    public boolean isRegistered(GeyserBedrockBlock bedrockBlock) {
        return getDefinition(bedrockBlock.getRuntimeId()) == bedrockBlock;
    }
}