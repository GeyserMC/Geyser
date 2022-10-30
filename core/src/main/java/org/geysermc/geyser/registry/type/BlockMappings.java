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

import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Builder;
import lombok.Value;
import org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition;

import java.util.Map;
import java.util.Set;

@Builder
@Value
public class BlockMappings {
    BlockDefinition bedrockAir;
    BlockDefinition bedrockWater;
    BlockDefinition bedrockMovingBlock;

    int blockStateVersion;

    BlockDefinition[] javaToBedrockBlocks;

    NbtList<NbtMap> bedrockBlockPalette;

    BlockDefinition commandBlock;

    Map<NbtMap, BlockDefinition> itemFrames;
    Map<String, NbtMap> flowerPotBlocks;

    Set<BlockDefinition> jigsawStates;

    public int getBedrockBlockId(int state) {
        if (state >= this.javaToBedrockBlocks.length) {
            return bedrockAir.getRuntimeId();
        }
        return this.javaToBedrockBlocks[state].getRuntimeId();
    }

    public BlockDefinition getBedrockBlock(int state) {
        if (state >= this.javaToBedrockBlocks.length) {
            return bedrockAir;
        }
        return this.javaToBedrockBlocks[state];
    }

    public int getItemFrame(NbtMap tag) {
        BlockDefinition definition = this.itemFrames.get(tag);
        return definition == null ? -1 : definition.getRuntimeId();
    }

    public boolean isItemFrame(int bedrockBlockRuntimeId) {
        for (Map.Entry<NbtMap, BlockDefinition> entry : this.itemFrames.entrySet()) {
            if (entry.getValue().getRuntimeId() == bedrockBlockRuntimeId) {
                return true;
            }
        }

        return false;
    }
}