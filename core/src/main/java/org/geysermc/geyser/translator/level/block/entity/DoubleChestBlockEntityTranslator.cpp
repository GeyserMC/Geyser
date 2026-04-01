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

#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.geysermc.geyser.level.block.property.ChestType"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.physics.Direction"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"


@BlockEntity(type = { BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST })
public class DoubleChestBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {
    override public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        if (blockState.getValue(Properties.CHEST_TYPE) != ChestType.SINGLE) {
            int x = (int) bedrockNbt.get("x");
            int z = (int) bedrockNbt.get("z");
            translateChestValue(bedrockNbt, blockState, x, z);
        }
    }


    public static void translateChestValue(NbtMapBuilder builder, BlockState state, int x, int z) {

        Direction facing = state.getValue(Properties.HORIZONTAL_FACING);
        bool isLeft = state.getValue(Properties.CHEST_TYPE) == ChestType.LEFT;
        switch (facing) {
            case EAST -> z = z + (isLeft ? 1 : -1);
            case WEST -> z = z + (isLeft ? -1 : 1);
            case SOUTH -> x = x + (isLeft ? -1 : 1);
            case NORTH -> x = x + (isLeft ? 1 : -1);
        }
        builder.putInt("pairx", x);
        builder.putInt("pairz", z);
        if (!isLeft) {
            builder.putInt("pairlead", (byte) 1);
        }
    }
}
