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

public class ChaosCubedConverter extends ConversionHelper {
    private static final Map<String, String> BLOCK_MAPPINGS = new HashMap<>();
    private static final List<String> CLEAR_BLOCK_STATES = new ArrayList<>();
    private static final Map<Item, Item> ITEM_MAPPINGS = new HashMap<>();

    private static void addBlock(Block newBlock, Block fallback) {
        addBlock(newBlock, fallback, false);
    }

    private static void addBlock(Block newBlock, Block fallback, boolean clearBlockStates) {
        BLOCK_MAPPINGS.put(newBlock.javaIdentifier().asString(), fallback.javaIdentifier().asString());
        ITEM_MAPPINGS.put(newBlock.asItem(), fallback.asItem());
        if (clearBlockStates) CLEAR_BLOCK_STATES.add(newBlock.javaIdentifier().asString());
    }

    private static void addBlockOnly(String newBlock, String fallback) {
        BLOCK_MAPPINGS.put(newBlock, fallback);
    }

    private static void addItem(Item newItem, Item fallback) {
        ITEM_MAPPINGS.put(newItem, fallback);
    }

    static {
        addBlock(Blocks.POTENT_SULFUR, Blocks.YELLOW_CONCRETE, true);
        addBlock(Blocks.SULFUR_SPIKE, Blocks.POINTED_DRIPSTONE);

        addBlock(Blocks.SULFUR, Blocks.YELLOW_CONCRETE);
        addBlock(Blocks.SULFUR_SLAB, Blocks.BAMBOO_SLAB);
        addBlockOnly("minecraft:sulfur_double_slab", "minecraft:bamboo_double_slab");
        addBlock(Blocks.SULFUR_STAIRS, Blocks.BAMBOO_STAIRS);
        addBlock(Blocks.SULFUR_WALL, Blocks.END_STONE_BRICK_WALL);

        addBlock(Blocks.POLISHED_SULFUR, Blocks.YELLOW_CONCRETE);
        addBlock(Blocks.POLISHED_SULFUR_SLAB, Blocks.BAMBOO_SLAB);
        addBlockOnly("minecraft:polished_sulfur_double_slab", "minecraft:bamboo_double_slab");
        addBlock(Blocks.POLISHED_SULFUR_STAIRS, Blocks.BAMBOO_STAIRS);
        addBlock(Blocks.POLISHED_SULFUR_WALL, Blocks.END_STONE_BRICK_WALL);

        addBlock(Blocks.SULFUR_BRICKS, Blocks.YELLOW_CONCRETE);
        addBlock(Blocks.SULFUR_BRICK_SLAB, Blocks.BAMBOO_SLAB);
        addBlockOnly("minecraft:sulfur_brick_double_slab", "minecraft:bamboo_double_slab");
        addBlock(Blocks.SULFUR_BRICK_STAIRS, Blocks.BAMBOO_STAIRS);
        addBlock(Blocks.SULFUR_BRICK_WALL, Blocks.END_STONE_BRICK_WALL);

        addBlock(Blocks.CHISELED_SULFUR, Blocks.YELLOW_CONCRETE);

        addBlock(Blocks.CINNABAR, Blocks.RED_CONCRETE);
        addBlock(Blocks.CINNABAR_SLAB, Blocks.MANGROVE_SLAB);
        addBlockOnly("minecraft:cinnabar_double_slab", "minecraft:mangrove_double_slab");
        addBlock(Blocks.CINNABAR_STAIRS, Blocks.MANGROVE_STAIRS);
        addBlock(Blocks.CINNABAR_WALL, Blocks.RED_NETHER_BRICK_WALL);

        addBlock(Blocks.POLISHED_CINNABAR, Blocks.RED_CONCRETE);
        addBlock(Blocks.POLISHED_CINNABAR_SLAB, Blocks.MANGROVE_SLAB);
        addBlockOnly("minecraft:polished_cinnabar_double_slab", "minecraft:mangrove_double_slab");
        addBlock(Blocks.POLISHED_CINNABAR_STAIRS, Blocks.MANGROVE_STAIRS);
        addBlock(Blocks.POLISHED_CINNABAR_WALL, Blocks.RED_NETHER_BRICK_WALL);

        addBlock(Blocks.CINNABAR_BRICKS, Blocks.RED_CONCRETE);
        addBlock(Blocks.CINNABAR_BRICK_SLAB, Blocks.MANGROVE_SLAB);
        addBlockOnly("minecraft:cinnabar_brick_double_slab", "minecraft:mangrove_double_slab");
        addBlock(Blocks.CINNABAR_BRICK_STAIRS, Blocks.MANGROVE_STAIRS);
        addBlock(Blocks.CINNABAR_BRICK_WALL, Blocks.RED_NETHER_BRICK_WALL);

        addBlock(Blocks.CHISELED_CINNABAR, Blocks.RED_CONCRETE);

        addItem(Items.SULFUR_CUBE_BUCKET, Items.PUFFERFISH_BUCKET);
        addItem(Items.SULFUR_CUBE_SPAWN_EGG, Items.SLIME_SPAWN_EGG);
        addItem(Items.MUSIC_DISC_BOUNCE, Items.MUSIC_DISC_BLOCKS);
    }

    public static NbtMap convertBlock(NbtMap tag) {
        String replacement = BLOCK_MAPPINGS.get(tag.getString("name"));
        if (replacement != null) {
            if (CLEAR_BLOCK_STATES.contains(tag.getString("name"))) {
                return withoutStates(replacement);
            }

            return withId(tag, replacement);
        }
        return tag;
    }

    public static Map<Item, Item> convertItem() {
        return ITEM_MAPPINGS;
    }
}
