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

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

/**
 * Pistons are a special case where they are only a block entity on Bedrock.
 */
public class PistonBlockEntityTranslator {

    /**
     * Used in ChunkUtils to determine if the block is a piston.
     * @param blockState Java BlockState of block.
     * @return if block is a piston or not.
     */
    public static boolean isBlock(int blockState) {
        return BlockStateValues.getPistonValues().containsKey(blockState);
    }

    /**
     * Calculates the Nukkit CompoundTag to send to the client on chunk
     * @param blockState Java block state of block.
     * @param position Bedrock position of piston.
     * @return Bedrock tag of piston.
     */
    public static CompoundTag getTag(int blockState, Vector3i position) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", position.getX())
                .intTag("y", position.getY())
                .intTag("z", position.getZ())
                .byteTag("isMovable", (byte) 1)
                .stringTag("id", "PistonArm");
        if (BlockStateValues.getPistonValues().containsKey(blockState)) {
            boolean extended = BlockStateValues.getPistonValues().get(blockState);
            // 1f if extended, otherwise 0f
            tagBuilder.floatTag("Progress", (extended) ? 1.0f : 0.0f);
            // 1 if sticky, 0 if not
            tagBuilder.byteTag("Sticky", (byte)((BlockStateValues.isStickyPiston(blockState)) ? 1 : 0));
        }
        return tagBuilder.buildRootTag();
    }

}
