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
public final class ItemTag {
    public static final Map<Key, VanillaTag> ALL_ITEM_TAGS = new HashMap<>();

    public static final VanillaTag WOOL = register("wool");
    public static final VanillaTag PLANKS = register("planks");
    public static final VanillaTag STONE_BRICKS = register("stone_bricks");
    public static final VanillaTag WOODEN_BUTTONS = register("wooden_buttons");
    public static final VanillaTag STONE_BUTTONS = register("stone_buttons");
    public static final VanillaTag BUTTONS = register("buttons");
    public static final VanillaTag WOOL_CARPETS = register("wool_carpets");
    public static final VanillaTag WOODEN_DOORS = register("wooden_doors");
    public static final VanillaTag WOODEN_STAIRS = register("wooden_stairs");
    public static final VanillaTag WOODEN_SLABS = register("wooden_slabs");
    public static final VanillaTag WOODEN_FENCES = register("wooden_fences");
    public static final VanillaTag FENCE_GATES = register("fence_gates");
    public static final VanillaTag WOODEN_PRESSURE_PLATES = register("wooden_pressure_plates");
    public static final VanillaTag WOODEN_TRAPDOORS = register("wooden_trapdoors");
    public static final VanillaTag DOORS = register("doors");
    public static final VanillaTag SAPLINGS = register("saplings");
    public static final VanillaTag LOGS_THAT_BURN = register("logs_that_burn");
    public static final VanillaTag LOGS = register("logs");
    public static final VanillaTag DARK_OAK_LOGS = register("dark_oak_logs");
    public static final VanillaTag OAK_LOGS = register("oak_logs");
    public static final VanillaTag BIRCH_LOGS = register("birch_logs");
    public static final VanillaTag ACACIA_LOGS = register("acacia_logs");
    public static final VanillaTag CHERRY_LOGS = register("cherry_logs");
    public static final VanillaTag JUNGLE_LOGS = register("jungle_logs");
    public static final VanillaTag SPRUCE_LOGS = register("spruce_logs");
    public static final VanillaTag MANGROVE_LOGS = register("mangrove_logs");
    public static final VanillaTag CRIMSON_STEMS = register("crimson_stems");
    public static final VanillaTag WARPED_STEMS = register("warped_stems");
    public static final VanillaTag BAMBOO_BLOCKS = register("bamboo_blocks");
    public static final VanillaTag WART_BLOCKS = register("wart_blocks");
    public static final VanillaTag BANNERS = register("banners");
    public static final VanillaTag SAND = register("sand");
    public static final VanillaTag SMELTS_TO_GLASS = register("smelts_to_glass");
    public static final VanillaTag STAIRS = register("stairs");
    public static final VanillaTag SLABS = register("slabs");
    public static final VanillaTag WALLS = register("walls");
    public static final VanillaTag ANVIL = register("anvil");
    public static final VanillaTag RAILS = register("rails");
    public static final VanillaTag LEAVES = register("leaves");
    public static final VanillaTag TRAPDOORS = register("trapdoors");
    public static final VanillaTag SMALL_FLOWERS = register("small_flowers");
    public static final VanillaTag BEDS = register("beds");
    public static final VanillaTag FENCES = register("fences");
    public static final VanillaTag TALL_FLOWERS = register("tall_flowers");
    public static final VanillaTag FLOWERS = register("flowers");
    public static final VanillaTag PIGLIN_REPELLENTS = register("piglin_repellents");
    public static final VanillaTag PIGLIN_LOVED = register("piglin_loved");
    public static final VanillaTag IGNORED_BY_PIGLIN_BABIES = register("ignored_by_piglin_babies");
    public static final VanillaTag MEAT = register("meat");
    public static final VanillaTag SNIFFER_FOOD = register("sniffer_food");
    public static final VanillaTag PIGLIN_FOOD = register("piglin_food");
    public static final VanillaTag FOX_FOOD = register("fox_food");
    public static final VanillaTag COW_FOOD = register("cow_food");
    public static final VanillaTag GOAT_FOOD = register("goat_food");
    public static final VanillaTag SHEEP_FOOD = register("sheep_food");
    public static final VanillaTag WOLF_FOOD = register("wolf_food");
    public static final VanillaTag CAT_FOOD = register("cat_food");
    public static final VanillaTag HORSE_FOOD = register("horse_food");
    public static final VanillaTag HORSE_TEMPT_ITEMS = register("horse_tempt_items");
    public static final VanillaTag CAMEL_FOOD = register("camel_food");
    public static final VanillaTag ARMADILLO_FOOD = register("armadillo_food");
    public static final VanillaTag BEE_FOOD = register("bee_food");
    public static final VanillaTag CHICKEN_FOOD = register("chicken_food");
    public static final VanillaTag FROG_FOOD = register("frog_food");
    public static final VanillaTag HOGLIN_FOOD = register("hoglin_food");
    public static final VanillaTag LLAMA_FOOD = register("llama_food");
    public static final VanillaTag LLAMA_TEMPT_ITEMS = register("llama_tempt_items");
    public static final VanillaTag OCELOT_FOOD = register("ocelot_food");
    public static final VanillaTag PANDA_FOOD = register("panda_food");
    public static final VanillaTag PIG_FOOD = register("pig_food");
    public static final VanillaTag RABBIT_FOOD = register("rabbit_food");
    public static final VanillaTag STRIDER_FOOD = register("strider_food");
    public static final VanillaTag STRIDER_TEMPT_ITEMS = register("strider_tempt_items");
    public static final VanillaTag TURTLE_FOOD = register("turtle_food");
    public static final VanillaTag PARROT_FOOD = register("parrot_food");
    public static final VanillaTag PARROT_POISONOUS_FOOD = register("parrot_poisonous_food");
    public static final VanillaTag AXOLOTL_FOOD = register("axolotl_food");
    public static final VanillaTag GOLD_ORES = register("gold_ores");
    public static final VanillaTag IRON_ORES = register("iron_ores");
    public static final VanillaTag DIAMOND_ORES = register("diamond_ores");
    public static final VanillaTag REDSTONE_ORES = register("redstone_ores");
    public static final VanillaTag LAPIS_ORES = register("lapis_ores");
    public static final VanillaTag COAL_ORES = register("coal_ores");
    public static final VanillaTag EMERALD_ORES = register("emerald_ores");
    public static final VanillaTag COPPER_ORES = register("copper_ores");
    public static final VanillaTag NON_FLAMMABLE_WOOD = register("non_flammable_wood");
    public static final VanillaTag SOUL_FIRE_BASE_BLOCKS = register("soul_fire_base_blocks");
    public static final VanillaTag CANDLES = register("candles");
    public static final VanillaTag DIRT = register("dirt");
    public static final VanillaTag TERRACOTTA = register("terracotta");
    public static final VanillaTag COMPLETES_FIND_TREE_TUTORIAL = register("completes_find_tree_tutorial");
    public static final VanillaTag BOATS = register("boats");
    public static final VanillaTag CHEST_BOATS = register("chest_boats");
    public static final VanillaTag FISHES = register("fishes");
    public static final VanillaTag SIGNS = register("signs");
    public static final VanillaTag CREEPER_DROP_MUSIC_DISCS = register("creeper_drop_music_discs");
    public static final VanillaTag COALS = register("coals");
    public static final VanillaTag ARROWS = register("arrows");
    public static final VanillaTag LECTERN_BOOKS = register("lectern_books");
    public static final VanillaTag BOOKSHELF_BOOKS = register("bookshelf_books");
    public static final VanillaTag BEACON_PAYMENT_ITEMS = register("beacon_payment_items");
    public static final VanillaTag STONE_TOOL_MATERIALS = register("stone_tool_materials");
    public static final VanillaTag STONE_CRAFTING_MATERIALS = register("stone_crafting_materials");
    public static final VanillaTag FREEZE_IMMUNE_WEARABLES = register("freeze_immune_wearables");
    public static final VanillaTag DAMPENS_VIBRATIONS = register("dampens_vibrations");
    public static final VanillaTag CLUSTER_MAX_HARVESTABLES = register("cluster_max_harvestables");
    public static final VanillaTag COMPASSES = register("compasses");
    public static final VanillaTag HANGING_SIGNS = register("hanging_signs");
    public static final VanillaTag CREEPER_IGNITERS = register("creeper_igniters");
    public static final VanillaTag NOTEBLOCK_TOP_INSTRUMENTS = register("noteblock_top_instruments");
    public static final VanillaTag FOOT_ARMOR = register("foot_armor");
    public static final VanillaTag LEG_ARMOR = register("leg_armor");
    public static final VanillaTag CHEST_ARMOR = register("chest_armor");
    public static final VanillaTag HEAD_ARMOR = register("head_armor");
    public static final VanillaTag SKULLS = register("skulls");
    public static final VanillaTag TRIMMABLE_ARMOR = register("trimmable_armor");
    public static final VanillaTag TRIM_MATERIALS = register("trim_materials");
    public static final VanillaTag TRIM_TEMPLATES = register("trim_templates");
    public static final VanillaTag DECORATED_POT_SHERDS = register("decorated_pot_sherds");
    public static final VanillaTag DECORATED_POT_INGREDIENTS = register("decorated_pot_ingredients");
    public static final VanillaTag SWORDS = register("swords");
    public static final VanillaTag AXES = register("axes");
    public static final VanillaTag HOES = register("hoes");
    public static final VanillaTag PICKAXES = register("pickaxes");
    public static final VanillaTag SHOVELS = register("shovels");
    public static final VanillaTag BREAKS_DECORATED_POTS = register("breaks_decorated_pots");
    public static final VanillaTag VILLAGER_PLANTABLE_SEEDS = register("villager_plantable_seeds");
    public static final VanillaTag DYEABLE = register("dyeable");
    public static final VanillaTag ENCHANTABLE_FOOT_ARMOR = register("enchantable/foot_armor");
    public static final VanillaTag ENCHANTABLE_LEG_ARMOR = register("enchantable/leg_armor");
    public static final VanillaTag ENCHANTABLE_CHEST_ARMOR = register("enchantable/chest_armor");
    public static final VanillaTag ENCHANTABLE_HEAD_ARMOR = register("enchantable/head_armor");
    public static final VanillaTag ENCHANTABLE_ARMOR = register("enchantable/armor");
    public static final VanillaTag ENCHANTABLE_SWORD = register("enchantable/sword");
    public static final VanillaTag ENCHANTABLE_FIRE_ASPECT = register("enchantable/fire_aspect");
    public static final VanillaTag ENCHANTABLE_SHARP_WEAPON = register("enchantable/sharp_weapon");
    public static final VanillaTag ENCHANTABLE_WEAPON = register("enchantable/weapon");
    public static final VanillaTag ENCHANTABLE_MINING = register("enchantable/mining");
    public static final VanillaTag ENCHANTABLE_MINING_LOOT = register("enchantable/mining_loot");
    public static final VanillaTag ENCHANTABLE_FISHING = register("enchantable/fishing");
    public static final VanillaTag ENCHANTABLE_TRIDENT = register("enchantable/trident");
    public static final VanillaTag ENCHANTABLE_DURABILITY = register("enchantable/durability");
    public static final VanillaTag ENCHANTABLE_BOW = register("enchantable/bow");
    public static final VanillaTag ENCHANTABLE_EQUIPPABLE = register("enchantable/equippable");
    public static final VanillaTag ENCHANTABLE_CROSSBOW = register("enchantable/crossbow");
    public static final VanillaTag ENCHANTABLE_VANISHING = register("enchantable/vanishing");
    public static final VanillaTag ENCHANTABLE_MACE = register("enchantable/mace");

    private ItemTag() {}

    private static VanillaTag register(String name) {
        Key identifier = MinecraftKey.key(name);
        int geyserId = ALL_ITEM_TAGS.size();
        VanillaTag tag = new VanillaTag(MinecraftKey.key("item"), identifier, geyserId);
        ALL_ITEM_TAGS.put(MinecraftKey.key(name), tag);
        return tag;
    }
}
