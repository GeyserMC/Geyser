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

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.PistonBlock;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.level.physics.PistonBehavior;
import org.geysermc.geyser.registry.BlockRegistries;

import java.util.Locale;

/**
 * Used for block entities if the Java block state contains Bedrock block information.
 */
public final class BlockStateValues {
    private static final IntSet HORIZONTAL_FACING_JIGSAWS = new IntOpenHashSet();
    private static final IntSet STICKY_PISTONS = new IntOpenHashSet();
    private static final Object2IntMap<Direction> PISTON_HEADS = new Object2IntOpenHashMap<>();
    private static final Int2ObjectMap<Direction> PISTON_ORIENTATION = new Int2ObjectOpenHashMap<>();
    private static final IntSet ALL_PISTON_HEADS = new IntOpenHashSet();
    private static final Int2IntMap WATER_LEVEL = new Int2IntOpenHashMap();
    private static final Int2IntMap LAVA_LEVEL = new Int2IntOpenHashMap();

    public static int JAVA_WATER_ID;

    public static final int NUM_FLUID_LEVELS = 9;

    /**
     * Determines if the block state contains Bedrock block information
     *
     * @param javaId         The Java Identifier of the block
     * @param javaBlockState the Java Block State of the block
     */
    public static void storeBlockStateValues(String javaId, int javaBlockState) {
        if (javaId.contains("piston[")) { // minecraft:moving_piston, minecraft:sticky_piston, minecraft:piston
            if (javaId.contains("sticky")) {
                STICKY_PISTONS.add(javaBlockState);
            }
            PISTON_ORIENTATION.put(javaBlockState, getBlockDirection(javaId));
            return;
        } else if (javaId.startsWith("minecraft:piston_head")) {
            ALL_PISTON_HEADS.add(javaBlockState);
            if (javaId.contains("short=false")) {
                PISTON_HEADS.put(getBlockDirection(javaId), javaBlockState);
            }
            return;
        }

        if (javaId.startsWith("minecraft:water") && !javaId.contains("cauldron")) {
            String strLevel = javaId.substring(javaId.lastIndexOf("level=") + 6, javaId.length() - 1);
            int level = Integer.parseInt(strLevel);
            WATER_LEVEL.put(javaBlockState, level);
            return;
        }

        if (javaId.startsWith("minecraft:lava") && !javaId.contains("cauldron")) {
            String strLevel = javaId.substring(javaId.lastIndexOf("level=") + 6, javaId.length() - 1);
            int level = Integer.parseInt(strLevel);
            LAVA_LEVEL.put(javaBlockState, level);
            return;
        }

        if (javaId.startsWith("minecraft:jigsaw[orientation=")) {
            String blockStateData = javaId.substring(javaId.indexOf("orientation=") + "orientation=".length(), javaId.lastIndexOf('_'));
            Direction direction = Direction.valueOf(blockStateData.toUpperCase(Locale.ROOT));
            if (direction.isHorizontal()) {
                HORIZONTAL_FACING_JIGSAWS.add(javaBlockState);
            }
        }
    }

    /**
     * @return a set of all forward-facing jigsaws, to use as a fallback if NBT is missing.
     */
    public static IntSet getHorizontalFacingJigsaws() {
        return HORIZONTAL_FACING_JIGSAWS;
    }

    public static boolean isStickyPiston(int blockState) {
        return STICKY_PISTONS.contains(blockState);
    }

    public static boolean isPistonHead(int state) {
        return ALL_PISTON_HEADS.contains(state);
    }

    /**
     * Get the Java Block State for a piston head for a specific direction
     * This is used in PistonBlockEntity to get the BlockCollision for the piston head.
     *
     * @param direction Direction the piston head points in
     * @return Block state for the piston head
     */
    public static int getPistonHead(Direction direction) {
        return PISTON_HEADS.getOrDefault(direction, Block.JAVA_AIR_ID);
    }

    /**
     * This is used in GeyserPistonEvents.java and accepts minecraft:piston,
     * minecraft:sticky_piston, and minecraft:moving_piston.
     *
     * @param state The block state of the piston base
     * @return The direction in which the piston faces
     */
    public static Direction getPistonOrientation(int state) {
        return PISTON_ORIENTATION.get(state);
    }

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
     * Get the type of fluid from the block state.
     *
     * @param state BlockState of the block
     * @return The type of fluid
     */
    public static Fluid getFluid(int state) {
        if (WATER_LEVEL.containsKey(state) || BlockRegistries.WATERLOGGED.get().get(state)) {
            return Fluid.WATER;
        }

        if (LAVA_LEVEL.containsKey(state)) {
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
        return WATER_LEVEL.getOrDefault(state, -1);
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
        return LAVA_LEVEL.getOrDefault(state, -1);
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

    private static Direction getBlockDirection(String javaId) {
        if (javaId.contains("down")) {
            return Direction.DOWN;
        } else if (javaId.contains("up")) {
            return Direction.UP;
        } else if (javaId.contains("south")) {
            return Direction.SOUTH;
        } else if (javaId.contains("west")) {
            return Direction.WEST;
        } else if (javaId.contains("north")) {
            return Direction.NORTH;
        } else if (javaId.contains("east")) {
            return Direction.EAST;
        }
        throw new IllegalStateException();
    }

    private BlockStateValues() {
    }
}
