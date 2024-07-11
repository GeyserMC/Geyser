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

/**
 * Lists vanilla block tags.
 */
@SuppressWarnings("unused")
public final class BlockTag {
    public static final Tag WOOL = register("wool");
    public static final Tag PLANKS = register("planks");
    public static final Tag STONE_BRICKS = register("stone_bricks");
    public static final Tag WOODEN_BUTTONS = register("wooden_buttons");
    public static final Tag STONE_BUTTONS = register("stone_buttons");
    public static final Tag BUTTONS = register("buttons");
    public static final Tag WOOL_CARPETS = register("wool_carpets");
    public static final Tag WOODEN_DOORS = register("wooden_doors");
    public static final Tag MOB_INTERACTABLE_DOORS = register("mob_interactable_doors");
    public static final Tag WOODEN_STAIRS = register("wooden_stairs");
    public static final Tag WOODEN_SLABS = register("wooden_slabs");
    public static final Tag WOODEN_FENCES = register("wooden_fences");
    public static final Tag PRESSURE_PLATES = register("pressure_plates");
    public static final Tag WOODEN_PRESSURE_PLATES = register("wooden_pressure_plates");
    public static final Tag STONE_PRESSURE_PLATES = register("stone_pressure_plates");
    public static final Tag WOODEN_TRAPDOORS = register("wooden_trapdoors");
    public static final Tag DOORS = register("doors");
    public static final Tag SAPLINGS = register("saplings");
    public static final Tag LOGS_THAT_BURN = register("logs_that_burn");
    public static final Tag OVERWORLD_NATURAL_LOGS = register("overworld_natural_logs");
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
    public static final Tag GOLD_ORES = register("gold_ores");
    public static final Tag IRON_ORES = register("iron_ores");
    public static final Tag DIAMOND_ORES = register("diamond_ores");
    public static final Tag REDSTONE_ORES = register("redstone_ores");
    public static final Tag LAPIS_ORES = register("lapis_ores");
    public static final Tag COAL_ORES = register("coal_ores");
    public static final Tag EMERALD_ORES = register("emerald_ores");
    public static final Tag COPPER_ORES = register("copper_ores");
    public static final Tag CANDLES = register("candles");
    public static final Tag DIRT = register("dirt");
    public static final Tag TERRACOTTA = register("terracotta");
    public static final Tag BADLANDS_TERRACOTTA = register("badlands_terracotta");
    public static final Tag CONCRETE_POWDER = register("concrete_powder");
    public static final Tag COMPLETES_FIND_TREE_TUTORIAL = register("completes_find_tree_tutorial");
    public static final Tag FLOWER_POTS = register("flower_pots");
    public static final Tag ENDERMAN_HOLDABLE = register("enderman_holdable");
    public static final Tag ICE = register("ice");
    public static final Tag VALID_SPAWN = register("valid_spawn");
    public static final Tag IMPERMEABLE = register("impermeable");
    public static final Tag UNDERWATER_BONEMEALS = register("underwater_bonemeals");
    public static final Tag CORAL_BLOCKS = register("coral_blocks");
    public static final Tag WALL_CORALS = register("wall_corals");
    public static final Tag CORAL_PLANTS = register("coral_plants");
    public static final Tag CORALS = register("corals");
    public static final Tag BAMBOO_PLANTABLE_ON = register("bamboo_plantable_on");
    public static final Tag STANDING_SIGNS = register("standing_signs");
    public static final Tag WALL_SIGNS = register("wall_signs");
    public static final Tag SIGNS = register("signs");
    public static final Tag CEILING_HANGING_SIGNS = register("ceiling_hanging_signs");
    public static final Tag WALL_HANGING_SIGNS = register("wall_hanging_signs");
    public static final Tag ALL_HANGING_SIGNS = register("all_hanging_signs");
    public static final Tag ALL_SIGNS = register("all_signs");
    public static final Tag DRAGON_IMMUNE = register("dragon_immune");
    public static final Tag DRAGON_TRANSPARENT = register("dragon_transparent");
    public static final Tag WITHER_IMMUNE = register("wither_immune");
    public static final Tag WITHER_SUMMON_BASE_BLOCKS = register("wither_summon_base_blocks");
    public static final Tag BEEHIVES = register("beehives");
    public static final Tag CROPS = register("crops");
    public static final Tag BEE_GROWABLES = register("bee_growables");
    public static final Tag PORTALS = register("portals");
    public static final Tag FIRE = register("fire");
    public static final Tag NYLIUM = register("nylium");
    public static final Tag BEACON_BASE_BLOCKS = register("beacon_base_blocks");
    public static final Tag SOUL_SPEED_BLOCKS = register("soul_speed_blocks");
    public static final Tag WALL_POST_OVERRIDE = register("wall_post_override");
    public static final Tag CLIMBABLE = register("climbable");
    public static final Tag FALL_DAMAGE_RESETTING = register("fall_damage_resetting");
    public static final Tag SHULKER_BOXES = register("shulker_boxes");
    public static final Tag HOGLIN_REPELLENTS = register("hoglin_repellents");
    public static final Tag SOUL_FIRE_BASE_BLOCKS = register("soul_fire_base_blocks");
    public static final Tag STRIDER_WARM_BLOCKS = register("strider_warm_blocks");
    public static final Tag CAMPFIRES = register("campfires");
    public static final Tag GUARDED_BY_PIGLINS = register("guarded_by_piglins");
    public static final Tag PREVENT_MOB_SPAWNING_INSIDE = register("prevent_mob_spawning_inside");
    public static final Tag FENCE_GATES = register("fence_gates");
    public static final Tag UNSTABLE_BOTTOM_CENTER = register("unstable_bottom_center");
    public static final Tag MUSHROOM_GROW_BLOCK = register("mushroom_grow_block");
    public static final Tag INFINIBURN_OVERWORLD = register("infiniburn_overworld");
    public static final Tag INFINIBURN_NETHER = register("infiniburn_nether");
    public static final Tag INFINIBURN_END = register("infiniburn_end");
    public static final Tag BASE_STONE_OVERWORLD = register("base_stone_overworld");
    public static final Tag STONE_ORE_REPLACEABLES = register("stone_ore_replaceables");
    public static final Tag DEEPSLATE_ORE_REPLACEABLES = register("deepslate_ore_replaceables");
    public static final Tag BASE_STONE_NETHER = register("base_stone_nether");
    public static final Tag OVERWORLD_CARVER_REPLACEABLES = register("overworld_carver_replaceables");
    public static final Tag NETHER_CARVER_REPLACEABLES = register("nether_carver_replaceables");
    public static final Tag CANDLE_CAKES = register("candle_cakes");
    public static final Tag CAULDRONS = register("cauldrons");
    public static final Tag CRYSTAL_SOUND_BLOCKS = register("crystal_sound_blocks");
    public static final Tag INSIDE_STEP_SOUND_BLOCKS = register("inside_step_sound_blocks");
    public static final Tag COMBINATION_STEP_SOUND_BLOCKS = register("combination_step_sound_blocks");
    public static final Tag CAMEL_SAND_STEP_SOUND_BLOCKS = register("camel_sand_step_sound_blocks");
    public static final Tag OCCLUDES_VIBRATION_SIGNALS = register("occludes_vibration_signals");
    public static final Tag DAMPENS_VIBRATIONS = register("dampens_vibrations");
    public static final Tag DRIPSTONE_REPLACEABLE_BLOCKS = register("dripstone_replaceable_blocks");
    public static final Tag CAVE_VINES = register("cave_vines");
    public static final Tag MOSS_REPLACEABLE = register("moss_replaceable");
    public static final Tag LUSH_GROUND_REPLACEABLE = register("lush_ground_replaceable");
    public static final Tag AZALEA_ROOT_REPLACEABLE = register("azalea_root_replaceable");
    public static final Tag SMALL_DRIPLEAF_PLACEABLE = register("small_dripleaf_placeable");
    public static final Tag BIG_DRIPLEAF_PLACEABLE = register("big_dripleaf_placeable");
    public static final Tag SNOW = register("snow");
    public static final Tag MINEABLE_AXE = register("mineable/axe");
    public static final Tag MINEABLE_HOE = register("mineable/hoe");
    public static final Tag MINEABLE_PICKAXE = register("mineable/pickaxe");
    public static final Tag MINEABLE_SHOVEL = register("mineable/shovel");
    public static final Tag SWORD_EFFICIENT = register("sword_efficient");
    public static final Tag NEEDS_DIAMOND_TOOL = register("needs_diamond_tool");
    public static final Tag NEEDS_IRON_TOOL = register("needs_iron_tool");
    public static final Tag NEEDS_STONE_TOOL = register("needs_stone_tool");
    public static final Tag INCORRECT_FOR_NETHERITE_TOOL = register("incorrect_for_netherite_tool");
    public static final Tag INCORRECT_FOR_DIAMOND_TOOL = register("incorrect_for_diamond_tool");
    public static final Tag INCORRECT_FOR_IRON_TOOL = register("incorrect_for_iron_tool");
    public static final Tag INCORRECT_FOR_STONE_TOOL = register("incorrect_for_stone_tool");
    public static final Tag INCORRECT_FOR_GOLD_TOOL = register("incorrect_for_gold_tool");
    public static final Tag INCORRECT_FOR_WOODEN_TOOL = register("incorrect_for_wooden_tool");
    public static final Tag FEATURES_CANNOT_REPLACE = register("features_cannot_replace");
    public static final Tag LAVA_POOL_STONE_CANNOT_REPLACE = register("lava_pool_stone_cannot_replace");
    public static final Tag GEODE_INVALID_BLOCKS = register("geode_invalid_blocks");
    public static final Tag FROG_PREFER_JUMP_TO = register("frog_prefer_jump_to");
    public static final Tag SCULK_REPLACEABLE = register("sculk_replaceable");
    public static final Tag SCULK_REPLACEABLE_WORLD_GEN = register("sculk_replaceable_world_gen");
    public static final Tag ANCIENT_CITY_REPLACEABLE = register("ancient_city_replaceable");
    public static final Tag VIBRATION_RESONATORS = register("vibration_resonators");
    public static final Tag ANIMALS_SPAWNABLE_ON = register("animals_spawnable_on");
    public static final Tag ARMADILLO_SPAWNABLE_ON = register("armadillo_spawnable_on");
    public static final Tag AXOLOTLS_SPAWNABLE_ON = register("axolotls_spawnable_on");
    public static final Tag GOATS_SPAWNABLE_ON = register("goats_spawnable_on");
    public static final Tag MOOSHROOMS_SPAWNABLE_ON = register("mooshrooms_spawnable_on");
    public static final Tag PARROTS_SPAWNABLE_ON = register("parrots_spawnable_on");
    public static final Tag POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = register("polar_bears_spawnable_on_alternate");
    public static final Tag RABBITS_SPAWNABLE_ON = register("rabbits_spawnable_on");
    public static final Tag FOXES_SPAWNABLE_ON = register("foxes_spawnable_on");
    public static final Tag WOLVES_SPAWNABLE_ON = register("wolves_spawnable_on");
    public static final Tag FROGS_SPAWNABLE_ON = register("frogs_spawnable_on");
    public static final Tag AZALEA_GROWS_ON = register("azalea_grows_on");
    public static final Tag CONVERTABLE_TO_MUD = register("convertable_to_mud");
    public static final Tag MANGROVE_LOGS_CAN_GROW_THROUGH = register("mangrove_logs_can_grow_through");
    public static final Tag MANGROVE_ROOTS_CAN_GROW_THROUGH = register("mangrove_roots_can_grow_through");
    public static final Tag DEAD_BUSH_MAY_PLACE_ON = register("dead_bush_may_place_on");
    public static final Tag SNAPS_GOAT_HORN = register("snaps_goat_horn");
    public static final Tag REPLACEABLE_BY_TREES = register("replaceable_by_trees");
    public static final Tag SNOW_LAYER_CANNOT_SURVIVE_ON = register("snow_layer_cannot_survive_on");
    public static final Tag SNOW_LAYER_CAN_SURVIVE_ON = register("snow_layer_can_survive_on");
    public static final Tag INVALID_SPAWN_INSIDE = register("invalid_spawn_inside");
    public static final Tag SNIFFER_DIGGABLE_BLOCK = register("sniffer_diggable_block");
    public static final Tag SNIFFER_EGG_HATCH_BOOST = register("sniffer_egg_hatch_boost");
    public static final Tag TRAIL_RUINS_REPLACEABLE = register("trail_ruins_replaceable");
    public static final Tag REPLACEABLE = register("replaceable");
    public static final Tag ENCHANTMENT_POWER_PROVIDER = register("enchantment_power_provider");
    public static final Tag ENCHANTMENT_POWER_TRANSMITTER = register("enchantment_power_transmitter");
    public static final Tag MAINTAINS_FARMLAND = register("maintains_farmland");
    public static final Tag BLOCKS_WIND_CHARGE_EXPLOSIONS = register("blocks_wind_charge_explosions");
    public static final Tag DOES_NOT_BLOCK_HOPPERS = register("does_not_block_hoppers");
    public static final Tag AIR = register("air");

    private BlockTag() {}

    private static Tag register(String name) {
        return TagRegistry.BLOCK.registerVanillaTag(MinecraftKey.key(name));
    }

    public static void init() {
        // no-op
    }
}
