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

package org.geysermc.connector.network.translators.block;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Map;

/**
 * Used for block entities if the Java block state contains Bedrock block information.
 */
public class BlockStateValues {

    private static final Object2IntMap<BlockState> BANNER_COLORS = new Object2IntOpenHashMap<>();
    private static final Object2ByteMap<BlockState> BED_COLORS = new Object2ByteOpenHashMap<>();
    private static final Object2ByteMap<BlockState> SKULL_VARIANTS = new Object2ByteOpenHashMap<>();
    private static final Object2ByteMap<BlockState> SKULL_ROTATIONS = new Object2ByteOpenHashMap<>();

    /**
     * Determines if the block state contains Bedrock block information
     * @param entry The String to JsonNode map used in BlockTranslator
     * @param javaBlockState the Java Block State of the block
     */
    public static void storeBlockStateValues(Map.Entry<String, JsonNode> entry, BlockState javaBlockState) {
        JsonNode bannerColor = entry.getValue().get("banner_color");
        if (bannerColor != null) {
            BlockStateValues.BANNER_COLORS.put(javaBlockState, (byte) bannerColor.intValue());
            return; // There will never be a banner color and a skull variant
        }

        JsonNode bedColor = entry.getValue().get("bed_color");
        if (bedColor != null) {
            BlockStateValues.BED_COLORS.put(javaBlockState, (byte) bedColor.intValue());
            return;
        }

        JsonNode skullVariation = entry.getValue().get("variation");
        if(skullVariation != null) {
            BlockStateValues.SKULL_VARIANTS.put(javaBlockState, (byte) skullVariation.intValue());
        }

        JsonNode skullRotation = entry.getValue().get("skull_rotation");
        if (skullRotation != null) {
            BlockStateValues.SKULL_ROTATIONS.put(javaBlockState, (byte) skullRotation.intValue());
        }
    }

    /**
     * Banner colors are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives an integer color that Bedrock can use.
     * @param state BlockState of the block
     * @return banner color integer or -1 if no color
     */
    public static int getBannerColor(BlockState state) {
        if (BANNER_COLORS.containsKey(state)) {
            return BANNER_COLORS.getInt(state);
        }
        return -1;
    }

    /**
     * Bed colors are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte color that Bedrock can use - Bedrock needs a byte in the final tag.
     * @param state BlockState of the block
     * @return bed color byte or -1 if no color
     */
    public static byte getBedColor(BlockState state) {
        if (BED_COLORS.containsKey(state)) {
            return BED_COLORS.getByte(state);
        }
        return -1;
    }

    /**
     * Skull variations are part of the namespaced ID in Java Edition, but part of the block entity tag in Bedrock.
     * This gives a byte variant ID that Bedrock can use.
     * @param state BlockState of the block
     * @return skull variant byte or -1 if no variant
     */
    public static byte getSkullVariant(BlockState state) {
        if (SKULL_VARIANTS.containsKey(state)) {
            return SKULL_VARIANTS.getByte(state);
        }
        return -1;
    }

    /**
     *
     * @param state BlockState of the block
     * @return skull rotation value or -1 if no value
     */
    public static byte getSkullRotation(BlockState state) {
        if (SKULL_ROTATIONS.containsKey(state)) {
            return SKULL_ROTATIONS.getByte(state);
        }
        return -1;
    }

}
