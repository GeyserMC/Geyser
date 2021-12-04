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

package org.geysermc.geyser.level.block;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.geyser.translator.level.block.entity.PistonBlockEntityTranslator;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.level.physics.PistonBehavior;
import org.geysermc.geyser.util.collection.FixedInt2ByteMap;
import org.geysermc.geyser.util.collection.FixedInt2IntMap;
import org.geysermc.geyser.util.collection.LecternHasBookMap;

/**
 * Used for block entities if the Java block state contains Bedrock block information.
 */
public final class BlockStateValues {
    private static final Int2IntMap BANNER_COLORS = new FixedInt2IntMap();
    private static final Int2ByteMap BED_COLORS = new FixedInt2ByteMap();
    private static final Int2ByteMap COMMAND_BLOCK_VALUES = new Int2ByteOpenHashMap();
    private static final Int2ObjectMap<DoubleChestValue> DOUBLE_CHEST_VALUES = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<String> FLOWER_POT_VALUES = new Int2ObjectOpenHashMap<>();
    private static final LecternHasBookMap LECTERN_BOOK_STATES = new LecternHasBookMap();
    private static final Int2IntMap NOTEBLOCK_PITCHES = new FixedInt2IntMap();
    private static final Int2BooleanMap PISTON_VALUES = new Int2BooleanOpenHashMap();
    private static final IntSet STICKY_PISTONS = new IntOpenHashSet();
    private static final Object2IntMap<Direction> PISTON_HEADS = new Object2IntOpenHashMap<>();
    private static final Int2ObjectMap<Direction> PISTON_ORIENTATION = new Int2ObjectOpenHashMap<>();
    private static final IntSet ALL_PISTON_HEADS = new IntOpenHashSet();
    private static final IntSet MOVING_PISTONS = new IntOpenHashSet();
    private static final Int2ByteMap SKULL_VARIANTS = new FixedInt2ByteMap();
    private static final Int2ByteMap SKULL_ROTATIONS = new Int2ByteOpenHashMap();
    private static final Int2IntMap SKULL_WALL_DIRECTIONS = new Int2IntOpenHashMap();
    private static final Int2ByteMap SHULKERBOX_DIRECTIONS = new FixedInt2ByteMap();
    private static final Int2IntMap WATER_LEVEL = new Int2IntOpenHashMap();

    public static final int JAVA_AIR_ID = 0;

    public static int JAVA_BELL_ID;
    public static int JAVA_COBWEB_ID;
    public static int JAVA_FURNACE_ID;
    public static int JAVA_FURNACE_LIT_ID;
    public static int JAVA_HONEY_BLOCK_ID;
    public static int JAVA_SLIME_BLOCK_ID;
    public static int JAVA_SPAWNER_ID;
    public static int JAVA_WATER_ID;

    /**
     * Determines if the block state contains Bedrock block information
     *
     * @param javaId         The Java Identifier of the block
     * @param javaBlockState the Java Block State of the block
     * @param blockData      JsonNode of info about the block from blocks.json
     */
    public static void storeBlockStateValues(String javaId, int javaBlockState, JsonNode blockData) {
        JsonNode bannerColor = blockData.get("banner_color");
        if (bannerColor != null) {
            BANNER_COLORS.put(javaBlockState, (byte) bannerColor.intValue());
            return; // There will never be a banner color and a skull variant
        }

        JsonNode bedColor = blockData.get("bed_color");
        if (bedColor != null) {
            BED_COLORS.put(javaBlockState, (byte) bedColor.intValue());
            return;
        }

        if (javaId.contains("command_block")) {
            COMMAND_BLOCK_VALUES.put(javaBlockState, javaId.contains("conditional=true") ? (byte) 1 : (byte) 0);
            return;
        }

        if (blockData.get("double_chest_position") != null) {
            boolean isX = (blockData.get("x") != null);
            boolean isDirectionPositive = ((blockData.get("x") != null && blockData.get("x").asBoolean()) ||
                    (blockData.get("z") != null && blockData.get("z").asBoolean()));
            boolean isLeft = (blockData.get("double_chest_position").asText().contains("left"));
            DOUBLE_CHEST_VALUES.put(javaBlockState, new DoubleChestValue(isX, isDirectionPositive, isLeft));
            return;
        }

        if (javaId.startsWith("minecraft:potted_") || javaId.equals("minecraft:flower_pot")) {
            String name = javaId.replace("potted_", "");
            if (name.contains("azalea")) {
                // Exception to the rule
                name = name.replace("_bush", "");
            }
            FLOWER_POT_VALUES.put(javaBlockState, name);
            return;
        }

        if (javaId.startsWith("minecraft:lectern")) {
            LECTERN_BOOK_STATES.put(javaBlockState, javaId.contains("has_book=true"));
            return;
        }

        JsonNode notePitch = blockData.get("note_pitch");
        if (notePitch != null) {
            NOTEBLOCK_PITCHES.put(javaBlockState, blockData.get("note_pitch").intValue());
            return;
        }

        if (javaId.contains("piston[")) { // minecraft:moving_piston, minecraft:sticky_piston, minecraft:piston
            if (javaId.startsWith("minecraft:moving_piston")) {
                MOVING_PISTONS.add(javaBlockState);
            } else {
                PISTON_VALUES.put(javaBlockState, javaId.contains("extended=true"));
            }
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

        JsonNode skullVariation = blockData.get("variation");
        if (skullVariation != null) {
            SKULL_VARIANTS.put(javaBlockState, (byte) skullVariation.intValue());
        }

        JsonNode skullRotation = blockData.get("skull_rotation");
        if (skullRotation != null) {
            SKULL_ROTATIONS.put(javaBlockState, (byte) skullRotation.intValue());
        }

        if (javaId.contains("wall_skull") || javaId.contains("wall_head")) {
            String direction = javaId.substring(javaId.lastIndexOf("facing=") + 7);
            int rotation = switch (direction.substring(0, direction.length() - 1)) {
                case "north" -> 180;
                case "west" -> 90;
                case "east" -> 270;
                default -> 0; // Also south
            };
            SKULL_WALL_DIRECTIONS.put(javaBlockState, rotation);
        }

        JsonNode shulkerDirection = blockData.get("shulker_direction");
        if (shulkerDirection != null) {
            BlockStateValues.SHULKERBOX_DIRECTIONS.put(javaBlockState, (byte) shulkerDirection.intValue());
        }

        if (javaId.startsWith("minecraft:water")) {
            String strLevel = javaId.substring(javaId.lastIndexOf("level=") + 6, javaId.length() - 1);
            int level = Integer.parseInt(strLevel);
            WATER_LEVEL.put(javaBlockState, level);
        }
    }

    /**
     * Banner colors are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives an integer color that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return Banner color integer or -1 if no color
     */
    public static int getBannerColor(int state) {
        return BANNER_COLORS.getOrDefault(state, -1);
    }

    /**
     * Bed colors are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte color that Bedrock can use - Bedrock needs a byte in the final tag.
     *
     * @param state BlockState of the block
     * @return Bed color byte or -1 if no color
     */
    public static byte getBedColor(int state) {
        return BED_COLORS.getOrDefault(state, (byte) -1);
    }

    /**
     * The block state in Java and Bedrock both contain the conditional bit, however command block block entity tags
     * in Bedrock need the conditional information.
     *
     * @return the list of all command blocks and if they are conditional (1 or 0)
     */
    public static Int2ByteMap getCommandBlockValues() {
        return COMMAND_BLOCK_VALUES;
    }

    /**
     * All double chest values are part of the block state in Java and part of the block entity tag in Bedrock.
     * This gives the DoubleChestValue that can be calculated into the final tag.
     *
     * @return The map of all DoubleChestValues.
     */
    public static Int2ObjectMap<DoubleChestValue> getDoubleChestValues() {
        return DOUBLE_CHEST_VALUES;
    }

    /**
     * Get the Int2ObjectMap of flower pot block states to containing plant
     *
     * @return Int2ObjectMap of flower pot values
     */
    public static Int2ObjectMap<String> getFlowerPotValues() {
        return FLOWER_POT_VALUES;
    }

    /**
     * @return the lectern book state map pointing to book present state
     */
    public static LecternHasBookMap getLecternBookStates() {
        return LECTERN_BOOK_STATES;
    }

    /**
     * The note that noteblocks output when hit is part of the block state in Java but sent as a BlockEventPacket in Bedrock.
     * This gives an integer pitch that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return note block note integer or -1 if not present
     */
    public static int getNoteblockPitch(int state) {
        return NOTEBLOCK_PITCHES.getOrDefault(state, -1);
    }

    /**
     * Get the Int2BooleanMap showing if a piston block state is extended or not.
     *
     * @return the Int2BooleanMap of piston extensions.
     */
    public static Int2BooleanMap getPistonValues() {
        return PISTON_VALUES;
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
        return PISTON_HEADS.getOrDefault(direction, BlockStateValues.JAVA_AIR_ID);
    }

    /**
     * Check if a block is a minecraft:moving_piston
     * This is used in ChunkUtils to prevent them from being placed as it causes
     * pistons to flicker and it is not needed
     *
     * @param state Block state of the block
     * @return True if the block is a moving_piston
     */
    public static boolean isMovingPiston(int state) {
        return MOVING_PISTONS.contains(state);
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
    public static boolean isBlockSticky(int state) {
        return state == JAVA_SLIME_BLOCK_ID || state == JAVA_HONEY_BLOCK_ID;
    }

    /**
     * Check if two blocks are attached to each other.
     *
     * @param stateA The block state of block a
     * @param stateB The block state of block b
     * @return True if the blocks are attached to each other
     */
    public static boolean isBlockAttached(int stateA, int stateB) {
        boolean aSticky = isBlockSticky(stateA);
        boolean bSticky = isBlockSticky(stateB);
        if (aSticky && bSticky) {
            // Only matching sticky blocks are attached together
            // Honey + Honey & Slime + Slime
            return stateA == stateB;
        }
        return aSticky || bSticky;
    }

    /**
     * @param state The block state of the block
     * @return true if a piston can break the block
     */
    public static boolean canPistonDestroyBlock(int state)  {
        return BlockRegistries.JAVA_BLOCKS.getOrDefault(state, BlockMapping.AIR).getPistonBehavior() == PistonBehavior.DESTROY;
    }

    public static boolean canPistonMoveBlock(int javaId, boolean isPushing) {
        if (javaId == JAVA_AIR_ID) {
            return true;
        }
        // Pistons can only be moved if they aren't extended
        if (PistonBlockEntityTranslator.isBlock(javaId)) {
            return !PISTON_VALUES.get(javaId);
        }
        BlockMapping block = BlockRegistries.JAVA_BLOCKS.getOrDefault(javaId, BlockMapping.AIR);
        // Bedrock, End portal frames, etc. can't be moved
        if (block.getHardness() == -1.0d) {
            return false;
        }
        return switch (block.getPistonBehavior()) {
            case BLOCK, DESTROY -> false;
            case PUSH_ONLY -> isPushing; // Glazed terracotta can only be pushed
            default -> !block.isBlockEntity(); // Pistons can't move block entities
        };
    }

    /**
     * Skull variations are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte variant ID that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return Skull variant byte or -1 if no variant
     */
    public static byte getSkullVariant(int state) {
        return SKULL_VARIANTS.getOrDefault(state, (byte) -1);
    }

    /**
     * Skull rotations are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte rotation that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return Skull rotation value or -1 if no value
     */
    public static byte getSkullRotation(int state) {
        return SKULL_ROTATIONS.getOrDefault(state, (byte) -1);
    }

    /**
     * Skull rotations are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a integer rotation that Bedrock can use.
     *
     * @return Skull wall rotation value with the blockstate
     */
    public static Int2IntMap getSkullWallDirections() {
        return SKULL_WALL_DIRECTIONS;
    }

    /**
     * Shulker box directions are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte direction that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return Shulker direction value or -1 if no value
     */
    public static byte getShulkerBoxDirection(int state) {
        return SHULKERBOX_DIRECTIONS.getOrDefault(state, (byte) -1);
    }

    /**
     * Get the level of water from the block state.
     * This is used in FishingHookEntity to create splash sounds when the hook hits the water.
     *
     * @param state BlockState of the block
     * @return The water level or -1 if the block isn't water
     */
    public static int getWaterLevel(int state) {
        return WATER_LEVEL.getOrDefault(state, -1);
    }

    /**
     * Get the slipperiness of a block.
     * This is used in ItemEntity to calculate the friction on an item as it slides across the ground
     *
     * @param state BlockState of the block
     * @return The block's slipperiness
     */
    public static float getSlipperiness(int state) {
        String blockIdentifier = BlockRegistries.JAVA_BLOCKS.getOrDefault(state, BlockMapping.AIR).getJavaIdentifier();
        return switch (blockIdentifier) {
            case "minecraft:slime_block" -> 0.8f;
            case "minecraft:ice", "minecraft:packed_ice" -> 0.98f;
            case "minecraft:blue_ice" -> 0.989f;
            default -> 0.6f;
        };
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
