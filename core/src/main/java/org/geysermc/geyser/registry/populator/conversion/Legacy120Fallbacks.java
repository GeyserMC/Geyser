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
 * Item/block remaps for Bedrock 1.20.x clients receiving Java 26.2 / 1.21+ content.
 */
public final class Legacy120Fallbacks {

    private Legacy120Fallbacks() {
    }

    /**
     * Fallbacks for all Bedrock &lt; 1.21.0 (protocol &lt; 685): remap Tricky Trials content
     * and later Java items that have no Bedrock 1.20 definition.
     */
    public static Map<Item, Item> forPre685() {
        Map<Item, Item> map = new HashMap<>(Legacy121Fallbacks.forPre748());

        map.put(Items.MACE, Items.DIAMOND_SWORD);
        map.put(Items.BREEZE_ROD, Items.BLAZE_ROD);
        map.put(Items.WIND_CHARGE, Items.SNOWBALL);
        map.put(Items.HEAVY_CORE, Items.IRON_BLOCK);
        map.put(Items.CRAFTER, Items.CRAFTING_TABLE);
        map.put(Items.TRIAL_SPAWNER, Items.SPAWNER);
        map.put(Items.VAULT, Items.CHEST);
        map.put(Items.TRIAL_KEY, Items.ECHO_SHARD);
        map.put(Items.OMINOUS_TRIAL_KEY, Items.ECHO_SHARD);
        map.put(Items.OMINOUS_BOTTLE, Items.EXPERIENCE_BOTTLE);
        map.put(Items.BREEZE_SPAWN_EGG, Items.BLAZE_SPAWN_EGG);
        map.put(Items.BOGGED_SPAWN_EGG, Items.STRAY_SPAWN_EGG);
        map.put(Items.FLOW_BANNER_PATTERN, Items.CREEPER_BANNER_PATTERN);
        map.put(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        map.put(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        map.put(Items.FLOW_POTTERY_SHERD, Items.ANGLER_POTTERY_SHERD);
        map.put(Items.GUSTER_BANNER_PATTERN, Items.CREEPER_BANNER_PATTERN);
        map.put(Items.GUSTER_POTTERY_SHERD, Items.ANGLER_POTTERY_SHERD);
        map.put(Items.SCRAPE_POTTERY_SHERD, Items.ANGLER_POTTERY_SHERD);
        map.put(Items.MUSIC_DISC_CREATOR, Items.MUSIC_DISC_OTHERSIDE);
        map.put(Items.MUSIC_DISC_CREATOR_MUSIC_BOX, Items.MUSIC_DISC_OTHERSIDE);
        map.put(Items.MUSIC_DISC_PRECIPICE, Items.MUSIC_DISC_OTHERSIDE);
        map.put(Items.MUSIC_DISC_TEARS, Items.MUSIC_DISC_CAT);
        map.put(Items.MUSIC_DISC_LAVA_CHICKEN, Items.MUSIC_DISC_CAT);

        map.put(Items.CHISELED_TUFF, Items.CHISELED_DEEPSLATE);
        map.put(Items.CHISELED_TUFF_BRICKS, Items.CHISELED_DEEPSLATE);
        map.put(Items.POLISHED_TUFF, Items.POLISHED_DEEPSLATE);
        map.put(Items.POLISHED_TUFF_SLAB, Items.POLISHED_DEEPSLATE_SLAB);
        map.put(Items.POLISHED_TUFF_STAIRS, Items.POLISHED_DEEPSLATE_STAIRS);
        map.put(Items.POLISHED_TUFF_WALL, Items.POLISHED_DEEPSLATE_WALL);
        map.put(Items.TUFF_BRICKS, Items.DEEPSLATE_BRICKS);
        map.put(Items.TUFF_BRICK_SLAB, Items.DEEPSLATE_BRICK_SLAB);
        map.put(Items.TUFF_BRICK_STAIRS, Items.DEEPSLATE_BRICK_STAIRS);
        map.put(Items.TUFF_BRICK_WALL, Items.DEEPSLATE_BRICK_WALL);

        map.put(Items.CHISELED_COPPER, Items.COPPER_BLOCK);
        map.put(Items.EXPOSED_CHISELED_COPPER, Items.EXPOSED_COPPER);
        map.put(Items.WEATHERED_CHISELED_COPPER, Items.WEATHERED_COPPER);
        map.put(Items.OXIDIZED_CHISELED_COPPER, Items.OXIDIZED_COPPER);
        map.put(Items.WAXED_CHISELED_COPPER, Items.WAXED_COPPER_BLOCK);
        map.put(Items.WAXED_EXPOSED_CHISELED_COPPER, Items.WAXED_EXPOSED_COPPER);
        map.put(Items.WAXED_WEATHERED_CHISELED_COPPER, Items.WAXED_WEATHERED_COPPER);
        map.put(Items.WAXED_OXIDIZED_CHISELED_COPPER, Items.WAXED_OXIDIZED_COPPER);

        map.put(Items.COPPER_BULB, Items.COPPER_BLOCK);
        map.put(Items.EXPOSED_COPPER_BULB, Items.EXPOSED_COPPER);
        map.put(Items.WEATHERED_COPPER_BULB, Items.WEATHERED_COPPER);
        map.put(Items.OXIDIZED_COPPER_BULB, Items.OXIDIZED_COPPER);
        map.put(Items.WAXED_COPPER_BULB, Items.WAXED_COPPER_BLOCK);
        map.put(Items.WAXED_EXPOSED_COPPER_BULB, Items.WAXED_EXPOSED_COPPER);
        map.put(Items.WAXED_WEATHERED_COPPER_BULB, Items.WAXED_WEATHERED_COPPER);
        map.put(Items.WAXED_OXIDIZED_COPPER_BULB, Items.WAXED_OXIDIZED_COPPER);

        map.put(Items.COPPER_GRATE, Items.RAW_IRON_BLOCK);
        map.put(Items.EXPOSED_COPPER_GRATE, Items.RAW_IRON_BLOCK);
        map.put(Items.WEATHERED_COPPER_GRATE, Items.RAW_IRON_BLOCK);
        map.put(Items.OXIDIZED_COPPER_GRATE, Items.RAW_IRON_BLOCK);
        map.put(Items.WAXED_COPPER_GRATE, Items.RAW_IRON_BLOCK);
        map.put(Items.WAXED_EXPOSED_COPPER_GRATE, Items.RAW_IRON_BLOCK);
        map.put(Items.WAXED_WEATHERED_COPPER_GRATE, Items.RAW_IRON_BLOCK);
        map.put(Items.WAXED_OXIDIZED_COPPER_GRATE, Items.RAW_IRON_BLOCK);

        map.put(Items.COPPER_DOOR, Items.IRON_DOOR);
        map.put(Items.EXPOSED_COPPER_DOOR, Items.IRON_DOOR);
        map.put(Items.WEATHERED_COPPER_DOOR, Items.IRON_DOOR);
        map.put(Items.OXIDIZED_COPPER_DOOR, Items.IRON_DOOR);
        map.put(Items.WAXED_COPPER_DOOR, Items.IRON_DOOR);
        map.put(Items.WAXED_EXPOSED_COPPER_DOOR, Items.IRON_DOOR);
        map.put(Items.WAXED_WEATHERED_COPPER_DOOR, Items.IRON_DOOR);
        map.put(Items.WAXED_OXIDIZED_COPPER_DOOR, Items.IRON_DOOR);

        map.put(Items.COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);
        map.put(Items.EXPOSED_COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);
        map.put(Items.WEATHERED_COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);
        map.put(Items.OXIDIZED_COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);
        map.put(Items.WAXED_COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);
        map.put(Items.WAXED_EXPOSED_COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);
        map.put(Items.WAXED_WEATHERED_COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);
        map.put(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR, Items.IRON_TRAPDOOR);

        // Copper tools/armor (Bedrock ~1.21.100+) — use iron on 1.20.
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
        map.put(Items.COPPER_SPEAR, Items.IRON_SWORD);
        map.put(Items.COPPER_NAUTILUS_ARMOR, Items.IRON_HORSE_ARMOR);
        map.put(Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR);

        // Happy Ghast harnesses / chicken eggs / bundles (post-1.20).
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

        return map;
    }

    /**
     * Fallbacks for Bedrock &lt; 1.20.70: armadillo content absent.
     */
    public static Map<Item, Item> forPre662() {
        Map<Item, Item> map = forPre685();
        map.put(Items.ARMADILLO_SPAWN_EGG, Items.PIG_SPAWN_EGG);
        map.put(Items.ARMADILLO_SCUTE, Items.TURTLE_SCUTE);
        map.put(Items.WOLF_ARMOR, Items.LEATHER_HORSE_ARMOR);
        return map;
    }

    public static NbtMap remapBlockPre685(NbtMap tag) {
        // Includes dirt_type / sand_type unflatten for 1.21.0-era palettes.
        tag = Legacy121Fallbacks.remapBlockPre712(tag);
        tag = ConversionTrickyTrials.remapBlock(tag);
        return Conversion685_671.remapBlock(tag);
    }

    public static NbtMap remapBlockPre671(NbtMap tag) {
        return Conversion671_662.remapBlock(remapBlockPre685(tag));
    }

    public static NbtMap remapBlockPre662(NbtMap tag) {
        return Conversion662_649.remapBlock(remapBlockPre671(tag));
    }

    public static NbtMap remapBlockPre649(NbtMap tag) {
        // Conversion649_630 applies Conversion662_649 then trial_spawner→mob_spawner
        return Conversion649_630.remapBlock(remapBlockPre671(tag));
    }

    public static NbtMap remapBlockPre630(NbtMap tag) {
        tag = remapBlockPre649(tag);
        return Conversion630_622.remapBlock(tag);
    }

    /**
     * Floor remaps for 1.20.30 (chests still use facing_direction; furnaces already use cardinal).
     */
    public static NbtMap remapBlockPre622(NbtMap tag) {
        return Conversion622_618.remapBlock(remapBlockPre630(tag));
    }

    /**
     * Floor remaps for 1.20.0–1.20.10: color meta glass/terracotta/powder, slab top_slot_bit,
     * furnaces facing_direction, repeater direction, amethyst facing_direction.
     */
    public static NbtMap remapBlockPre618(NbtMap tag) {
        // Chests first (cardinal → facing_direction), then 1.20.30 reverse remaps.
        return Conversion618_594.remapBlock(remapBlockPre622(tag));
    }

    /**
     * Floor remaps for 1.20.0 only: concrete/shulker color meta + observer int facing.
     */
    public static NbtMap remapBlockPre594(NbtMap tag) {
        return Conversion594_589.remapBlock(remapBlockPre618(tag));
    }

    public static GeyserMappingItem remapItemPre685(Item item, GeyserMappingItem mapping) {
        mapping = Legacy121Fallbacks.remapItemPre712(item, mapping);
        return Conversion685_671.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre671(Item item, GeyserMappingItem mapping) {
        mapping = remapItemPre685(item, mapping);
        return Conversion671_662.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre662(Item item, GeyserMappingItem mapping) {
        mapping = remapItemPre671(item, mapping);
        return Conversion662_649.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre649(Item item, GeyserMappingItem mapping) {
        mapping = remapItemPre662(item, mapping);
        return Conversion649_630.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre630(Item item, GeyserMappingItem mapping) {
        mapping = remapItemPre649(item, mapping);
        return Conversion630_622.remapItem(item, mapping);
    }

    public static GeyserMappingItem remapItemPre622(Item item, GeyserMappingItem mapping) {
        return remapItemPre630(item, mapping);
    }

    /**
     * Items for 1.20.0–1.20.10: glass/pane/powder/terracotta still use color meta.
     */
    public static GeyserMappingItem remapItemPre618(Item item, GeyserMappingItem mapping) {
        mapping = remapItemPre622(item, mapping);
        return Conversion618_594.remapItem(item, mapping);
    }

    /**
     * Items for 1.20.0 only: concrete/shulker still use color meta.
     */
    public static GeyserMappingItem remapItemPre594(Item item, GeyserMappingItem mapping) {
        mapping = remapItemPre618(item, mapping);
        return Conversion594_589.remapItem(item, mapping);
    }
}
