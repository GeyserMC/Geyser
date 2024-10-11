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
    public static final Tag<Item> WOOL = register("wool");
    public static final Tag<Item> PLANKS = register("planks");
    public static final Tag<Item> STONE_BRICKS = register("stone_bricks");
    public static final Tag<Item> WOODEN_BUTTONS = register("wooden_buttons");
    public static final Tag<Item> STONE_BUTTONS = register("stone_buttons");
    public static final Tag<Item> BUTTONS = register("buttons");
    public static final Tag<Item> WOOL_CARPETS = register("wool_carpets");
    public static final Tag<Item> WOODEN_DOORS = register("wooden_doors");
    public static final Tag<Item> WOODEN_STAIRS = register("wooden_stairs");
    public static final Tag<Item> WOODEN_SLABS = register("wooden_slabs");
    public static final Tag<Item> WOODEN_FENCES = register("wooden_fences");
    public static final Tag<Item> FENCE_GATES = register("fence_gates");
    public static final Tag<Item> WOODEN_PRESSURE_PLATES = register("wooden_pressure_plates");
    public static final Tag<Item> WOODEN_TRAPDOORS = register("wooden_trapdoors");
    public static final Tag<Item> DOORS = register("doors");
    public static final Tag<Item> SAPLINGS = register("saplings");
    public static final Tag<Item> LOGS_THAT_BURN = register("logs_that_burn");
    public static final Tag<Item> LOGS = register("logs");
    public static final Tag<Item> DARK_OAK_LOGS = register("dark_oak_logs");
    public static final Tag<Item> OAK_LOGS = register("oak_logs");
    public static final Tag<Item> BIRCH_LOGS = register("birch_logs");
    public static final Tag<Item> ACACIA_LOGS = register("acacia_logs");
    public static final Tag<Item> CHERRY_LOGS = register("cherry_logs");
    public static final Tag<Item> JUNGLE_LOGS = register("jungle_logs");
    public static final Tag<Item> SPRUCE_LOGS = register("spruce_logs");
    public static final Tag<Item> MANGROVE_LOGS = register("mangrove_logs");
    public static final Tag<Item> CRIMSON_STEMS = register("crimson_stems");
    public static final Tag<Item> WARPED_STEMS = register("warped_stems");
    public static final Tag<Item> BAMBOO_BLOCKS = register("bamboo_blocks");
    public static final Tag<Item> WART_BLOCKS = register("wart_blocks");
    public static final Tag<Item> BANNERS = register("banners");
    public static final Tag<Item> SAND = register("sand");
    public static final Tag<Item> SMELTS_TO_GLASS = register("smelts_to_glass");
    public static final Tag<Item> STAIRS = register("stairs");
    public static final Tag<Item> SLABS = register("slabs");
    public static final Tag<Item> WALLS = register("walls");
    public static final Tag<Item> ANVIL = register("anvil");
    public static final Tag<Item> RAILS = register("rails");
    public static final Tag<Item> LEAVES = register("leaves");
    public static final Tag<Item> TRAPDOORS = register("trapdoors");
    public static final Tag<Item> SMALL_FLOWERS = register("small_flowers");
    public static final Tag<Item> BEDS = register("beds");
    public static final Tag<Item> FENCES = register("fences");
    public static final Tag<Item> TALL_FLOWERS = register("tall_flowers");
    public static final Tag<Item> FLOWERS = register("flowers");
    public static final Tag<Item> PIGLIN_REPELLENTS = register("piglin_repellents");
    public static final Tag<Item> PIGLIN_LOVED = register("piglin_loved");
    public static final Tag<Item> IGNORED_BY_PIGLIN_BABIES = register("ignored_by_piglin_babies");
    public static final Tag<Item> MEAT = register("meat");
    public static final Tag<Item> SNIFFER_FOOD = register("sniffer_food");
    public static final Tag<Item> PIGLIN_FOOD = register("piglin_food");
    public static final Tag<Item> FOX_FOOD = register("fox_food");
    public static final Tag<Item> COW_FOOD = register("cow_food");
    public static final Tag<Item> GOAT_FOOD = register("goat_food");
    public static final Tag<Item> SHEEP_FOOD = register("sheep_food");
    public static final Tag<Item> WOLF_FOOD = register("wolf_food");
    public static final Tag<Item> CAT_FOOD = register("cat_food");
    public static final Tag<Item> HORSE_FOOD = register("horse_food");
    public static final Tag<Item> HORSE_TEMPT_ITEMS = register("horse_tempt_items");
    public static final Tag<Item> CAMEL_FOOD = register("camel_food");
    public static final Tag<Item> ARMADILLO_FOOD = register("armadillo_food");
    public static final Tag<Item> BEE_FOOD = register("bee_food");
    public static final Tag<Item> CHICKEN_FOOD = register("chicken_food");
    public static final Tag<Item> FROG_FOOD = register("frog_food");
    public static final Tag<Item> HOGLIN_FOOD = register("hoglin_food");
    public static final Tag<Item> LLAMA_FOOD = register("llama_food");
    public static final Tag<Item> LLAMA_TEMPT_ITEMS = register("llama_tempt_items");
    public static final Tag<Item> OCELOT_FOOD = register("ocelot_food");
    public static final Tag<Item> PANDA_FOOD = register("panda_food");
    public static final Tag<Item> PIG_FOOD = register("pig_food");
    public static final Tag<Item> RABBIT_FOOD = register("rabbit_food");
    public static final Tag<Item> STRIDER_FOOD = register("strider_food");
    public static final Tag<Item> STRIDER_TEMPT_ITEMS = register("strider_tempt_items");
    public static final Tag<Item> TURTLE_FOOD = register("turtle_food");
    public static final Tag<Item> PARROT_FOOD = register("parrot_food");
    public static final Tag<Item> PARROT_POISONOUS_FOOD = register("parrot_poisonous_food");
    public static final Tag<Item> AXOLOTL_FOOD = register("axolotl_food");
    public static final Tag<Item> GOLD_ORES = register("gold_ores");
    public static final Tag<Item> IRON_ORES = register("iron_ores");
    public static final Tag<Item> DIAMOND_ORES = register("diamond_ores");
    public static final Tag<Item> REDSTONE_ORES = register("redstone_ores");
    public static final Tag<Item> LAPIS_ORES = register("lapis_ores");
    public static final Tag<Item> COAL_ORES = register("coal_ores");
    public static final Tag<Item> EMERALD_ORES = register("emerald_ores");
    public static final Tag<Item> COPPER_ORES = register("copper_ores");
    public static final Tag<Item> NON_FLAMMABLE_WOOD = register("non_flammable_wood");
    public static final Tag<Item> SOUL_FIRE_BASE_BLOCKS = register("soul_fire_base_blocks");
    public static final Tag<Item> CANDLES = register("candles");
    public static final Tag<Item> DIRT = register("dirt");
    public static final Tag<Item> TERRACOTTA = register("terracotta");
    public static final Tag<Item> COMPLETES_FIND_TREE_TUTORIAL = register("completes_find_tree_tutorial");
    public static final Tag<Item> BOATS = register("boats");
    public static final Tag<Item> CHEST_BOATS = register("chest_boats");
    public static final Tag<Item> FISHES = register("fishes");
    public static final Tag<Item> SIGNS = register("signs");
    public static final Tag<Item> CREEPER_DROP_MUSIC_DISCS = register("creeper_drop_music_discs");
    public static final Tag<Item> COALS = register("coals");
    public static final Tag<Item> ARROWS = register("arrows");
    public static final Tag<Item> LECTERN_BOOKS = register("lectern_books");
    public static final Tag<Item> BOOKSHELF_BOOKS = register("bookshelf_books");
    public static final Tag<Item> BEACON_PAYMENT_ITEMS = register("beacon_payment_items");
    public static final Tag<Item> STONE_TOOL_MATERIALS = register("stone_tool_materials");
    public static final Tag<Item> STONE_CRAFTING_MATERIALS = register("stone_crafting_materials");
    public static final Tag<Item> FREEZE_IMMUNE_WEARABLES = register("freeze_immune_wearables");
    public static final Tag<Item> DAMPENS_VIBRATIONS = register("dampens_vibrations");
    public static final Tag<Item> CLUSTER_MAX_HARVESTABLES = register("cluster_max_harvestables");
    public static final Tag<Item> COMPASSES = register("compasses");
    public static final Tag<Item> HANGING_SIGNS = register("hanging_signs");
    public static final Tag<Item> CREEPER_IGNITERS = register("creeper_igniters");
    public static final Tag<Item> NOTEBLOCK_TOP_INSTRUMENTS = register("noteblock_top_instruments");
    public static final Tag<Item> FOOT_ARMOR = register("foot_armor");
    public static final Tag<Item> LEG_ARMOR = register("leg_armor");
    public static final Tag<Item> CHEST_ARMOR = register("chest_armor");
    public static final Tag<Item> HEAD_ARMOR = register("head_armor");
    public static final Tag<Item> SKULLS = register("skulls");
    public static final Tag<Item> TRIMMABLE_ARMOR = register("trimmable_armor");
    public static final Tag<Item> TRIM_MATERIALS = register("trim_materials");
    public static final Tag<Item> TRIM_TEMPLATES = register("trim_templates");
    public static final Tag<Item> DECORATED_POT_SHERDS = register("decorated_pot_sherds");
    public static final Tag<Item> DECORATED_POT_INGREDIENTS = register("decorated_pot_ingredients");
    public static final Tag<Item> SWORDS = register("swords");
    public static final Tag<Item> AXES = register("axes");
    public static final Tag<Item> HOES = register("hoes");
    public static final Tag<Item> PICKAXES = register("pickaxes");
    public static final Tag<Item> SHOVELS = register("shovels");
    public static final Tag<Item> BREAKS_DECORATED_POTS = register("breaks_decorated_pots");
    public static final Tag<Item> VILLAGER_PLANTABLE_SEEDS = register("villager_plantable_seeds");
    public static final Tag<Item> DYEABLE = register("dyeable");
    public static final Tag<Item> ENCHANTABLE_FOOT_ARMOR = register("enchantable/foot_armor");
    public static final Tag<Item> ENCHANTABLE_LEG_ARMOR = register("enchantable/leg_armor");
    public static final Tag<Item> ENCHANTABLE_CHEST_ARMOR = register("enchantable/chest_armor");
    public static final Tag<Item> ENCHANTABLE_HEAD_ARMOR = register("enchantable/head_armor");
    public static final Tag<Item> ENCHANTABLE_ARMOR = register("enchantable/armor");
    public static final Tag<Item> ENCHANTABLE_SWORD = register("enchantable/sword");
    public static final Tag<Item> ENCHANTABLE_FIRE_ASPECT = register("enchantable/fire_aspect");
    public static final Tag<Item> ENCHANTABLE_SHARP_WEAPON = register("enchantable/sharp_weapon");
    public static final Tag<Item> ENCHANTABLE_WEAPON = register("enchantable/weapon");
    public static final Tag<Item> ENCHANTABLE_MINING = register("enchantable/mining");
    public static final Tag<Item> ENCHANTABLE_MINING_LOOT = register("enchantable/mining_loot");
    public static final Tag<Item> ENCHANTABLE_FISHING = register("enchantable/fishing");
    public static final Tag<Item> ENCHANTABLE_TRIDENT = register("enchantable/trident");
    public static final Tag<Item> ENCHANTABLE_DURABILITY = register("enchantable/durability");
    public static final Tag<Item> ENCHANTABLE_BOW = register("enchantable/bow");
    public static final Tag<Item> ENCHANTABLE_EQUIPPABLE = register("enchantable/equippable");
    public static final Tag<Item> ENCHANTABLE_CROSSBOW = register("enchantable/crossbow");
    public static final Tag<Item> ENCHANTABLE_VANISHING = register("enchantable/vanishing");
    public static final Tag<Item> ENCHANTABLE_MACE = register("enchantable/mace");

    private ItemTag() {}

    private static Tag<Item> register(String name) {
        return JavaRegistries.ITEM.registerVanillaTag(MinecraftKey.key(name));
    }

    public static void init() {
        // no-op
    }
}
