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
import org.geysermc.geyser.registry.type.GeyserMappingItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Cumulative item/block remaps for Bedrock 1.21.x clients receiving Java 26.2 content.
 */
public final class Legacy121Fallbacks {

    private Legacy121Fallbacks() {
    }

    /**
     * Fallbacks for protocols at/above 1.21.110-ish (844+) but below modern 26.x extras:
     * spears, nautilus gear, camel husk / parched eggs.
     */
    public static Map<Item, Item> forPre26Extras() {
        Map<Item, Item> map = new HashMap<>(GoldenDandelionConverter.convertItem());
        map.put(Items.WOODEN_SPEAR, Items.WOODEN_SWORD);
        map.put(Items.STONE_SPEAR, Items.STONE_SWORD);
        map.put(Items.COPPER_SPEAR, Items.COPPER_SWORD);
        map.put(Items.IRON_SPEAR, Items.IRON_SWORD);
        map.put(Items.GOLDEN_SPEAR, Items.GOLDEN_SWORD);
        map.put(Items.DIAMOND_SPEAR, Items.DIAMOND_SWORD);
        map.put(Items.NETHERITE_SPEAR, Items.NETHERITE_SWORD);
        map.put(Items.COPPER_NAUTILUS_ARMOR, Items.COPPER_HORSE_ARMOR);
        map.put(Items.IRON_NAUTILUS_ARMOR, Items.IRON_HORSE_ARMOR);
        map.put(Items.GOLDEN_NAUTILUS_ARMOR, Items.GOLDEN_HORSE_ARMOR);
        map.put(Items.DIAMOND_NAUTILUS_ARMOR, Items.DIAMOND_HORSE_ARMOR);
        map.put(Items.NETHERITE_NAUTILUS_ARMOR, Items.DIAMOND_HORSE_ARMOR);
        map.put(Items.NETHERITE_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR);
        map.put(Items.NAUTILUS_SPAWN_EGG, Items.PUFFERFISH_SPAWN_EGG);
        map.put(Items.ZOMBIE_NAUTILUS_SPAWN_EGG, Items.PUFFERFISH_SPAWN_EGG);
        map.put(Items.CAMEL_HUSK_SPAWN_EGG, Items.CAMEL_SPAWN_EGG);
        map.put(Items.PARCHED_SPAWN_EGG, Items.SKELETON_SPAWN_EGG);
        return map;
    }

    /**
     * Fallbacks for protocols below 1.21.110 (pre-844): also shelves, copper décor, etc.
     */
    public static Map<Item, Item> forPre844() {
        Map<Item, Item> map = forPre26Extras();
        map.put(Items.ACACIA_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.BAMBOO_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.BIRCH_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.CHERRY_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.CRIMSON_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.DARK_OAK_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.JUNGLE_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.MANGROVE_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.OAK_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.PALE_OAK_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.SPRUCE_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.WARPED_SHELF, Items.CHISELED_BOOKSHELF);
        map.put(Items.COPPER_BARS, Items.IRON_BARS);
        map.put(Items.EXPOSED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WEATHERED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.OXIDIZED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_EXPOSED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_WEATHERED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.WAXED_OXIDIZED_COPPER_BARS, Items.IRON_BARS);
        map.put(Items.COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.EXPOSED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WEATHERED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.OXIDIZED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_EXPOSED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_WEATHERED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, Items.ARMOR_STAND);
        map.put(Items.COPPER_LANTERN, Items.LANTERN);
        map.put(Items.EXPOSED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WEATHERED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.OXIDIZED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_EXPOSED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_WEATHERED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.WAXED_OXIDIZED_COPPER_LANTERN, Items.LANTERN);
        map.put(Items.EXPOSED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WEATHERED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.OXIDIZED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_EXPOSED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_WEATHERED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.WAXED_OXIDIZED_LIGHTNING_ROD, Items.LIGHTNING_ROD);
        map.put(Items.COPPER_TORCH, Items.TORCH);
        map.put(Items.COPPER_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR);
        // forPre26Extras targets copper_horse_armor, which only exists from 1.21.110.
        map.put(Items.COPPER_NAUTILUS_ARMOR, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.COPPER_CHEST, Items.CHEST);
        map.put(Items.EXPOSED_COPPER_CHEST, Items.CHEST);
        map.put(Items.WEATHERED_COPPER_CHEST, Items.CHEST);
        map.put(Items.OXIDIZED_COPPER_CHEST, Items.CHEST);
        map.put(Items.WAXED_COPPER_CHEST, Items.CHEST);
        map.put(Items.WAXED_EXPOSED_COPPER_CHEST, Items.CHEST);
        map.put(Items.WAXED_WEATHERED_COPPER_CHEST, Items.CHEST);
        map.put(Items.WAXED_OXIDIZED_COPPER_CHEST, Items.CHEST);
        map.put(Items.COPPER_GOLEM_SPAWN_EGG, Items.IRON_GOLEM_SPAWN_EGG);
        return map;
    }

    /**
     * Fallbacks for protocols below 1.21.80 (pre-800).
     */
    public static Map<Item, Item> forPre800() {
        Map<Item, Item> map = forPre844();
        map.put(Items.DRIED_GHAST, Items.SOUL_SAND);
        map.put(Items.BUSH, Items.FERN);
        map.put(Items.FIREFLY_BUSH, Items.DEAD_BUSH);
        map.put(Items.TALL_DRY_GRASS, Items.SHORT_GRASS);
        map.put(Items.SHORT_DRY_GRASS, Items.SHORT_GRASS);
        map.put(Items.CACTUS_FLOWER, Items.CACTUS);
        map.put(Items.LEAF_LITTER, Items.OAK_LEAVES);
        map.put(Items.WILDFLOWERS, Items.OXEYE_DAISY);
        map.put(Items.HAPPY_GHAST_SPAWN_EGG, Items.GHAST_SPAWN_EGG);
        putHarnessEggCopperFallbacks(map);
        return map;
    }

    /**
     * Fallbacks for 1.21.80 (harnesses / eggs exist; copper tools and later discs do not).
     */
    public static Map<Item, Item> forPre818() {
        Map<Item, Item> map = forPre844();
        putCopperToolFallbacks(map);
        map.put(Items.MUSIC_DISC_TEARS, Items.MUSIC_DISC_CAT);
        map.put(Items.MUSIC_DISC_LAVA_CHICKEN, Items.MUSIC_DISC_CAT);
        return map;
    }

    /**
     * Fallbacks for 1.21.90–1.21.93 (harnesses exist; copper tools / lava chicken do not).
     */
    public static Map<Item, Item> forPre827() {
        Map<Item, Item> map = forPre844();
        putCopperToolFallbacks(map);
        map.put(Items.MUSIC_DISC_LAVA_CHICKEN, Items.MUSIC_DISC_CAT);
        return map;
    }

    /**
     * Fallbacks for protocols below 1.21.50 (pre-766): Pale Garden / resin → birch / red sandstone.
     */
    public static Map<Item, Item> forPre748() {
        Map<Item, Item> map = forPre800();
        map.put(Items.PALE_OAK_PLANKS, Items.BIRCH_PLANKS);
        map.put(Items.PALE_OAK_SAPLING, Items.BIRCH_SAPLING);
        map.put(Items.PALE_OAK_LOG, Items.BIRCH_LOG);
        map.put(Items.STRIPPED_PALE_OAK_LOG, Items.STRIPPED_BIRCH_LOG);
        map.put(Items.STRIPPED_PALE_OAK_WOOD, Items.STRIPPED_BIRCH_WOOD);
        map.put(Items.PALE_OAK_WOOD, Items.BIRCH_WOOD);
        map.put(Items.PALE_OAK_LEAVES, Items.BIRCH_LEAVES);
        map.put(Items.PALE_OAK_SLAB, Items.BIRCH_SLAB);
        map.put(Items.PALE_OAK_STAIRS, Items.BIRCH_STAIRS);
        map.put(Items.PALE_OAK_FENCE, Items.BIRCH_FENCE);
        map.put(Items.PALE_OAK_FENCE_GATE, Items.BIRCH_FENCE_GATE);
        map.put(Items.PALE_OAK_DOOR, Items.BIRCH_DOOR);
        map.put(Items.PALE_OAK_TRAPDOOR, Items.BIRCH_TRAPDOOR);
        map.put(Items.PALE_OAK_BUTTON, Items.BIRCH_BUTTON);
        map.put(Items.PALE_OAK_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE);
        map.put(Items.PALE_OAK_SIGN, Items.BIRCH_SIGN);
        map.put(Items.PALE_OAK_HANGING_SIGN, Items.BIRCH_HANGING_SIGN);
        map.put(Items.PALE_OAK_BOAT, Items.BIRCH_BOAT);
        map.put(Items.PALE_OAK_CHEST_BOAT, Items.BIRCH_CHEST_BOAT);
        map.put(Items.PALE_MOSS_BLOCK, Items.MOSS_BLOCK);
        map.put(Items.PALE_MOSS_CARPET, Items.MOSS_CARPET);
        map.put(Items.PALE_HANGING_MOSS, Items.HANGING_ROOTS);
        map.put(Items.OPEN_EYEBLOSSOM, Items.OXEYE_DAISY);
        map.put(Items.CLOSED_EYEBLOSSOM, Items.WHITE_TULIP);
        map.put(Items.RESIN_CLUMP, Items.HONEYCOMB);
        map.put(Items.RESIN_BLOCK, Items.RED_SANDSTONE);
        map.put(Items.RESIN_BRICKS, Items.CUT_RED_SANDSTONE);
        map.put(Items.RESIN_BRICK_STAIRS, Items.RED_SANDSTONE_STAIRS);
        map.put(Items.RESIN_BRICK_SLAB, Items.RED_SANDSTONE_SLAB);
        map.put(Items.RESIN_BRICK_WALL, Items.RED_SANDSTONE_WALL);
        map.put(Items.CHISELED_RESIN_BRICKS, Items.CHISELED_RED_SANDSTONE);
        map.put(Items.RESIN_BRICK, Items.BRICK);
        map.put(Items.CREAKING_HEART, Items.CHISELED_POLISHED_BLACKSTONE);
        map.put(Items.CREAKING_SPAWN_EGG, Items.WARDEN_SPAWN_EGG);
        // Bundles arrive in Bedrock 1.21.50 — remap for older 1.21.x palettes only.
        putBundleFallbacks(map);
        return map;
    }

    private static void putHarnessEggCopperFallbacks(Map<Item, Item> map) {
        map.put(Items.WHITE_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.ORANGE_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.MAGENTA_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.LIGHT_BLUE_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.YELLOW_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.LIME_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.PINK_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.GRAY_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.LIGHT_GRAY_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.CYAN_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.PURPLE_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.BLUE_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.BROWN_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.GREEN_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.RED_HARNESS, Items.LEATHER_HORSE_ARMOR);
        map.put(Items.BLACK_HARNESS, Items.LEATHER_HORSE_ARMOR);

        map.put(Items.BLUE_EGG, Items.EGG);
        map.put(Items.BROWN_EGG, Items.EGG);
        map.put(Items.MUSIC_DISC_TEARS, Items.MUSIC_DISC_CAT);
        map.put(Items.MUSIC_DISC_LAVA_CHICKEN, Items.MUSIC_DISC_CAT);

        putCopperToolFallbacks(map);
    }

    private static void putCopperToolFallbacks(Map<Item, Item> map) {
        map.put(Items.COPPER_SWORD, Items.IRON_SWORD);
        map.put(Items.COPPER_SHOVEL, Items.IRON_SHOVEL);
        map.put(Items.COPPER_PICKAXE, Items.IRON_PICKAXE);
        map.put(Items.COPPER_AXE, Items.IRON_AXE);
        map.put(Items.COPPER_HOE, Items.IRON_HOE);
        map.put(Items.COPPER_HELMET, Items.IRON_HELMET);
        map.put(Items.COPPER_CHESTPLATE, Items.IRON_CHESTPLATE);
        map.put(Items.COPPER_LEGGINGS, Items.IRON_LEGGINGS);
        map.put(Items.COPPER_BOOTS, Items.IRON_BOOTS);
        map.put(Items.COPPER_NUGGET, Items.IRON_NUGGET);
        // Override forPre26Extras targets that do not exist yet on these palettes.
        map.put(Items.COPPER_SPEAR, Items.IRON_SWORD);
        map.put(Items.COPPER_NAUTILUS_ARMOR, Items.IRON_HORSE_ARMOR);
        map.put(Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR);
    }

    private static void putBundleFallbacks(Map<Item, Item> map) {
        map.put(Items.BUNDLE, Items.CHEST);
        map.put(Items.WHITE_BUNDLE, Items.CHEST);
        map.put(Items.ORANGE_BUNDLE, Items.CHEST);
        map.put(Items.MAGENTA_BUNDLE, Items.CHEST);
        map.put(Items.LIGHT_BLUE_BUNDLE, Items.CHEST);
        map.put(Items.YELLOW_BUNDLE, Items.CHEST);
        map.put(Items.LIME_BUNDLE, Items.CHEST);
        map.put(Items.PINK_BUNDLE, Items.CHEST);
        map.put(Items.GRAY_BUNDLE, Items.CHEST);
        map.put(Items.LIGHT_GRAY_BUNDLE, Items.CHEST);
        map.put(Items.CYAN_BUNDLE, Items.CHEST);
        map.put(Items.PURPLE_BUNDLE, Items.CHEST);
        map.put(Items.BLUE_BUNDLE, Items.CHEST);
        map.put(Items.BROWN_BUNDLE, Items.CHEST);
        map.put(Items.GREEN_BUNDLE, Items.CHEST);
        map.put(Items.RED_BUNDLE, Items.CHEST);
        map.put(Items.BLACK_BUNDLE, Items.CHEST);
    }

    public static NbtMap remapBlockPre26(NbtMap tag) {
        return GoldenDandelionConverter.convertBlock(tag);
    }

    public static NbtMap remapBlockPre844(NbtMap tag) {
        return Conversion827_819.remapBlock(GoldenDandelionConverter.convertBlock(tag));
    }

    public static NbtMap remapBlockPre800(NbtMap tag) {
        return Conversion800_786.remapBlock(remapBlockPre844(tag));
    }

    public static NbtMap remapBlockPre776(NbtMap tag) {
        return Conversion786_776.remapBlock(remapBlockPre800(tag));
    }

    public static NbtMap remapBlockPre766(NbtMap tag) {
        // Conversion776_766 already applies Conversion786_776 first.
        return Conversion776_766.remapBlock(remapBlockPre800(tag));
    }

    public static NbtMap remapBlockPre748(NbtMap tag) {
        // Conversion766_748 already applies 776→786; only prepend Java-26 + copper/shelves + dried_ghast.
        return Conversion766_748.remapBlock(remapBlockPre800(tag));
    }

    /**
     * Bedrock 1.21.30 still requires stripped_bit on cherry/mangrove wood.
     */
    public static NbtMap remapBlockPre748Stripped(NbtMap tag) {
        return Conversion748_729.remapBlock(remapBlockPre748(tag));
    }

    /**
     * Bedrock 1.21.20 still uses legacy wall/sponge/purpur/tnt states.
     */
    public static NbtMap remapBlockPre729(NbtMap tag) {
        // Conversion729_712 already applies Conversion748_729 (stripped_bit) first.
        return Conversion729_712.remapBlock(remapBlockPre748(tag));
    }

    /**
     * Bedrock 1.21.0 still uses dirt_type / sand_type and other pre-flatten states.
     */
    public static NbtMap remapBlockPre712(NbtMap tag) {
        // Conversion712_685 already applies Conversion729_712 (and thus 748 stripped_bit) first.
        return Conversion712_685.remapBlock(remapBlockPre748(tag));
    }

    public static GeyserMappingItem remapItemPre844(Item item, GeyserMappingItem mapping) {
        return Conversion844_827.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre748(Item item, GeyserMappingItem mapping) {
        mapping = remapItemPre844(item, mapping);
        return Conversion748_729.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre729(Item item, GeyserMappingItem mapping) {
        // Conversion729_712 already applies Conversion748_729 first.
        mapping = remapItemPre844(item, mapping);
        return Conversion729_712.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre712(Item item, GeyserMappingItem mapping) {
        // Conversion712_685 already applies Conversion729_712 (and thus 748) first.
        mapping = remapItemPre844(item, mapping);
        return Conversion712_685.remapItem(item, mapping);
    }
}
