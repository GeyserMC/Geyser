/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache.tags;

import org.geysermc.geyser.util.MinecraftKey;

/**
 * Lists vanilla item tags.
 */
@SuppressWarnings("unused")
public final class ItemTag {
    public static final Tag WOOL = register("wool");
    public static final Tag PLANKS = register("planks");
    public static final Tag STONE_BRICKS = register("stone_bricks");
    public static final Tag WOODEN_BUTTONS = register("wooden_buttons");
    public static final Tag STONE_BUTTONS = register("stone_buttons");
    public static final Tag BUTTONS = register("buttons");
    public static final Tag WOOL_CARPETS = register("wool_carpets");
    public static final Tag WOODEN_DOORS = register("wooden_doors");
    public static final Tag WOODEN_STAIRS = register("wooden_stairs");
    public static final Tag WOODEN_SLABS = register("wooden_slabs");
    public static final Tag WOODEN_FENCES = register("wooden_fences");
    public static final Tag FENCE_GATES = register("fence_gates");
    public static final Tag WOODEN_PRESSURE_PLATES = register("wooden_pressure_plates");
    public static final Tag WOODEN_TRAPDOORS = register("wooden_trapdoors");
    public static final Tag DOORS = register("doors");
    public static final Tag SAPLINGS = register("saplings");
    public static final Tag LOGS_THAT_BURN = register("logs_that_burn");
    public static final Tag LOGS = register("logs");
    public static final Tag DARK_OAK_LOGS = register("dark_oak_logs");
    public static final Tag OAK_LOGS = register("oak_logs");
    public static final Tag BIRCH_LOGS = register("birch_logs");
    public static final Tag ACACIA_LOGS = register("acacia_logs");
    public static final Tag CHERRY_LOGS = register("cherry_logs");
    public static final Tag JUNGLE_LOGS = register("jungle_logs");
    public static final Tag SPRUCE_LOGS = register("spruce_logs");
    public static final Tag MANGROVE_LOGS = register("mangrove_logs");
    public static final Tag CRIMSON_STEMS = register("crimson_stems");
    public static final Tag WARPED_STEMS = register("warped_stems");
    public static final Tag BAMBOO_BLOCKS = register("bamboo_blocks");
    public static final Tag WART_BLOCKS = register("wart_blocks");
    public static final Tag BANNERS = register("banners");
    public static final Tag SAND = register("sand");
    public static final Tag SMELTS_TO_GLASS = register("smelts_to_glass");
    public static final Tag STAIRS = register("stairs");
    public static final Tag SLABS = register("slabs");
    public static final Tag WALLS = register("walls");
    public static final Tag ANVIL = register("anvil");
    public static final Tag RAILS = register("rails");
    public static final Tag LEAVES = register("leaves");
    public static final Tag TRAPDOORS = register("trapdoors");
    public static final Tag SMALL_FLOWERS = register("small_flowers");
    public static final Tag BEDS = register("beds");
    public static final Tag FENCES = register("fences");
    public static final Tag TALL_FLOWERS = register("tall_flowers");
    public static final Tag FLOWERS = register("flowers");
    public static final Tag PIGLIN_REPELLENTS = register("piglin_repellents");
    public static final Tag PIGLIN_LOVED = register("piglin_loved");
    public static final Tag IGNORED_BY_PIGLIN_BABIES = register("ignored_by_piglin_babies");
    public static final Tag MEAT = register("meat");
    public static final Tag SNIFFER_FOOD = register("sniffer_food");
    public static final Tag PIGLIN_FOOD = register("piglin_food");
    public static final Tag FOX_FOOD = register("fox_food");
    public static final Tag COW_FOOD = register("cow_food");
    public static final Tag GOAT_FOOD = register("goat_food");
    public static final Tag SHEEP_FOOD = register("sheep_food");
    public static final Tag WOLF_FOOD = register("wolf_food");
    public static final Tag CAT_FOOD = register("cat_food");
    public static final Tag HORSE_FOOD = register("horse_food");
    public static final Tag HORSE_TEMPT_ITEMS = register("horse_tempt_items");
    public static final Tag CAMEL_FOOD = register("camel_food");
    public static final Tag ARMADILLO_FOOD = register("armadillo_food");
    public static final Tag BEE_FOOD = register("bee_food");
    public static final Tag CHICKEN_FOOD = register("chicken_food");
    public static final Tag FROG_FOOD = register("frog_food");
    public static final Tag HOGLIN_FOOD = register("hoglin_food");
    public static final Tag LLAMA_FOOD = register("llama_food");
    public static final Tag LLAMA_TEMPT_ITEMS = register("llama_tempt_items");
    public static final Tag OCELOT_FOOD = register("ocelot_food");
    public static final Tag PANDA_FOOD = register("panda_food");
    public static final Tag PIG_FOOD = register("pig_food");
    public static final Tag RABBIT_FOOD = register("rabbit_food");
    public static final Tag STRIDER_FOOD = register("strider_food");
    public static final Tag STRIDER_TEMPT_ITEMS = register("strider_tempt_items");
    public static final Tag TURTLE_FOOD = register("turtle_food");
    public static final Tag PARROT_FOOD = register("parrot_food");
    public static final Tag PARROT_POISONOUS_FOOD = register("parrot_poisonous_food");
    public static final Tag AXOLOTL_FOOD = register("axolotl_food");
    public static final Tag GOLD_ORES = register("gold_ores");
    public static final Tag IRON_ORES = register("iron_ores");
    public static final Tag DIAMOND_ORES = register("diamond_ores");
    public static final Tag REDSTONE_ORES = register("redstone_ores");
    public static final Tag LAPIS_ORES = register("lapis_ores");
    public static final Tag COAL_ORES = register("coal_ores");
    public static final Tag EMERALD_ORES = register("emerald_ores");
    public static final Tag COPPER_ORES = register("copper_ores");
    public static final Tag NON_FLAMMABLE_WOOD = register("non_flammable_wood");
    public static final Tag SOUL_FIRE_BASE_BLOCKS = register("soul_fire_base_blocks");
    public static final Tag CANDLES = register("candles");
    public static final Tag DIRT = register("dirt");
    public static final Tag TERRACOTTA = register("terracotta");
    public static final Tag COMPLETES_FIND_TREE_TUTORIAL = register("completes_find_tree_tutorial");
    public static final Tag BOATS = register("boats");
    public static final Tag CHEST_BOATS = register("chest_boats");
    public static final Tag FISHES = register("fishes");
    public static final Tag SIGNS = register("signs");
    public static final Tag CREEPER_DROP_MUSIC_DISCS = register("creeper_drop_music_discs");
    public static final Tag COALS = register("coals");
    public static final Tag ARROWS = register("arrows");
    public static final Tag LECTERN_BOOKS = register("lectern_books");
    public static final Tag BOOKSHELF_BOOKS = register("bookshelf_books");
    public static final Tag BEACON_PAYMENT_ITEMS = register("beacon_payment_items");
    public static final Tag STONE_TOOL_MATERIALS = register("stone_tool_materials");
    public static final Tag STONE_CRAFTING_MATERIALS = register("stone_crafting_materials");
    public static final Tag FREEZE_IMMUNE_WEARABLES = register("freeze_immune_wearables");
    public static final Tag DAMPENS_VIBRATIONS = register("dampens_vibrations");
    public static final Tag CLUSTER_MAX_HARVESTABLES = register("cluster_max_harvestables");
    public static final Tag COMPASSES = register("compasses");
    public static final Tag HANGING_SIGNS = register("hanging_signs");
    public static final Tag CREEPER_IGNITERS = register("creeper_igniters");
    public static final Tag NOTEBLOCK_TOP_INSTRUMENTS = register("noteblock_top_instruments");
    public static final Tag FOOT_ARMOR = register("foot_armor");
    public static final Tag LEG_ARMOR = register("leg_armor");
    public static final Tag CHEST_ARMOR = register("chest_armor");
    public static final Tag HEAD_ARMOR = register("head_armor");
    public static final Tag SKULLS = register("skulls");
    public static final Tag TRIMMABLE_ARMOR = register("trimmable_armor");
    public static final Tag TRIM_MATERIALS = register("trim_materials");
    public static final Tag TRIM_TEMPLATES = register("trim_templates");
    public static final Tag DECORATED_POT_SHERDS = register("decorated_pot_sherds");
    public static final Tag DECORATED_POT_INGREDIENTS = register("decorated_pot_ingredients");
    public static final Tag SWORDS = register("swords");
    public static final Tag AXES = register("axes");
    public static final Tag HOES = register("hoes");
    public static final Tag PICKAXES = register("pickaxes");
    public static final Tag SHOVELS = register("shovels");
    public static final Tag BREAKS_DECORATED_POTS = register("breaks_decorated_pots");
    public static final Tag VILLAGER_PLANTABLE_SEEDS = register("villager_plantable_seeds");
    public static final Tag DYEABLE = register("dyeable");
    public static final Tag ENCHANTABLE_FOOT_ARMOR = register("enchantable/foot_armor");
    public static final Tag ENCHANTABLE_LEG_ARMOR = register("enchantable/leg_armor");
    public static final Tag ENCHANTABLE_CHEST_ARMOR = register("enchantable/chest_armor");
    public static final Tag ENCHANTABLE_HEAD_ARMOR = register("enchantable/head_armor");
    public static final Tag ENCHANTABLE_ARMOR = register("enchantable/armor");
    public static final Tag ENCHANTABLE_SWORD = register("enchantable/sword");
    public static final Tag ENCHANTABLE_FIRE_ASPECT = register("enchantable/fire_aspect");
    public static final Tag ENCHANTABLE_SHARP_WEAPON = register("enchantable/sharp_weapon");
    public static final Tag ENCHANTABLE_WEAPON = register("enchantable/weapon");
    public static final Tag ENCHANTABLE_MINING = register("enchantable/mining");
    public static final Tag ENCHANTABLE_MINING_LOOT = register("enchantable/mining_loot");
    public static final Tag ENCHANTABLE_FISHING = register("enchantable/fishing");
    public static final Tag ENCHANTABLE_TRIDENT = register("enchantable/trident");
    public static final Tag ENCHANTABLE_DURABILITY = register("enchantable/durability");
    public static final Tag ENCHANTABLE_BOW = register("enchantable/bow");
    public static final Tag ENCHANTABLE_EQUIPPABLE = register("enchantable/equippable");
    public static final Tag ENCHANTABLE_CROSSBOW = register("enchantable/crossbow");
    public static final Tag ENCHANTABLE_VANISHING = register("enchantable/vanishing");
    public static final Tag ENCHANTABLE_MACE = register("enchantable/mace");

    private ItemTag() {}

    private static Tag register(String name) {
        return TagRegistry.ITEM.registerVanillaTag(MinecraftKey.key(name));
    }

    public static void init() {
        // no-op
    }
}
