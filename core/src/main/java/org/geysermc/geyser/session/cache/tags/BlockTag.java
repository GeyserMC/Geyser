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

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
// TODO rename to vanillablocktags
public final class BlockTag {
    public static final Map<Key, VanillaTag> ALL_BLOCK_TAGS = new HashMap<>();

    public static final VanillaTag WOOL = register("wool");
    public static final VanillaTag PLANKS = register("planks");
    public static final VanillaTag STONE_BRICKS = register("stone_bricks");
    public static final VanillaTag WOODEN_BUTTONS = register("wooden_buttons");
    public static final VanillaTag STONE_BUTTONS = register("stone_buttons");
    public static final VanillaTag BUTTONS = register("buttons");
    public static final VanillaTag WOOL_CARPETS = register("wool_carpets");
    public static final VanillaTag WOODEN_DOORS = register("wooden_doors");
    public static final VanillaTag MOB_INTERACTABLE_DOORS = register("mob_interactable_doors");
    public static final VanillaTag WOODEN_STAIRS = register("wooden_stairs");
    public static final VanillaTag WOODEN_SLABS = register("wooden_slabs");
    public static final VanillaTag WOODEN_FENCES = register("wooden_fences");
    public static final VanillaTag PRESSURE_PLATES = register("pressure_plates");
    public static final VanillaTag WOODEN_PRESSURE_PLATES = register("wooden_pressure_plates");
    public static final VanillaTag STONE_PRESSURE_PLATES = register("stone_pressure_plates");
    public static final VanillaTag WOODEN_TRAPDOORS = register("wooden_trapdoors");
    public static final VanillaTag DOORS = register("doors");
    public static final VanillaTag SAPLINGS = register("saplings");
    public static final VanillaTag LOGS_THAT_BURN = register("logs_that_burn");
    public static final VanillaTag OVERWORLD_NATURAL_LOGS = register("overworld_natural_logs");
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
    public static final VanillaTag GOLD_ORES = register("gold_ores");
    public static final VanillaTag IRON_ORES = register("iron_ores");
    public static final VanillaTag DIAMOND_ORES = register("diamond_ores");
    public static final VanillaTag REDSTONE_ORES = register("redstone_ores");
    public static final VanillaTag LAPIS_ORES = register("lapis_ores");
    public static final VanillaTag COAL_ORES = register("coal_ores");
    public static final VanillaTag EMERALD_ORES = register("emerald_ores");
    public static final VanillaTag COPPER_ORES = register("copper_ores");
    public static final VanillaTag CANDLES = register("candles");
    public static final VanillaTag DIRT = register("dirt");
    public static final VanillaTag TERRACOTTA = register("terracotta");
    public static final VanillaTag BADLANDS_TERRACOTTA = register("badlands_terracotta");
    public static final VanillaTag CONCRETE_POWDER = register("concrete_powder");
    public static final VanillaTag COMPLETES_FIND_TREE_TUTORIAL = register("completes_find_tree_tutorial");
    public static final VanillaTag FLOWER_POTS = register("flower_pots");
    public static final VanillaTag ENDERMAN_HOLDABLE = register("enderman_holdable");
    public static final VanillaTag ICE = register("ice");
    public static final VanillaTag VALID_SPAWN = register("valid_spawn");
    public static final VanillaTag IMPERMEABLE = register("impermeable");
    public static final VanillaTag UNDERWATER_BONEMEALS = register("underwater_bonemeals");
    public static final VanillaTag CORAL_BLOCKS = register("coral_blocks");
    public static final VanillaTag WALL_CORALS = register("wall_corals");
    public static final VanillaTag CORAL_PLANTS = register("coral_plants");
    public static final VanillaTag CORALS = register("corals");
    public static final VanillaTag BAMBOO_PLANTABLE_ON = register("bamboo_plantable_on");
    public static final VanillaTag STANDING_SIGNS = register("standing_signs");
    public static final VanillaTag WALL_SIGNS = register("wall_signs");
    public static final VanillaTag SIGNS = register("signs");
    public static final VanillaTag CEILING_HANGING_SIGNS = register("ceiling_hanging_signs");
    public static final VanillaTag WALL_HANGING_SIGNS = register("wall_hanging_signs");
    public static final VanillaTag ALL_HANGING_SIGNS = register("all_hanging_signs");
    public static final VanillaTag ALL_SIGNS = register("all_signs");
    public static final VanillaTag DRAGON_IMMUNE = register("dragon_immune");
    public static final VanillaTag DRAGON_TRANSPARENT = register("dragon_transparent");
    public static final VanillaTag WITHER_IMMUNE = register("wither_immune");
    public static final VanillaTag WITHER_SUMMON_BASE_BLOCKS = register("wither_summon_base_blocks");
    public static final VanillaTag BEEHIVES = register("beehives");
    public static final VanillaTag CROPS = register("crops");
    public static final VanillaTag BEE_GROWABLES = register("bee_growables");
    public static final VanillaTag PORTALS = register("portals");
    public static final VanillaTag FIRE = register("fire");
    public static final VanillaTag NYLIUM = register("nylium");
    public static final VanillaTag BEACON_BASE_BLOCKS = register("beacon_base_blocks");
    public static final VanillaTag SOUL_SPEED_BLOCKS = register("soul_speed_blocks");
    public static final VanillaTag WALL_POST_OVERRIDE = register("wall_post_override");
    public static final VanillaTag CLIMBABLE = register("climbable");
    public static final VanillaTag FALL_DAMAGE_RESETTING = register("fall_damage_resetting");
    public static final VanillaTag SHULKER_BOXES = register("shulker_boxes");
    public static final VanillaTag HOGLIN_REPELLENTS = register("hoglin_repellents");
    public static final VanillaTag SOUL_FIRE_BASE_BLOCKS = register("soul_fire_base_blocks");
    public static final VanillaTag STRIDER_WARM_BLOCKS = register("strider_warm_blocks");
    public static final VanillaTag CAMPFIRES = register("campfires");
    public static final VanillaTag GUARDED_BY_PIGLINS = register("guarded_by_piglins");
    public static final VanillaTag PREVENT_MOB_SPAWNING_INSIDE = register("prevent_mob_spawning_inside");
    public static final VanillaTag FENCE_GATES = register("fence_gates");
    public static final VanillaTag UNSTABLE_BOTTOM_CENTER = register("unstable_bottom_center");
    public static final VanillaTag MUSHROOM_GROW_BLOCK = register("mushroom_grow_block");
    public static final VanillaTag INFINIBURN_OVERWORLD = register("infiniburn_overworld");
    public static final VanillaTag INFINIBURN_NETHER = register("infiniburn_nether");
    public static final VanillaTag INFINIBURN_END = register("infiniburn_end");
    public static final VanillaTag BASE_STONE_OVERWORLD = register("base_stone_overworld");
    public static final VanillaTag STONE_ORE_REPLACEABLES = register("stone_ore_replaceables");
    public static final VanillaTag DEEPSLATE_ORE_REPLACEABLES = register("deepslate_ore_replaceables");
    public static final VanillaTag BASE_STONE_NETHER = register("base_stone_nether");
    public static final VanillaTag OVERWORLD_CARVER_REPLACEABLES = register("overworld_carver_replaceables");
    public static final VanillaTag NETHER_CARVER_REPLACEABLES = register("nether_carver_replaceables");
    public static final VanillaTag CANDLE_CAKES = register("candle_cakes");
    public static final VanillaTag CAULDRONS = register("cauldrons");
    public static final VanillaTag CRYSTAL_SOUND_BLOCKS = register("crystal_sound_blocks");
    public static final VanillaTag INSIDE_STEP_SOUND_BLOCKS = register("inside_step_sound_blocks");
    public static final VanillaTag COMBINATION_STEP_SOUND_BLOCKS = register("combination_step_sound_blocks");
    public static final VanillaTag CAMEL_SAND_STEP_SOUND_BLOCKS = register("camel_sand_step_sound_blocks");
    public static final VanillaTag OCCLUDES_VIBRATION_SIGNALS = register("occludes_vibration_signals");
    public static final VanillaTag DAMPENS_VIBRATIONS = register("dampens_vibrations");
    public static final VanillaTag DRIPSTONE_REPLACEABLE_BLOCKS = register("dripstone_replaceable_blocks");
    public static final VanillaTag CAVE_VINES = register("cave_vines");
    public static final VanillaTag MOSS_REPLACEABLE = register("moss_replaceable");
    public static final VanillaTag LUSH_GROUND_REPLACEABLE = register("lush_ground_replaceable");
    public static final VanillaTag AZALEA_ROOT_REPLACEABLE = register("azalea_root_replaceable");
    public static final VanillaTag SMALL_DRIPLEAF_PLACEABLE = register("small_dripleaf_placeable");
    public static final VanillaTag BIG_DRIPLEAF_PLACEABLE = register("big_dripleaf_placeable");
    public static final VanillaTag SNOW = register("snow");
    public static final VanillaTag MINEABLE_AXE = register("mineable/axe");
    public static final VanillaTag MINEABLE_HOE = register("mineable/hoe");
    public static final VanillaTag MINEABLE_PICKAXE = register("mineable/pickaxe");
    public static final VanillaTag MINEABLE_SHOVEL = register("mineable/shovel");
    public static final VanillaTag SWORD_EFFICIENT = register("sword_efficient");
    public static final VanillaTag NEEDS_DIAMOND_TOOL = register("needs_diamond_tool");
    public static final VanillaTag NEEDS_IRON_TOOL = register("needs_iron_tool");
    public static final VanillaTag NEEDS_STONE_TOOL = register("needs_stone_tool");
    public static final VanillaTag INCORRECT_FOR_NETHERITE_TOOL = register("incorrect_for_netherite_tool");
    public static final VanillaTag INCORRECT_FOR_DIAMOND_TOOL = register("incorrect_for_diamond_tool");
    public static final VanillaTag INCORRECT_FOR_IRON_TOOL = register("incorrect_for_iron_tool");
    public static final VanillaTag INCORRECT_FOR_STONE_TOOL = register("incorrect_for_stone_tool");
    public static final VanillaTag INCORRECT_FOR_GOLD_TOOL = register("incorrect_for_gold_tool");
    public static final VanillaTag INCORRECT_FOR_WOODEN_TOOL = register("incorrect_for_wooden_tool");
    public static final VanillaTag FEATURES_CANNOT_REPLACE = register("features_cannot_replace");
    public static final VanillaTag LAVA_POOL_STONE_CANNOT_REPLACE = register("lava_pool_stone_cannot_replace");
    public static final VanillaTag GEODE_INVALID_BLOCKS = register("geode_invalid_blocks");
    public static final VanillaTag FROG_PREFER_JUMP_TO = register("frog_prefer_jump_to");
    public static final VanillaTag SCULK_REPLACEABLE = register("sculk_replaceable");
    public static final VanillaTag SCULK_REPLACEABLE_WORLD_GEN = register("sculk_replaceable_world_gen");
    public static final VanillaTag ANCIENT_CITY_REPLACEABLE = register("ancient_city_replaceable");
    public static final VanillaTag VIBRATION_RESONATORS = register("vibration_resonators");
    public static final VanillaTag ANIMALS_SPAWNABLE_ON = register("animals_spawnable_on");
    public static final VanillaTag ARMADILLO_SPAWNABLE_ON = register("armadillo_spawnable_on");
    public static final VanillaTag AXOLOTLS_SPAWNABLE_ON = register("axolotls_spawnable_on");
    public static final VanillaTag GOATS_SPAWNABLE_ON = register("goats_spawnable_on");
    public static final VanillaTag MOOSHROOMS_SPAWNABLE_ON = register("mooshrooms_spawnable_on");
    public static final VanillaTag PARROTS_SPAWNABLE_ON = register("parrots_spawnable_on");
    public static final VanillaTag POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = register("polar_bears_spawnable_on_alternate");
    public static final VanillaTag RABBITS_SPAWNABLE_ON = register("rabbits_spawnable_on");
    public static final VanillaTag FOXES_SPAWNABLE_ON = register("foxes_spawnable_on");
    public static final VanillaTag WOLVES_SPAWNABLE_ON = register("wolves_spawnable_on");
    public static final VanillaTag FROGS_SPAWNABLE_ON = register("frogs_spawnable_on");
    public static final VanillaTag AZALEA_GROWS_ON = register("azalea_grows_on");
    public static final VanillaTag CONVERTABLE_TO_MUD = register("convertable_to_mud");
    public static final VanillaTag MANGROVE_LOGS_CAN_GROW_THROUGH = register("mangrove_logs_can_grow_through");
    public static final VanillaTag MANGROVE_ROOTS_CAN_GROW_THROUGH = register("mangrove_roots_can_grow_through");
    public static final VanillaTag DEAD_BUSH_MAY_PLACE_ON = register("dead_bush_may_place_on");
    public static final VanillaTag SNAPS_GOAT_HORN = register("snaps_goat_horn");
    public static final VanillaTag REPLACEABLE_BY_TREES = register("replaceable_by_trees");
    public static final VanillaTag SNOW_LAYER_CANNOT_SURVIVE_ON = register("snow_layer_cannot_survive_on");
    public static final VanillaTag SNOW_LAYER_CAN_SURVIVE_ON = register("snow_layer_can_survive_on");
    public static final VanillaTag INVALID_SPAWN_INSIDE = register("invalid_spawn_inside");
    public static final VanillaTag SNIFFER_DIGGABLE_BLOCK = register("sniffer_diggable_block");
    public static final VanillaTag SNIFFER_EGG_HATCH_BOOST = register("sniffer_egg_hatch_boost");
    public static final VanillaTag TRAIL_RUINS_REPLACEABLE = register("trail_ruins_replaceable");
    public static final VanillaTag REPLACEABLE = register("replaceable");
    public static final VanillaTag ENCHANTMENT_POWER_PROVIDER = register("enchantment_power_provider");
    public static final VanillaTag ENCHANTMENT_POWER_TRANSMITTER = register("enchantment_power_transmitter");
    public static final VanillaTag MAINTAINS_FARMLAND = register("maintains_farmland");
    public static final VanillaTag BLOCKS_WIND_CHARGE_EXPLOSIONS = register("blocks_wind_charge_explosions");
    public static final VanillaTag DOES_NOT_BLOCK_HOPPERS = register("does_not_block_hoppers");
    public static final VanillaTag AIR = register("air");

    private BlockTag() {}

    private static VanillaTag register(String name) {
        Key identifier = MinecraftKey.key(name);
        int geyserId = ALL_BLOCK_TAGS.size();
        VanillaTag tag = new VanillaTag(MinecraftKey.key("block"), identifier, geyserId);
        ALL_BLOCK_TAGS.put(identifier, tag);
        return tag;
    }
}
