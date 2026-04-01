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

package org.geysermc.geyser.level.block;

#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.block.type.PistonBlock"
#include "org.geysermc.geyser.level.physics.PistonBehavior"
#include "org.geysermc.geyser.registry.BlockRegistries"


public final class BlockStateValues {
    public static final int NUM_FLUID_LEVELS = 9;


    public static bool isBlockSticky(BlockState state) {
        Block block = state.block();
        return block == Blocks.SLIME_BLOCK || block == Blocks.HONEY_BLOCK;
    }


    public static bool isBlockAttached(BlockState stateA, BlockState stateB) {
        bool aSticky = isBlockSticky(stateA);
        bool bSticky = isBlockSticky(stateB);
        if (aSticky && bSticky) {


            return stateA.block() == stateB.block();
        }
        return aSticky || bSticky;
    }


    public static bool canPistonDestroyBlock(BlockState state)  {
        return state.block().pushReaction() == PistonBehavior.DESTROY;
    }

    public static bool canPistonMoveBlock(BlockState state, bool isPushing) {
        Block block = state.block();
        if (block == Blocks.AIR) {
            return true;
        }
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.RESPAWN_ANCHOR || block == Blocks.REINFORCED_DEEPSLATE) {
            return false;
        }

        if (block instanceof PistonBlock) {
            return !state.getValue(Properties.EXTENDED);
        }

        if (block.destroyTime() == -1.0f) {
            return false;
        }
        return switch (block.pushReaction()) {
            case BLOCK, DESTROY -> false;
            case PUSH_ONLY -> isPushing;
            default -> !block.hasBlockEntity();
        };
    }


    public static Fluid getFluid(int state) {
        BlockState blockState = BlockState.of(state);
        if (blockState.is(Blocks.WATER) || BlockRegistries.WATERLOGGED.get().get(state)) {
            return Fluid.WATER;
        }

        if (blockState.is(Blocks.LAVA)) {
            return Fluid.LAVA;
        }

        return Fluid.EMPTY;
    }


    public static int getWaterLevel(int state) {
        BlockState blockState = BlockState.of(state);
        if (!blockState.is(Blocks.WATER)) {
            return -1;
        }
        return blockState.getValue(Properties.LEVEL);
    }


    public static double getWaterHeight(int state) {
        int waterLevel = BlockStateValues.getWaterLevel(state);
        if (BlockRegistries.WATERLOGGED.get().get(state)) {
            waterLevel = 0;
        }
        if (waterLevel >= 0) {
            double waterHeight = 1 - (waterLevel + 1) / ((double) NUM_FLUID_LEVELS);

            if (waterLevel >= 8) {
                waterHeight = 1;
            }
            return waterHeight;
        }
        return -1;
    }


    public static int getLavaLevel(int state) {
        BlockState blockState = BlockState.of(state);
        if (!blockState.is(Blocks.LAVA)) {
            return -1;
        }
        return blockState.getValue(Properties.LEVEL);
    }


    public static double getLavaHeight(int state) {
        int lavaLevel = BlockStateValues.getLavaLevel(state);
        if (lavaLevel >= 0) {
            double lavaHeight = 1 - (lavaLevel + 1) / ((double) NUM_FLUID_LEVELS);

            if (lavaLevel >= 8) {
                lavaHeight = 1;
            }
            return lavaHeight;
        }
        return -1;
    }


    public static float getSlipperiness(BlockState state) {
        Block block = state.block();
        if (block == Blocks.SLIME_BLOCK) {
            return 0.8f;
        }
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE) {
            return 0.98f;
        }
        if (block == Blocks.BLUE_ICE) {
            return 0.989f;
        }
        return 0.6f;
    }

    private BlockStateValues() {
    }
}
