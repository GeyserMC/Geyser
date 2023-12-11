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

package org.geysermc.geyser.translator.level.block.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Implemented only if a block is a block entity in Bedrock and not Java Edition.
 */
public interface BedrockOnlyBlockEntity extends RequiresBlockState {
    /**
     * Determines if block is part of class
     * @param blockState BlockState to be compared
     * @return true if part of the class
     */
    boolean isBlock(int blockState);

    /**
     * Update the block on Bedrock Edition.
     * @param session GeyserConnection.
     * @param blockState The Java block state.
     * @param position The Bedrock block position.
     */
    void updateBlock(GeyserSession session, int blockState, Vector3i position);

    /**
     * Get the tag of the Bedrock-only block entity
     * @param position Bedrock position of block.
     * @param blockState Java BlockState of block.
     * @return Bedrock tag, or null if not a Bedrock-only Block Entity
     */
    static @Nullable NbtMap getTag(GeyserSession session, Vector3i position, int blockState) {
        if (FlowerPotBlockEntityTranslator.isFlowerBlock(blockState)) {
            return FlowerPotBlockEntityTranslator.getTag(session, blockState, position);
        } else if (PistonBlockEntityTranslator.isBlock(blockState)) {
            return PistonBlockEntityTranslator.getTag(blockState, position);
        } else if (BlockStateValues.isNonWaterCauldron(blockState)) {
            // As of 1.18.30: this is required to make rendering not look weird on chunk load (lava and snow cauldrons look dim)
            return NbtMap.builder()
                    .putString("id", "Cauldron")
                    .putByte("isMovable", (byte) 0)
                    .putShort("PotionId", (short) -1)
                    .putShort("PotionType", (short) -1)
                    .putList("Items", NbtType.END, NbtList.EMPTY)
                    .putInt("x", position.getX())
                    .putInt("y", position.getY())
                    .putInt("z", position.getZ())
                    .build();
        }
        return null;
    }
}
