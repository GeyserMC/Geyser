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

import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.util.MinecraftKey;

/**
 * Lists vanilla item tags.
 */
@SuppressWarnings("unused")
public final class ItemTag {
    public static final Tag<Item> WOOL = create("wool");
    public static final Tag<Item> PLANKS = create("planks");
    public static final Tag<Item> STONE_BRICKS = create("stone_bricks");
    public static final Tag<Item> WOODEN_BUTTONS = create("wooden_buttons");
    public static final Tag<Item> STONE_BUTTONS = create("stone_buttons");
    public static final Tag<Item> BUTTONS = create("buttons");
    public static final Tag<Item> WOOL_CARPETS = create("wool_carpets");
    public static final Tag<Item> WOODEN_DOORS = create("wooden_doors");
    public static final Tag<Item> WOODEN_STAIRS = create("wooden_stairs");
    public static final Tag<Item> WOODEN_SLABS = create("wooden_slabs");
    public static final Tag<Item> WOODEN_FENCES = create("wooden_fences");
    public static final Tag<Item> FENCE_GATES = create("fence_gates");
    public static final Tag<Item> WOODEN_PRESSURE_PLATES = create("wooden_pressure_plates");
    public static final Tag<Item> WOODEN_TRAPDOORS = create("wooden_trapdoors");
    public static final Tag<Item> DOORS = create("doors");
    public static final Tag<Item> SAPLINGS = create("saplings");
    public static final Tag<Item> LOGS_THAT_BURN = create("logs_that_burn");
    public static final Tag<Item> LOGS = create("logs");
    public static final Tag<Item> DARK_OAK_LOGS = create("dark_oak_logs");
    public static final Tag<Item> OAK_LOGS = create("oak_logs");
    public static final Tag<Item> BIRCH_LOGS = create("birch_logs");
    public static final Tag<Item> ACACIA_LOGS = create("acacia_logs");
    public static final Tag<Item> CHERRY_LOGS = create("cherry_logs");
    public static final Tag<Item> JUNGLE_LOGS = create("jungle_logs");
    public static final Tag<Item> SPRUCE_LOGS = create("spruce_logs");
    public static final Tag<Item> MANGROVE_LOGS = create("mangrove_logs");
    public static final Tag<Item> CRIMSON_STEMS = create("crimson_stems");
    public static final Tag<Item> WARPED_STEMS = create("warped_stems");
    public static final Tag<Item> BAMBOO_BLOCKS = create("bamboo_blocks");
    public static final Tag<Item> WART_BLOCKS = create("wart_blocks");
    public static final Tag<Item> BANNERS = create("banners");
    public static final Tag<Item> SAND = create("sand");
    public static final Tag<Item> SMELTS_TO_GLASS = create("smelts_to_glass");
    public static final Tag<Item> STAIRS = create("stairs");
    public static final Tag<Item> SLABS = create("slabs");
    public static final Tag<Item> WALLS = create("walls");
    public static final Tag<Item> ANVIL = create("anvil");
    public static final Tag<Item> RAILS = create("rails");
    public static final Tag<Item> LEAVES = create("leaves");
    public static final Tag<Item> TRAPDOORS = create("trapdoors");
    public static final Tag<Item> SMALL_FLOWERS = create("small_flowers");
    public static final Tag<Item> BEDS = create("beds");
    public static final Tag<Item> FENCES = create("fences");
    public static final Tag<Item> TALL_FLOWERS = create("tall_flowers");
    public static final Tag<Item> FLOWERS = create("flowers");
    public static final Tag<Item> PIGLIN_REPELLENTS = create("piglin_repellents");
    public static final Tag<Item> PIGLIN_LOVED = create("piglin_loved");
    public static final Tag<Item> IGNORED_BY_PIGLIN_BABIES = create("ignored_by_piglin_babies");
    public static final Tag<Item> MEAT = create("meat");
    public static final Tag<Item> SNIFFER_FOOD = create("sniffer_food");
    public static final Tag<Item> PIGLIN_FOOD = create("piglin_food");
    public static final Tag<Item> FOX_FOOD = create("fox_food");
    public static final Tag<Item> COW_FOOD = create("cow_food");
    public static final Tag<Item> GOAT_FOOD = create("goat_food");
    public static final Tag<Item> SHEEP_FOOD = create("sheep_food");
    public static final Tag<Item> WOLF_FOOD = create("wolf_food");
    public static final Tag<Item> CAT_FOOD = create("cat_food");
    public static final Tag<Item> HORSE_FOOD = create("horse_food");
    public static final Tag<Item> HORSE_TEMPT_ITEMS = create("horse_tempt_items");
    public static final Tag<Item> CAMEL_FOOD = create("camel_food");
    public static final Tag<Item> ARMADILLO_FOOD = create("armadillo_food");
    public static final Tag<Item> BEE_FOOD = create("bee_food");
    public static final Tag<Item> CHICKEN_FOOD = create("chicken_food");
    public static final Tag<Item> FROG_FOOD = create("frog_food");
    public static final Tag<Item> HOGLIN_FOOD = create("hoglin_food");
    public static final Tag<Item> LLAMA_FOOD = create("llama_food");
    public static final Tag<Item> LLAMA_TEMPT_ITEMS = create("llama_tempt_items");
    public static final Tag<Item> OCELOT_FOOD = create("ocelot_food");
    public static final Tag<Item> PANDA_FOOD = create("panda_food");
    public static final Tag<Item> PIG_FOOD = create("pig_food");
    public static final Tag<Item> RABBIT_FOOD = create("rabbit_food");
    public static final Tag<Item> STRIDER_FOOD = create("strider_food");
    public static final Tag<Item> STRIDER_TEMPT_ITEMS = create("strider_tempt_items");
    public static final Tag<Item> TURTLE_FOOD = create("turtle_food");
    public static final Tag<Item> PARROT_FOOD = create("parrot_food");
    public static final Tag<Item> PARROT_POISONOUS_FOOD = create("parrot_poisonous_food");
    public static final Tag<Item> AXOLOTL_FOOD = create("axolotl_food");
    public static final Tag<Item> GOLD_ORES = create("gold_ores");
    public static final Tag<Item> IRON_ORES = create("iron_ores");
    public static final Tag<Item> DIAMOND_ORES = create("diamond_ores");
    public static final Tag<Item> REDSTONE_ORES = create("redstone_ores");
    public static final Tag<Item> LAPIS_ORES = create("lapis_ores");
    public static final Tag<Item> COAL_ORES = create("coal_ores");
    public static final Tag<Item> EMERALD_ORES = create("emerald_ores");
    public static final Tag<Item> COPPER_ORES = create("copper_ores");
    public static final Tag<Item> NON_FLAMMABLE_WOOD = create("non_flammable_wood");
    public static final Tag<Item> SOUL_FIRE_BASE_BLOCKS = create("soul_fire_base_blocks");
    public static final Tag<Item> CANDLES = create("candles");
    public static final Tag<Item> DIRT = create("dirt");
    public static final Tag<Item> TERRACOTTA = create("terracotta");
    public static final Tag<Item> COMPLETES_FIND_TREE_TUTORIAL = create("completes_find_tree_tutorial");
    public static final Tag<Item> BOATS = create("boats");
    public static final Tag<Item> CHEST_BOATS = create("chest_boats");
    public static final Tag<Item> FISHES = create("fishes");
    public static final Tag<Item> SIGNS = create("signs");
    public static final Tag<Item> CREEPER_DROP_MUSIC_DISCS = create("creeper_drop_music_discs");
    public static final Tag<Item> COALS = create("coals");
    public static final Tag<Item> ARROWS = create("arrows");
    public static final Tag<Item> LECTERN_BOOKS = create("lectern_books");
    public static final Tag<Item> BOOKSHELF_BOOKS = create("bookshelf_books");
    public static final Tag<Item> BEACON_PAYMENT_ITEMS = create("beacon_payment_items");
    public static final Tag<Item> STONE_TOOL_MATERIALS = create("stone_tool_materials");
    public static final Tag<Item> STONE_CRAFTING_MATERIALS = create("stone_crafting_materials");
    public static final Tag<Item> FREEZE_IMMUNE_WEARABLES = create("freeze_immune_wearables");
    public static final Tag<Item> DAMPENS_VIBRATIONS = create("dampens_vibrations");
    public static final Tag<Item> CLUSTER_MAX_HARVESTABLES = create("cluster_max_harvestables");
    public static final Tag<Item> COMPASSES = create("compasses");
    public static final Tag<Item> HANGING_SIGNS = create("hanging_signs");
    public static final Tag<Item> CREEPER_IGNITERS = create("creeper_igniters");
    public static final Tag<Item> NOTEBLOCK_TOP_INSTRUMENTS = create("noteblock_top_instruments");
    public static final Tag<Item> FOOT_ARMOR = create("foot_armor");
    public static final Tag<Item> LEG_ARMOR = create("leg_armor");
    public static final Tag<Item> CHEST_ARMOR = create("chest_armor");
    public static final Tag<Item> HEAD_ARMOR = create("head_armor");
    public static final Tag<Item> SKULLS = create("skulls");
    public static final Tag<Item> TRIMMABLE_ARMOR = create("trimmable_armor");
    public static final Tag<Item> TRIM_MATERIALS = create("trim_materials");
    public static final Tag<Item> TRIM_TEMPLATES = create("trim_templates");
    public static final Tag<Item> DECORATED_POT_SHERDS = create("decorated_pot_sherds");
    public static final Tag<Item> DECORATED_POT_INGREDIENTS = create("decorated_pot_ingredients");
    public static final Tag<Item> SWORDS = create("swords");
    public static final Tag<Item> AXES = create("axes");
    public static final Tag<Item> HOES = create("hoes");
    public static final Tag<Item> PICKAXES = create("pickaxes");
    public static final Tag<Item> SHOVELS = create("shovels");
    public static final Tag<Item> BREAKS_DECORATED_POTS = create("breaks_decorated_pots");
    public static final Tag<Item> VILLAGER_PLANTABLE_SEEDS = create("villager_plantable_seeds");
    public static final Tag<Item> DYEABLE = create("dyeable");
    public static final Tag<Item> ENCHANTABLE_FOOT_ARMOR = create("enchantable/foot_armor");
    public static final Tag<Item> ENCHANTABLE_LEG_ARMOR = create("enchantable/leg_armor");
    public static final Tag<Item> ENCHANTABLE_CHEST_ARMOR = create("enchantable/chest_armor");
    public static final Tag<Item> ENCHANTABLE_HEAD_ARMOR = create("enchantable/head_armor");
    public static final Tag<Item> ENCHANTABLE_ARMOR = create("enchantable/armor");
    public static final Tag<Item> ENCHANTABLE_SWORD = create("enchantable/sword");
    public static final Tag<Item> ENCHANTABLE_FIRE_ASPECT = create("enchantable/fire_aspect");
    public static final Tag<Item> ENCHANTABLE_SHARP_WEAPON = create("enchantable/sharp_weapon");
    public static final Tag<Item> ENCHANTABLE_WEAPON = create("enchantable/weapon");
    public static final Tag<Item> ENCHANTABLE_MINING = create("enchantable/mining");
    public static final Tag<Item> ENCHANTABLE_MINING_LOOT = create("enchantable/mining_loot");
    public static final Tag<Item> ENCHANTABLE_FISHING = create("enchantable/fishing");
    public static final Tag<Item> ENCHANTABLE_TRIDENT = create("enchantable/trident");
    public static final Tag<Item> ENCHANTABLE_DURABILITY = create("enchantable/durability");
    public static final Tag<Item> ENCHANTABLE_BOW = create("enchantable/bow");
    public static final Tag<Item> ENCHANTABLE_EQUIPPABLE = create("enchantable/equippable");
    public static final Tag<Item> ENCHANTABLE_CROSSBOW = create("enchantable/crossbow");
    public static final Tag<Item> ENCHANTABLE_VANISHING = create("enchantable/vanishing");
    public static final Tag<Item> ENCHANTABLE_MACE = create("enchantable/mace");

    private ItemTag() {}

    private static Tag<Item> create(String name) {
        return new Tag<>(JavaRegistries.ITEM, MinecraftKey.key(name));
    }
}
