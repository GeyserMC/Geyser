/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WildernessBoundConverter extends ConversionHelper {
    private static final Map<String, String> BLOCK_MAPPINGS = new HashMap<>();
    private static final List<String> CLEAR_BLOCK_STATES = new ArrayList<>();
    private static final Map<Item, Item> ITEM_MAPPINGS = new HashMap<>();

    private static void addBlock(Block newBlock, Block fallback) {
        addBlock(newBlock, fallback, false);
    }

    private static void addBlock(Block newBlock, Block fallback, boolean clearBlockStates) {
        addBlock(newBlock.javaIdentifier().asString(), fallback.javaIdentifier().asString(), newBlock.asItem(), fallback.asItem(), clearBlockStates);
    }

    private static void addBlock(String newBlock, String blockFallback, Item newItem, Item itemFallback, boolean clearBlockStates) {
        BLOCK_MAPPINGS.put(newBlock, blockFallback);
        ITEM_MAPPINGS.put(newItem, itemFallback);
        if (clearBlockStates) {
            CLEAR_BLOCK_STATES.add(newBlock);
        }
    }

    private static void addBlockOnly(String newBlock, String fallback) {
        BLOCK_MAPPINGS.put(newBlock, fallback);
    }

    private static void addItem(Item newItem, Item fallback) {
        ITEM_MAPPINGS.put(newItem, fallback);
    }

    static {
        addBlock(Blocks.WHITE_CONCRETE_STAIRS, Blocks.SMOOTH_QUARTZ_STAIRS);
        addBlock(Blocks.ORANGE_CONCRETE_STAIRS, Blocks.RED_SANDSTONE_STAIRS);
        addBlock(Blocks.MAGENTA_CONCRETE_STAIRS, Blocks.POLISHED_GRANITE_STAIRS);
        addBlock("minecraft:light_blue_concrete_stairs", "minecraft:prismarine_bricks_stairs",
            Items.LIGHT_BLUE_CONCRETE_STAIRS, Items.PRISMARINE_BRICK_STAIRS, false);
        addBlock(Blocks.YELLOW_CONCRETE_STAIRS, Blocks.BAMBOO_STAIRS);
        addBlock("minecraft:lime_concrete_stairs", "minecraft:prismarine_bricks_stairs",
            Items.LIME_CONCRETE_STAIRS, Items.PRISMARINE_BRICK_STAIRS, false);
        addBlock(Blocks.PINK_CONCRETE_STAIRS, Blocks.GRANITE_STAIRS);
        addBlock(Blocks.GRAY_CONCRETE_STAIRS, Blocks.ANDESITE_STAIRS);
        addBlock(Blocks.LIGHT_GRAY_CONCRETE_STAIRS, Blocks.POLISHED_ANDESITE_STAIRS);
        addBlock(Blocks.CYAN_CONCRETE_STAIRS, Blocks.WARPED_STAIRS);
        addBlock(Blocks.PURPLE_CONCRETE_STAIRS, Blocks.PURPUR_STAIRS);
        addBlock(Blocks.BLUE_CONCRETE_STAIRS, Blocks.PRISMARINE_STAIRS);
        addBlock(Blocks.BROWN_CONCRETE_STAIRS, Blocks.CINNABAR_STAIRS);
        addBlock(Blocks.GREEN_CONCRETE_STAIRS, Blocks.DARK_PRISMARINE_STAIRS);
        addBlock(Blocks.RED_CONCRETE_STAIRS, Blocks.CRIMSON_STAIRS);
        addBlock(Blocks.BLACK_CONCRETE_STAIRS, Blocks.BLACKSTONE_STAIRS);

        addBlock(Blocks.WHITE_CONCRETE_SLAB, Blocks.SMOOTH_QUARTZ_SLAB);
        addBlock(Blocks.ORANGE_CONCRETE_SLAB, Blocks.RED_SANDSTONE_SLAB);
        addBlock(Blocks.MAGENTA_CONCRETE_SLAB, Blocks.POLISHED_GRANITE_SLAB);
        addBlock(Blocks.LIGHT_BLUE_CONCRETE_SLAB, Blocks.PRISMARINE_BRICK_SLAB);
        addBlock(Blocks.YELLOW_CONCRETE_SLAB, Blocks.BAMBOO_SLAB);
        addBlock(Blocks.LIME_CONCRETE_SLAB, Blocks.PRISMARINE_BRICK_SLAB);
        addBlock(Blocks.PINK_CONCRETE_SLAB, Blocks.GRANITE_SLAB);
        addBlock(Blocks.GRAY_CONCRETE_SLAB, Blocks.ANDESITE_SLAB);
        addBlock(Blocks.LIGHT_GRAY_CONCRETE_SLAB, Blocks.POLISHED_ANDESITE_SLAB);
        addBlock(Blocks.CYAN_CONCRETE_SLAB, Blocks.WARPED_SLAB);
        addBlock(Blocks.PURPLE_CONCRETE_SLAB, Blocks.PURPUR_SLAB);
        addBlock(Blocks.BLUE_CONCRETE_SLAB, Blocks.PRISMARINE_SLAB);
        addBlock(Blocks.BROWN_CONCRETE_SLAB, Blocks.CINNABAR_SLAB);
        addBlock(Blocks.GREEN_CONCRETE_SLAB, Blocks.DARK_PRISMARINE_SLAB);
        addBlock(Blocks.RED_CONCRETE_SLAB, Blocks.CRIMSON_SLAB);
        addBlock(Blocks.BLACK_CONCRETE_SLAB, Blocks.BLACKSTONE_SLAB);

        // Bedrock <3
        addBlockOnly("minecraft:white_concrete_double_slab", "minecraft:smooth_quartz_double_slab");
        addBlockOnly("minecraft:orange_concrete_double_slab", "minecraft:red_sandstone_double_slab");
        addBlockOnly("minecraft:magenta_concrete_double_slab", "minecraft:polished_granite_double_slab");
        addBlockOnly("minecraft:light_blue_concrete_double_slab", "minecraft:prismarine_brick_double_slab");
        addBlockOnly("minecraft:yellow_concrete_double_slab", "minecraft:bamboo_double_slab");
        addBlockOnly("minecraft:lime_concrete_double_slab", "minecraft:prismarine_brick_double_slab");
        addBlockOnly("minecraft:pink_concrete_double_slab", "minecraft:granite_double_slab");
        addBlockOnly("minecraft:gray_concrete_double_slab", "minecraft:andesite_double_slab");
        addBlockOnly("minecraft:light_gray_concrete_double_slab", "minecraft:polished_andesite_double_slab");
        addBlockOnly("minecraft:cyan_concrete_double_slab", "minecraft:warped_double_slab");
        addBlockOnly("minecraft:purple_concrete_double_slab", "minecraft:purpur_double_slab");
        addBlockOnly("minecraft:blue_concrete_double_slab", "minecraft:prismarine_double_slab");
        addBlockOnly("minecraft:brown_concrete_double_slab", "minecraft:cinnabar_double_slab");
        addBlockOnly("minecraft:green_concrete_double_slab", "minecraft:dark_prismarine_double_slab");
        addBlockOnly("minecraft:red_concrete_double_slab", "minecraft:crimson_double_slab");
        addBlockOnly("minecraft:black_concrete_double_slab", "minecraft:blackstone_double_slab");

        addBlock(Blocks.POPLAR_SAPLING, Blocks.PALE_OAK_SAPLING);
        addBlock(Blocks.ORANGE_POPLAR_LEAVES, Blocks.ACACIA_LEAVES);
        addBlock(Blocks.RED_POPLAR_LEAVES, Blocks.MANGROVE_LEAVES);
        addBlock(Blocks.YELLOW_POPLAR_LEAVES, Blocks.AZALEA_LEAVES);
        addBlock(Blocks.POPLAR_LOG, Blocks.PALE_OAK_LOG);
        addBlock(Blocks.STRIPPED_POPLAR_LOG, Blocks.STRIPPED_PALE_OAK_LOG);
        addBlock(Blocks.POPLAR_WOOD, Blocks.PALE_OAK_WOOD);
        addBlock(Blocks.STRIPPED_POPLAR_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD);

        addBlock(Blocks.POPLAR_PLANKS, Blocks.PALE_OAK_PLANKS);
        addBlock(Blocks.POPLAR_STAIRS, Blocks.PALE_OAK_STAIRS);
        addBlock(Blocks.POPLAR_SLAB, Blocks.PALE_OAK_SLAB);
        addBlockOnly("minecraft:poplar_double_slab", "minecraft:pale_oak_double_slab");
        addBlock("minecraft:poplar_standing_sign", "minecraft:pale_oak_standing_sign",
            Items.POPLAR_SIGN, Items.PALE_OAK_SIGN, false);
        addBlock(Blocks.POPLAR_WALL_SIGN, Blocks.PALE_OAK_WALL_SIGN);
        addBlock(Blocks.POPLAR_HANGING_SIGN, Blocks.PALE_OAK_HANGING_SIGN);
        addBlock(Blocks.POPLAR_WALL_HANGING_SIGN, Blocks.PALE_OAK_WALL_HANGING_SIGN);
        addBlock(Blocks.POPLAR_BUTTON, Blocks.PALE_OAK_BUTTON);
        addBlock(Blocks.POPLAR_PRESSURE_PLATE, Blocks.PALE_OAK_PRESSURE_PLATE);
        addBlock(Blocks.POPLAR_DOOR, Blocks.PALE_OAK_DOOR);
        addBlock(Blocks.POPLAR_FENCE, Blocks.PALE_OAK_FENCE);
        addBlock(Blocks.POPLAR_FENCE_GATE, Blocks.PALE_OAK_FENCE_GATE);
        addBlock(Blocks.POPLAR_TRAPDOOR, Blocks.PALE_OAK_TRAPDOOR);
        addBlock(Blocks.POPLAR_SHELF, Blocks.PALE_OAK_SHELF);

        addItem(Items.POPLAR_BOAT, Items.PALE_OAK_BOAT);
        addItem(Items.POPLAR_CHEST_BOAT, Items.PALE_OAK_CHEST_BOAT);

        addBlock(Blocks.RED_SHRUB, Blocks.BUSH);
        addBlock(Blocks.SHELF_MUSHROOM, Blocks.OAK_PLANKS, true); // FIXME
        addBlock(Blocks.STRAW_BED, Blocks.YELLOW_CARPET, true); // TODO ideally convert to yellow bed, but the block state is different on bedrock

        addBlock(Blocks.WHITE_WOOL_STAIRS, Blocks.SMOOTH_QUARTZ_STAIRS);
        addBlock(Blocks.ORANGE_WOOL_STAIRS, Blocks.RED_SANDSTONE_STAIRS);
        addBlock(Blocks.MAGENTA_WOOL_STAIRS, Blocks.POLISHED_GRANITE_STAIRS);
        addBlock("minecraft:light_blue_wool_stairs", "minecraft:prismarine_bricks_stairs",
            Items.LIGHT_BLUE_WOOL_STAIRS, Items.PRISMARINE_BRICK_STAIRS, false);
        addBlock(Blocks.YELLOW_WOOL_STAIRS, Blocks.BAMBOO_STAIRS);
        addBlock("minecraft:lime_wool_stairs", "minecraft:prismarine_bricks_stairs",
            Items.LIME_WOOL_STAIRS, Items.PRISMARINE_BRICK_STAIRS, false);
        addBlock(Blocks.PINK_WOOL_STAIRS, Blocks.GRANITE_STAIRS);
        addBlock(Blocks.GRAY_WOOL_STAIRS, Blocks.ANDESITE_STAIRS);
        addBlock(Blocks.LIGHT_GRAY_WOOL_STAIRS, Blocks.POLISHED_ANDESITE_STAIRS);
        addBlock(Blocks.CYAN_WOOL_STAIRS, Blocks.WARPED_STAIRS);
        addBlock(Blocks.PURPLE_WOOL_STAIRS, Blocks.PURPUR_STAIRS);
        addBlock(Blocks.BLUE_WOOL_STAIRS, Blocks.PRISMARINE_STAIRS);
        addBlock(Blocks.BROWN_WOOL_STAIRS, Blocks.CINNABAR_STAIRS);
        addBlock(Blocks.GREEN_WOOL_STAIRS, Blocks.DARK_PRISMARINE_STAIRS);
        addBlock(Blocks.RED_WOOL_STAIRS, Blocks.CRIMSON_STAIRS);
        addBlock(Blocks.BLACK_WOOL_STAIRS, Blocks.BLACKSTONE_STAIRS);

        addBlock(Blocks.WHITE_WOOL_SLAB, Blocks.SMOOTH_QUARTZ_SLAB);
        addBlock(Blocks.ORANGE_WOOL_SLAB, Blocks.RED_SANDSTONE_SLAB);
        addBlock(Blocks.MAGENTA_WOOL_SLAB, Blocks.POLISHED_GRANITE_SLAB);
        addBlock(Blocks.LIGHT_BLUE_WOOL_SLAB, Blocks.PRISMARINE_BRICK_SLAB);
        addBlock(Blocks.YELLOW_WOOL_SLAB, Blocks.BAMBOO_SLAB);
        addBlock(Blocks.LIME_WOOL_SLAB, Blocks.PRISMARINE_BRICK_SLAB);
        addBlock(Blocks.PINK_WOOL_SLAB, Blocks.GRANITE_SLAB);
        addBlock(Blocks.GRAY_WOOL_SLAB, Blocks.ANDESITE_SLAB);
        addBlock(Blocks.LIGHT_GRAY_WOOL_SLAB, Blocks.POLISHED_ANDESITE_SLAB);
        addBlock(Blocks.CYAN_WOOL_SLAB, Blocks.WARPED_SLAB);
        addBlock(Blocks.PURPLE_WOOL_SLAB, Blocks.PURPUR_SLAB);
        addBlock(Blocks.BLUE_WOOL_SLAB, Blocks.PRISMARINE_SLAB);
        addBlock(Blocks.BROWN_WOOL_SLAB, Blocks.CINNABAR_SLAB);
        addBlock(Blocks.GREEN_WOOL_SLAB, Blocks.DARK_PRISMARINE_SLAB);
        addBlock(Blocks.RED_WOOL_SLAB, Blocks.CRIMSON_SLAB);
        addBlock(Blocks.BLACK_WOOL_SLAB, Blocks.BLACKSTONE_SLAB);

        // Bedrock <3
        addBlockOnly("minecraft:white_wool_double_slab", "minecraft:smooth_quartz_double_slab");
        addBlockOnly("minecraft:orange_wool_double_slab", "minecraft:red_sandstone_double_slab");
        addBlockOnly("minecraft:magenta_wool_double_slab", "minecraft:polished_granite_double_slab");
        addBlockOnly("minecraft:light_blue_wool_double_slab", "minecraft:prismarine_brick_double_slab");
        addBlockOnly("minecraft:yellow_wool_double_slab", "minecraft:bamboo_double_slab");
        addBlockOnly("minecraft:lime_wool_double_slab", "minecraft:prismarine_brick_double_slab");
        addBlockOnly("minecraft:pink_wool_double_slab", "minecraft:granite_double_slab");
        addBlockOnly("minecraft:gray_wool_double_slab", "minecraft:andesite_double_slab");
        addBlockOnly("minecraft:light_gray_wool_double_slab", "minecraft:polished_andesite_double_slab");
        addBlockOnly("minecraft:cyan_wool_double_slab", "minecraft:warped_double_slab");
        addBlockOnly("minecraft:purple_wool_double_slab", "minecraft:purpur_double_slab");
        addBlockOnly("minecraft:blue_wool_double_slab", "minecraft:prismarine_double_slab");
        addBlockOnly("minecraft:brown_wool_double_slab", "minecraft:cinnabar_double_slab");
        addBlockOnly("minecraft:green_wool_double_slab", "minecraft:dark_prismarine_double_slab");
        addBlockOnly("minecraft:red_wool_double_slab", "minecraft:crimson_double_slab");
        addBlockOnly("minecraft:black_wool_double_slab", "minecraft:blackstone_double_slab");

        addItem(Items.WHITE_CUSHION, Items.WHITE_CARPET);
        addItem(Items.ORANGE_CUSHION, Items.ORANGE_CARPET);
        addItem(Items.MAGENTA_CUSHION, Items.MAGENTA_CARPET);
        addItem(Items.LIGHT_BLUE_CUSHION, Items.LIGHT_BLUE_CARPET);
        addItem(Items.YELLOW_CUSHION, Items.YELLOW_CARPET);
        addItem(Items.LIME_CUSHION, Items.LIME_CARPET);
        addItem(Items.PINK_CUSHION, Items.PINK_CARPET);
        addItem(Items.GRAY_CUSHION, Items.GRAY_CARPET);
        addItem(Items.LIGHT_GRAY_CUSHION, Items.LIGHT_GRAY_CARPET);
        addItem(Items.CYAN_CUSHION, Items.CYAN_CARPET);
        addItem(Items.PURPLE_CUSHION, Items.PURPLE_CARPET);
        addItem(Items.BLUE_CUSHION, Items.BLUE_CARPET);
        addItem(Items.BROWN_CUSHION, Items.BROWN_CARPET);
        addItem(Items.GREEN_CUSHION, Items.GREEN_CARPET);
        addItem(Items.RED_CUSHION, Items.RED_CARPET);
        addItem(Items.BLACK_CUSHION, Items.BLACK_CARPET);
    }

    public static NbtMap convertBlock(NbtMap tag) {
        String replacement = BLOCK_MAPPINGS.get(tag.getString("name"));
        if (replacement != null) {
            if (CLEAR_BLOCK_STATES.contains(tag.getString("name"))) {
                tag = withoutStates(replacement);
            } else {
                tag = withId(tag, replacement);
            }
        }
        return ICanHasStates.convertBlock(tag);
    }

    public static Map<Item, Item> itemMappings() {
        return ITEM_MAPPINGS;
    }
}
