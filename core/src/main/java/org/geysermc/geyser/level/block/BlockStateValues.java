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

import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.PistonBlock;
import org.geysermc.geyser.level.physics.PistonBehavior;
import org.geysermc.geyser.registry.BlockRegistries;

/**
 * Used for block entities if the Java block state contains Bedrock block information.
 */
public final class BlockStateValues {
    public static final int NUM_FLUID_LEVELS = 9;

    /**
     * Checks if a block sticks to other blocks
     * (Slime and honey blocks)
     *
     * @param state The block state
     * @return True if the block sticks to adjacent blocks
     */
    public static boolean isBlockSticky(BlockState state) {
        Block block = state.block();
        return block == Blocks.SLIME_BLOCK || block == Blocks.HONEY_BLOCK;
    }

    /**
     * Check if two blocks are attached to each other.
     *
     * @param stateA The block state of block a
     * @param stateB The block state of block b
     * @return True if the blocks are attached to each other
     */
    public static boolean isBlockAttached(BlockState stateA, BlockState stateB) {
        boolean aSticky = isBlockSticky(stateA);
        boolean bSticky = isBlockSticky(stateB);
        if (aSticky && bSticky) {
            // Only matching sticky blocks are attached together
            // Honey + Honey & Slime + Slime
            return stateA.block() == stateB.block();
        }
        return aSticky || bSticky;
    }

    /**
     * @param state The block state of the block
     * @return true if a piston can break the block
     */
    public static boolean canPistonDestroyBlock(BlockState state)  {
        return state.block().pushReaction() == PistonBehavior.DESTROY;
    }

    public static boolean canPistonMoveBlock(BlockState state, boolean isPushing) {
        Block block = state.block();
        if (block == Blocks.AIR) {
            return true;
        }
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.RESPAWN_ANCHOR || block == Blocks.REINFORCED_DEEPSLATE) { // Hardcoded as of 1.20.5
            return false;
        }
        // Pistons can only be moved if they aren't extended
        if (block instanceof PistonBlock) {
            return !state.getValue(Properties.EXTENDED);
        }
        // Bedrock, End portal frames, etc. can't be moved
        if (block.destroyTime() == -1.0f) {
            return false;
        }
        return switch (block.pushReaction()) {
            case BLOCK, DESTROY -> false;
            case PUSH_ONLY -> isPushing; // Glazed terracotta can only be pushed
            default -> !block.hasBlockEntity(); // Pistons can't move block entities
        };
    }

    /**
     * Get the type of fluid from the block state, including waterlogged blocks.
     *
     * @param state BlockState of the block
     * @return The type of fluid
     */
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

    /**
     * Get the level of water from the block state.
     *
     * @param state BlockState of the block
     * @return The water level or -1 if the block isn't water
     */
    public static int getWaterLevel(int state) {
        BlockState blockState = BlockState.of(state);
        if (!blockState.is(Blocks.WATER)) {
            return -1;
        }
        return blockState.getValue(Properties.LEVEL);
    }

    /**
     * Get the height of water from the block state
     * This is used in FishingHookEntity to create splash sounds when the hook hits the water. In addition,
     * CollisionManager uses this to determine if the player's eyes are in water.
     *
     * @param state BlockState of the block
     * @return The water height or -1 if the block does not contain water
     */
    public static double getWaterHeight(int state) {
        int waterLevel = BlockStateValues.getWaterLevel(state);
        if (BlockRegistries.WATERLOGGED.get().get(state)) {
            waterLevel = 0;
        }
        if (waterLevel >= 0) {
            double waterHeight = 1 - (waterLevel + 1) / ((double) NUM_FLUID_LEVELS);
            // Falling water is a full block
            if (waterLevel >= 8) {
                waterHeight = 1;
            }
            return waterHeight;
        }
        return -1;
    }

    /**
     * Get the level of lava from the block state.
     *
     * @param state BlockState of the block
     * @return The lava level or -1 if the block isn't lava
     */
    public static int getLavaLevel(int state) {
        BlockState blockState = BlockState.of(state);
        if (!blockState.is(Blocks.LAVA)) {
            return -1;
        }
        return blockState.getValue(Properties.LEVEL);
    }

    /**
     * Get the height of lava from the block state
     *
     * @param state BlockState of the block
     * @return The lava height or -1 if the block does not contain lava
     */
    public static double getLavaHeight(int state) {
        int lavaLevel = BlockStateValues.getLavaLevel(state);
        if (lavaLevel >= 0) {
            double lavaHeight = 1 - (lavaLevel + 1) / ((double) NUM_FLUID_LEVELS);
            // Falling lava is a full block
            if (lavaLevel >= 8) {
                lavaHeight = 1;
            }
            return lavaHeight;
        }
        return -1;
    }

    /**
     * Get the slipperiness of a block.
     * This is used in ItemEntity to calculate the friction on an item as it slides across the ground
     *
     * @param state BlockState of the block
     * @return The block's slipperiness
     */
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
