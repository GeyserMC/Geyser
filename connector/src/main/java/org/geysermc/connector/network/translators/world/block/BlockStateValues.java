/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.world.block;

import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.nbt.NbtMap;
import it.unimi.dsi.fastutil.ints.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Used for block entities if the Java block state contains Bedrock block information.
 */
public class BlockStateValues {

    private static final Int2IntMap BANNER_COLORS = new Int2IntOpenHashMap();
    private static final Int2ByteMap BED_COLORS = new Int2ByteOpenHashMap();
    private static final Int2ObjectMap<DoubleChestValue> DOUBLE_CHEST_VALUES = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<String> FLOWER_POT_VALUES = new Int2ObjectOpenHashMap<>();
    private static final Map<Integer, String> WALL_SKULL_DIRECTION = new HashMap<>();
    private static final Map<String, NbtMap> FLOWER_POT_BLOCKS = new HashMap<>();
    private static final Int2IntMap NOTEBLOCK_PITCHES = new Int2IntOpenHashMap();
    private static final Int2BooleanMap IS_STICKY_PISTON = new Int2BooleanOpenHashMap();
    private static final Int2BooleanMap PISTON_VALUES = new Int2BooleanOpenHashMap();
    private static final Int2ByteMap SKULL_VARIANTS = new Int2ByteOpenHashMap();
    private static final Int2ByteMap SKULL_ROTATIONS = new Int2ByteOpenHashMap();
    private static final Int2ByteMap SHULKERBOX_DIRECTIONS = new Int2ByteOpenHashMap();

    /**
     * Determines if the block state contains Bedrock block information
     * @param entry The String to JsonNode map used in BlockTranslator
     * @param javaBlockState the Java Block State of the block
     */
    public static void storeBlockStateValues(Map.Entry<String, JsonNode> entry, int javaBlockState) {
        JsonNode bannerColor = entry.getValue().get("banner_color");
        if (bannerColor != null) {
            BANNER_COLORS.put(javaBlockState, (byte) bannerColor.intValue());
            return; // There will never be a banner color and a skull variant
        }

        JsonNode bedColor = entry.getValue().get("bed_color");
        if (bedColor != null) {
            BED_COLORS.put(javaBlockState, (byte) bedColor.intValue());
            return;
        }

        if (entry.getValue().get("double_chest_position") != null) {
            boolean isX = (entry.getValue().get("x") != null);
            boolean isDirectionPositive = ((entry.getValue().get("x") != null && entry.getValue().get("x").asBoolean()) ||
                    (entry.getValue().get("z") != null && entry.getValue().get("z").asBoolean()));
            boolean isLeft = (entry.getValue().get("double_chest_position").asText().contains("left"));
            DOUBLE_CHEST_VALUES.put(javaBlockState, new DoubleChestValue(isX, isDirectionPositive, isLeft));
            return;
        }

        if (entry.getKey().contains("potted_") || entry.getKey().contains("flower_pot")) {
            FLOWER_POT_VALUES.put(javaBlockState, entry.getKey().replace("potted_", ""));
            return;
        }

        JsonNode notePitch = entry.getValue().get("note_pitch");
        if (notePitch != null) {
            NOTEBLOCK_PITCHES.put(javaBlockState, entry.getValue().get("note_pitch").intValue());
            return;
        }

        if (entry.getKey().contains("piston")) {
            // True if extended, false if not
            PISTON_VALUES.put(javaBlockState, entry.getKey().contains("extended=true"));
            IS_STICKY_PISTON.put(javaBlockState, entry.getKey().contains("sticky"));
            return;
        }

        JsonNode skullVariation = entry.getValue().get("variation");
        if(skullVariation != null) {
            SKULL_VARIANTS.put(javaBlockState, (byte) skullVariation.intValue());
        }

        JsonNode skullRotation = entry.getValue().get("skull_rotation");
        if (skullRotation != null) {
            SKULL_ROTATIONS.put(javaBlockState, (byte) skullRotation.intValue());
        }

        JsonNode shulkerDirection = entry.getValue().get("shulker_direction");
        if (shulkerDirection != null) {
            BlockStateValues.SHULKERBOX_DIRECTIONS.put(javaBlockState, (byte) shulkerDirection.intValue());
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
        if (BANNER_COLORS.containsKey(state)) {
            return BANNER_COLORS.get(state);
        }
        return -1;
    }

    /**
     * Bed colors are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte color that Bedrock can use - Bedrock needs a byte in the final tag.
     *
     * @param state BlockState of the block
     * @return Bed color byte or -1 if no color
     */
    public static byte getBedColor(int state) {
        if (BED_COLORS.containsKey(state)) {
            return BED_COLORS.get(state);
        }
        return -1;
    }

    /**
     * All double chest values are part of the block state in Java and part of the block entity tag in Bedrock.
     * This gives the DoubleChestValue that can be calculated into the final tag.
     * @return The map of all DoubleChestValues.
     */
    public static Int2ObjectMap<DoubleChestValue> getDoubleChestValues() {
        return DOUBLE_CHEST_VALUES;
    }

    /**
     * Get the Int2ObjectMap of flower pot block states to containing plant
     * @return Int2ObjectMap of flower pot values
     */
    public static Int2ObjectMap<String> getFlowerPotValues() {
        return FLOWER_POT_VALUES;
    }

    /**
     * Get the map of contained flower pot plants to Bedrock CompoundTag
     * @return Map of flower pot blocks.
     */
    public static Map<String, NbtMap> getFlowerPotBlocks() {
        return FLOWER_POT_BLOCKS;
    }

    /**
     * The note that noteblocks output when hit is part of the block state in Java but sent as a BlockEventPacket in Bedrock.
     * This gives an integer pitch that Bedrock can use.
     * @param state BlockState of the block
     * @return note block note integer or -1 if not present
     */
    public static int getNoteblockPitch(int state) {
        if (NOTEBLOCK_PITCHES.containsKey(state)) {
            return NOTEBLOCK_PITCHES.get(state);
        }
        return -1;
    }

    /**
     * Get the Int2BooleanMap showing if a piston block state is extended or not.
     * @return the Int2BooleanMap of piston extensions.
     */
    public static Int2BooleanMap getPistonValues() {
        return PISTON_VALUES;
    }

    public static boolean isStickyPiston(int blockState) {
        return IS_STICKY_PISTON.get(blockState);
    }

    /**
     * Skull variations are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte variant ID that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return Skull variant byte or -1 if no variant
     */
    public static byte getSkullVariant(int state) {
        if (SKULL_VARIANTS.containsKey(state)) {
            return SKULL_VARIANTS.get(state);
        }
        return -1;
    }

    /**
     * Skull rotations are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a string rotation that Bedrock can use.
     *
     * @return Skull wall rotation value with the blockstate
     */
    public static Map<Integer, String> getWallSkullDirection() {
        return WALL_SKULL_DIRECTION;
    }

    /**
     * Skull rotations are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte rotation that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return Skull rotation value or -1 if no value
     */
    public static byte getSkullRotation(int state) {
        if (SKULL_ROTATIONS.containsKey(state)) {
            return SKULL_ROTATIONS.get(state);
        }
        return -1;
    }


    /**
     * Shulker box directions are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte direction that Bedrock can use.
     *
     * @param state BlockState of the block
     * @return Shulker direction value or -1 if no value
     */
    public static byte getShulkerBoxDirection(int state) {
        if (SHULKERBOX_DIRECTIONS.containsKey(state)) {
            return SHULKERBOX_DIRECTIONS.get(state);
        }
        return -1;
    }
}
