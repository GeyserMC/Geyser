/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.block.DoubleChestValue;
import org.geysermc.connector.utils.BlockEntityUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Chests have more block entity properties in Bedrock, which is solved by implementing the BedrockOnlyBlockEntity
 */
@BlockEntity(name = "Chest", regex = "chest")
public class DoubleChestBlockEntityTranslator extends BlockEntityTranslator implements BedrockOnlyBlockEntity, RequiresBlockState {

    @Override
    public boolean isBlock(int blockState) {
        return BlockStateValues.getDoubleChestValues().containsKey(blockState);
    }

    @Override
    public void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        CompoundTag javaTag = getConstantJavaTag("chest", position.getX(), position.getY(), position.getZ());
        NbtMapBuilder tagBuilder = getConstantBedrockTag(BlockEntityUtils.getBedrockBlockEntityId("chest"), position.getX(), position.getY(), position.getZ()).toBuilder();
        translateTag(javaTag, blockState).forEach(tagBuilder::put);
        BlockEntityUtils.updateBlockEntity(session, tagBuilder.build(), position);
    }

    @Override
    public Map<String, Object> translateTag(CompoundTag tag, int blockState) {
        Map<String, Object> tags = new HashMap<>();
        if (BlockStateValues.getDoubleChestValues().containsKey(blockState)) {
            DoubleChestValue chestValues = BlockStateValues.getDoubleChestValues().get(blockState);
            if (chestValues != null) {
                int x = (int) tag.getValue().get("x").getValue();
                int z = (int) tag.getValue().get("z").getValue();
                // Calculate the position of the other chest based on the Java block state
                if (chestValues.isFacingEast) {
                    if (chestValues.isDirectionPositive) {
                        // East
                        z = z + (chestValues.isLeft ? 1 : -1);
                    } else {
                        // West
                        z = z + (chestValues.isLeft ? -1 : 1);
                    }
                } else {
                    if (chestValues.isDirectionPositive) {
                        // South
                        x = x + (chestValues.isLeft ? -1 : 1);
                    } else {
                        // North
                        x = x + (chestValues.isLeft ? 1 : -1);
                    }
                }
                tags.put("pairx", x);
                tags.put("pairz", z);
                if (!chestValues.isLeft) {
                    tags.put("pairlead", (byte) 1);
                }
            }
        }
        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        return null;
    }

    @Override
    public NbtMap getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        return null;
    }
}
