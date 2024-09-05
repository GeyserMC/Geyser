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

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.geyser.util.Ordered;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class ItemTag implements Ordered {
    public static final Map<Key, ItemTag> ALL_ITEM_TAGS = new HashMap<>();

    public static final ItemTag WOOL = new ItemTag("wool");
    public static final ItemTag PLANKS = new ItemTag("planks");
    public static final ItemTag STONE_BRICKS = new ItemTag("stone_bricks");
    public static final ItemTag WOODEN_BUTTONS = new ItemTag("wooden_buttons");
    public static final ItemTag STONE_BUTTONS = new ItemTag("stone_buttons");
    public static final ItemTag BUTTONS = new ItemTag("buttons");
    public static final ItemTag WOOL_CARPETS = new ItemTag("wool_carpets");
    public static final ItemTag WOODEN_DOORS = new ItemTag("wooden_doors");
    public static final ItemTag WOODEN_STAIRS = new ItemTag("wooden_stairs");
    public static final ItemTag WOODEN_SLABS = new ItemTag("wooden_slabs");
    public static final ItemTag WOODEN_FENCES = new ItemTag("wooden_fences");
    public static final ItemTag FENCE_GATES = new ItemTag("fence_gates");
    public static final ItemTag WOODEN_PRESSURE_PLATES = new ItemTag("wooden_pressure_plates");
    public static final ItemTag WOODEN_TRAPDOORS = new ItemTag("wooden_trapdoors");
    public static final ItemTag DOORS = new ItemTag("doors");
    public static final ItemTag SAPLINGS = new ItemTag("saplings");
    public static final ItemTag LOGS_THAT_BURN = new ItemTag("logs_that_burn");
    public static final ItemTag LOGS = new ItemTag("logs");
    public static final ItemTag DARK_OAK_LOGS = new ItemTag("dark_oak_logs");
    public static final ItemTag OAK_LOGS = new ItemTag("oak_logs");
    public static final ItemTag BIRCH_LOGS = new ItemTag("birch_logs");
    public static final ItemTag ACACIA_LOGS = new ItemTag("acacia_logs");
    public static final ItemTag CHERRY_LOGS = new ItemTag("cherry_logs");
    public static final ItemTag JUNGLE_LOGS = new ItemTag("jungle_logs");
    public static final ItemTag SPRUCE_LOGS = new ItemTag("spruce_logs");
    public static final ItemTag MANGROVE_LOGS = new ItemTag("mangrove_logs");
    public static final ItemTag CRIMSON_STEMS = new ItemTag("crimson_stems");
    public static final ItemTag WARPED_STEMS = new ItemTag("warped_stems");
    public static final ItemTag BAMBOO_BLOCKS = new ItemTag("bamboo_blocks");
    public static final ItemTag WART_BLOCKS = new ItemTag("wart_blocks");
    public static final ItemTag BANNERS = new ItemTag("banners");
    public static final ItemTag SAND = new ItemTag("sand");
    public static final ItemTag SMELTS_TO_GLASS = new ItemTag("smelts_to_glass");
    public static final ItemTag STAIRS = new ItemTag("stairs");
    public static final ItemTag SLABS = new ItemTag("slabs");
    public static final ItemTag WALLS = new ItemTag("walls");
    public static final ItemTag ANVIL = new ItemTag("anvil");
    public static final ItemTag RAILS = new ItemTag("rails");
    public static final ItemTag LEAVES = new ItemTag("leaves");
    public static final ItemTag TRAPDOORS = new ItemTag("trapdoors");
    public static final ItemTag SMALL_FLOWERS = new ItemTag("small_flowers");
    public static final ItemTag BEDS = new ItemTag("beds");
    public static final ItemTag FENCES = new ItemTag("fences");
    public static final ItemTag TALL_FLOWERS = new ItemTag("tall_flowers");
    public static final ItemTag FLOWERS = new ItemTag("flowers");
    public static final ItemTag PIGLIN_REPELLENTS = new ItemTag("piglin_repellents");
    public static final ItemTag PIGLIN_LOVED = new ItemTag("piglin_loved");
    public static final ItemTag IGNORED_BY_PIGLIN_BABIES = new ItemTag("ignored_by_piglin_babies");
    public static final ItemTag MEAT = new ItemTag("meat");
    public static final ItemTag SNIFFER_FOOD = new ItemTag("sniffer_food");
    public static final ItemTag PIGLIN_FOOD = new ItemTag("piglin_food");
    public static final ItemTag FOX_FOOD = new ItemTag("fox_food");
    public static final ItemTag COW_FOOD = new ItemTag("cow_food");
    public static final ItemTag GOAT_FOOD = new ItemTag("goat_food");
    public static final ItemTag SHEEP_FOOD = new ItemTag("sheep_food");
    public static final ItemTag WOLF_FOOD = new ItemTag("wolf_food");
    public static final ItemTag CAT_FOOD = new ItemTag("cat_food");
    public static final ItemTag HORSE_FOOD = new ItemTag("horse_food");
    public static final ItemTag HORSE_TEMPT_ITEMS = new ItemTag("horse_tempt_items");
    public static final ItemTag CAMEL_FOOD = new ItemTag("camel_food");
    public static final ItemTag ARMADILLO_FOOD = new ItemTag("armadillo_food");
    public static final ItemTag BEE_FOOD = new ItemTag("bee_food");
    public static final ItemTag CHICKEN_FOOD = new ItemTag("chicken_food");
    public static final ItemTag FROG_FOOD = new ItemTag("frog_food");
    public static final ItemTag HOGLIN_FOOD = new ItemTag("hoglin_food");
    public static final ItemTag LLAMA_FOOD = new ItemTag("llama_food");
    public static final ItemTag LLAMA_TEMPT_ITEMS = new ItemTag("llama_tempt_items");
    public static final ItemTag OCELOT_FOOD = new ItemTag("ocelot_food");
    public static final ItemTag PANDA_FOOD = new ItemTag("panda_food");
    public static final ItemTag PIG_FOOD = new ItemTag("pig_food");
    public static final ItemTag RABBIT_FOOD = new ItemTag("rabbit_food");
    public static final ItemTag STRIDER_FOOD = new ItemTag("strider_food");
    public static final ItemTag STRIDER_TEMPT_ITEMS = new ItemTag("strider_tempt_items");
    public static final ItemTag TURTLE_FOOD = new ItemTag("turtle_food");
    public static final ItemTag PARROT_FOOD = new ItemTag("parrot_food");
    public static final ItemTag PARROT_POISONOUS_FOOD = new ItemTag("parrot_poisonous_food");
    public static final ItemTag AXOLOTL_FOOD = new ItemTag("axolotl_food");
    public static final ItemTag GOLD_ORES = new ItemTag("gold_ores");
    public static final ItemTag IRON_ORES = new ItemTag("iron_ores");
    public static final ItemTag DIAMOND_ORES = new ItemTag("diamond_ores");
    public static final ItemTag REDSTONE_ORES = new ItemTag("redstone_ores");
    public static final ItemTag LAPIS_ORES = new ItemTag("lapis_ores");
    public static final ItemTag COAL_ORES = new ItemTag("coal_ores");
    public static final ItemTag EMERALD_ORES = new ItemTag("emerald_ores");
    public static final ItemTag COPPER_ORES = new ItemTag("copper_ores");
    public static final ItemTag NON_FLAMMABLE_WOOD = new ItemTag("non_flammable_wood");
    public static final ItemTag SOUL_FIRE_BASE_BLOCKS = new ItemTag("soul_fire_base_blocks");
    public static final ItemTag CANDLES = new ItemTag("candles");
    public static final ItemTag DIRT = new ItemTag("dirt");
    public static final ItemTag TERRACOTTA = new ItemTag("terracotta");
    public static final ItemTag COMPLETES_FIND_TREE_TUTORIAL = new ItemTag("completes_find_tree_tutorial");
    public static final ItemTag BOATS = new ItemTag("boats");
    public static final ItemTag CHEST_BOATS = new ItemTag("chest_boats");
    public static final ItemTag FISHES = new ItemTag("fishes");
    public static final ItemTag SIGNS = new ItemTag("signs");
    public static final ItemTag CREEPER_DROP_MUSIC_DISCS = new ItemTag("creeper_drop_music_discs");
    public static final ItemTag COALS = new ItemTag("coals");
    public static final ItemTag ARROWS = new ItemTag("arrows");
    public static final ItemTag LECTERN_BOOKS = new ItemTag("lectern_books");
    public static final ItemTag BOOKSHELF_BOOKS = new ItemTag("bookshelf_books");
    public static final ItemTag BEACON_PAYMENT_ITEMS = new ItemTag("beacon_payment_items");
    public static final ItemTag STONE_TOOL_MATERIALS = new ItemTag("stone_tool_materials");
    public static final ItemTag STONE_CRAFTING_MATERIALS = new ItemTag("stone_crafting_materials");
    public static final ItemTag FREEZE_IMMUNE_WEARABLES = new ItemTag("freeze_immune_wearables");
    public static final ItemTag DAMPENS_VIBRATIONS = new ItemTag("dampens_vibrations");
    public static final ItemTag CLUSTER_MAX_HARVESTABLES = new ItemTag("cluster_max_harvestables");
    public static final ItemTag COMPASSES = new ItemTag("compasses");
    public static final ItemTag HANGING_SIGNS = new ItemTag("hanging_signs");
    public static final ItemTag CREEPER_IGNITERS = new ItemTag("creeper_igniters");
    public static final ItemTag NOTEBLOCK_TOP_INSTRUMENTS = new ItemTag("noteblock_top_instruments");
    public static final ItemTag FOOT_ARMOR = new ItemTag("foot_armor");
    public static final ItemTag LEG_ARMOR = new ItemTag("leg_armor");
    public static final ItemTag CHEST_ARMOR = new ItemTag("chest_armor");
    public static final ItemTag HEAD_ARMOR = new ItemTag("head_armor");
    public static final ItemTag SKULLS = new ItemTag("skulls");
    public static final ItemTag TRIMMABLE_ARMOR = new ItemTag("trimmable_armor");
    public static final ItemTag TRIM_MATERIALS = new ItemTag("trim_materials");
    public static final ItemTag TRIM_TEMPLATES = new ItemTag("trim_templates");
    public static final ItemTag DECORATED_POT_SHERDS = new ItemTag("decorated_pot_sherds");
    public static final ItemTag DECORATED_POT_INGREDIENTS = new ItemTag("decorated_pot_ingredients");
    public static final ItemTag SWORDS = new ItemTag("swords");
    public static final ItemTag AXES = new ItemTag("axes");
    public static final ItemTag HOES = new ItemTag("hoes");
    public static final ItemTag PICKAXES = new ItemTag("pickaxes");
    public static final ItemTag SHOVELS = new ItemTag("shovels");
    public static final ItemTag BREAKS_DECORATED_POTS = new ItemTag("breaks_decorated_pots");
    public static final ItemTag VILLAGER_PLANTABLE_SEEDS = new ItemTag("villager_plantable_seeds");
    public static final ItemTag DYEABLE = new ItemTag("dyeable");
    public static final ItemTag ENCHANTABLE_FOOT_ARMOR = new ItemTag("enchantable/foot_armor");
    public static final ItemTag ENCHANTABLE_LEG_ARMOR = new ItemTag("enchantable/leg_armor");
    public static final ItemTag ENCHANTABLE_CHEST_ARMOR = new ItemTag("enchantable/chest_armor");
    public static final ItemTag ENCHANTABLE_HEAD_ARMOR = new ItemTag("enchantable/head_armor");
    public static final ItemTag ENCHANTABLE_ARMOR = new ItemTag("enchantable/armor");
    public static final ItemTag ENCHANTABLE_SWORD = new ItemTag("enchantable/sword");
    public static final ItemTag ENCHANTABLE_FIRE_ASPECT = new ItemTag("enchantable/fire_aspect");
    public static final ItemTag ENCHANTABLE_SHARP_WEAPON = new ItemTag("enchantable/sharp_weapon");
    public static final ItemTag ENCHANTABLE_WEAPON = new ItemTag("enchantable/weapon");
    public static final ItemTag ENCHANTABLE_MINING = new ItemTag("enchantable/mining");
    public static final ItemTag ENCHANTABLE_MINING_LOOT = new ItemTag("enchantable/mining_loot");
    public static final ItemTag ENCHANTABLE_FISHING = new ItemTag("enchantable/fishing");
    public static final ItemTag ENCHANTABLE_TRIDENT = new ItemTag("enchantable/trident");
    public static final ItemTag ENCHANTABLE_DURABILITY = new ItemTag("enchantable/durability");
    public static final ItemTag ENCHANTABLE_BOW = new ItemTag("enchantable/bow");
    public static final ItemTag ENCHANTABLE_EQUIPPABLE = new ItemTag("enchantable/equippable");
    public static final ItemTag ENCHANTABLE_CROSSBOW = new ItemTag("enchantable/crossbow");
    public static final ItemTag ENCHANTABLE_VANISHING = new ItemTag("enchantable/vanishing");
    public static final ItemTag ENCHANTABLE_MACE = new ItemTag("enchantable/mace");

    private final int id;
    
    private ItemTag(String identifier) {
        this.id = ALL_ITEM_TAGS.size();
        register(identifier, this);
    }

    @Override
    public int ordinal() {
        return id;
    }

    private static void register(String name, ItemTag tag) {
        ALL_ITEM_TAGS.put(MinecraftKey.key(name), tag);
    }
}
