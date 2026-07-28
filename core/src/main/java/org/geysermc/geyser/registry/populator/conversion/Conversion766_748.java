/*
 * Copyright (c) 2024-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.populator.conversion;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Remaps Pale Garden / resin content (1.21.50+) down for Bedrock below protocol 766.
 */
public final class Conversion766_748 {
    private static final List<String> PALE_WOODEN_BLOCKS = new ArrayList<>();
    private static final List<String> OTHER_NEW_BLOCKS = new ArrayList<>();

    static {
        Set.of(
            Blocks.PALE_OAK_WOOD,
            Blocks.PALE_OAK_PLANKS,
            Blocks.PALE_OAK_SAPLING,
            Blocks.PALE_OAK_LOG,
            Blocks.STRIPPED_PALE_OAK_LOG,
            Blocks.STRIPPED_PALE_OAK_WOOD,
            Blocks.PALE_OAK_LEAVES,
            Blocks.PALE_OAK_HANGING_SIGN,
            Blocks.PALE_OAK_PRESSURE_PLATE,
            Blocks.PALE_OAK_TRAPDOOR,
            Blocks.PALE_OAK_BUTTON,
            Blocks.PALE_OAK_STAIRS,
            Blocks.PALE_OAK_SLAB,
            Blocks.PALE_OAK_FENCE_GATE,
            Blocks.PALE_OAK_FENCE,
            Blocks.PALE_OAK_DOOR
        ).forEach(block -> PALE_WOODEN_BLOCKS.add(block.javaIdentifier().value()));

        PALE_WOODEN_BLOCKS.add("pale_oak_standing_sign");
        PALE_WOODEN_BLOCKS.add("pale_oak_wall_sign");
        PALE_WOODEN_BLOCKS.add("pale_oak_double_slab");

        Set.of(
            Blocks.PALE_MOSS_BLOCK,
            Blocks.PALE_MOSS_CARPET,
            Blocks.PALE_HANGING_MOSS,
            Blocks.OPEN_EYEBLOSSOM,
            Blocks.CLOSED_EYEBLOSSOM,
            Blocks.RESIN_CLUMP,
            Blocks.RESIN_BLOCK,
            Blocks.RESIN_BRICKS,
            Blocks.RESIN_BRICK_STAIRS,
            Blocks.RESIN_BRICK_SLAB,
            Blocks.RESIN_BRICK_WALL,
            Blocks.CHISELED_RESIN_BRICKS,
            Blocks.CREAKING_HEART
        ).forEach(block -> OTHER_NEW_BLOCKS.add(block.javaIdentifier().value()));

        OTHER_NEW_BLOCKS.add("resin_brick_double_slab");
    }

    private Conversion766_748() {
    }

    public static NbtMap remapBlock(NbtMap tag) {
        // First: Downgrade from 1.21.60+
        tag = Conversion776_766.remapBlock(tag);

        String name = tag.getString("name").replace("minecraft:", "");
        if (PALE_WOODEN_BLOCKS.contains(name)) {
            return ConversionHelper.withName(tag, name.replace("pale_oak", "birch"));
        }

        if (OTHER_NEW_BLOCKS.contains(name)) {
            return switch (name) {
                case "resin_brick_double_slab" -> ConversionHelper.withName(tag, "red_sandstone_double_slab");
                case "pale_moss_block" -> ConversionHelper.withName(tag, "moss_block");
                case "pale_moss_carpet" -> ConversionHelper.withoutStates("minecraft:moss_carpet");
                case "pale_hanging_moss" -> ConversionHelper.withoutStates("minecraft:hanging_roots");
                case "open_eyeblossom" -> ConversionHelper.withoutStates("minecraft:oxeye_daisy");
                case "closed_eyeblossom" -> ConversionHelper.withoutStates("minecraft:white_tulip");
                case "resin_clump" -> ConversionHelper.withoutStates("minecraft:unknown");
                case "resin_block" -> ConversionHelper.withoutStates("minecraft:red_sandstone");
                case "resin_bricks" -> ConversionHelper.withoutStates("minecraft:cut_red_sandstone");
                case "resin_brick_stairs" -> ConversionHelper.withName(tag, "red_sandstone_stairs");
                case "resin_brick_slab" -> ConversionHelper.withName(tag, "red_sandstone_slab");
                case "resin_brick_wall" -> ConversionHelper.withName(tag, "red_sandstone_wall");
                case "chiseled_resin_bricks" -> ConversionHelper.withName(tag, "chiseled_red_sandstone");
                case "creaking_heart" -> ConversionHelper.withoutStates("minecraft:chiseled_polished_blackstone");
                default -> tag;
            };
        }

        return tag;
    }
}
