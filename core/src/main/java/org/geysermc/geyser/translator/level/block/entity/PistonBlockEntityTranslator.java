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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Pistons are a special case where they are only a block entity on Bedrock.
 */
@BlockEntity(type = "PistonArm")
public class PistonBlockEntityTranslator extends BlockEntityTranslator {
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

    @Override
    public NbtMap getBlockEntityTag(GeyserSession session, String type, int x, int y, int z, CompoundTag tag, int blockState) {
        Direction direction = BlockStateValues.getPistonHeadOrientation(blockState);
        Vector3i position = Vector3i.from(x, y, z).sub(direction.getUnitVector()); // Piston arm is one block away from the head
        NbtMapBuilder tagBuilder = getConstantBedrockTag(type, position.getX(), position.getY(), position.getZ());
        translateTag(session, tagBuilder, tag, blockState);
        return tagBuilder.build();
    }

    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder builder, CompoundTag tag, int blockState) {
        builder.putBoolean("Sticky", BlockStateValues.isStickyPistonHead(blockState))
            .putFloat("Progress", 1.0f) // We wouldn't be here if this wasn't extended
            .putFloat("LastProgress", 1.0f)
            .putByte("NewState", (byte) 2)
            .putByte("State", (byte) 2)
            .putBoolean("isMovable", false);
    }
}
