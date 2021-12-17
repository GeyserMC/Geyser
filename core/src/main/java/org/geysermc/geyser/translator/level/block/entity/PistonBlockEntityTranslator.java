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

package org.geysermc.geyser.translator.level.block.entity;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import org.geysermc.geyser.level.block.BlockStateValues;

/**
 * Pistons are a special case where they are only a block entity on Bedrock.
 */
public class PistonBlockEntityTranslator {
    /**
     * Used in ChunkUtils to determine if the block is a piston.
     *
     * @param blockState Java BlockState of block.
     * @return if block is a piston or not.
     */
    public static boolean isBlock(int blockState) {
        return BlockStateValues.getPistonValues().containsKey(blockState);
    }

    /**
     * Calculates the Nukkit CompoundTag to send to the client on chunk
     *
     * @param blockState Java block state of block.
     * @param position   Bedrock position of piston.
     * @return Bedrock tag of piston.
     */
    public static NbtMap getTag(int blockState, Vector3i position) {
        boolean extended = BlockStateValues.getPistonValues().get(blockState);
        boolean sticky = BlockStateValues.isStickyPiston(blockState);
        return PistonBlockEntity.buildStaticPistonTag(position, extended, sticky);
    }
}
