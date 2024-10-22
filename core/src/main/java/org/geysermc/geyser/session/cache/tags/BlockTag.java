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

import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.util.MinecraftKey;

/**
 * Lists vanilla block tags.
 */
@SuppressWarnings("unused")
public final class BlockTag {
    public static final Tag<Block> WOOL = create("wool");
    public static final Tag<Block> PLANKS = create("planks");
    public static final Tag<Block> STONE_BRICKS = create("stone_bricks");
    public static final Tag<Block> WOODEN_BUTTONS = create("wooden_buttons");
    public static final Tag<Block> STONE_BUTTONS = create("stone_buttons");
    public static final Tag<Block> BUTTONS = create("buttons");
    public static final Tag<Block> WOOL_CARPETS = create("wool_carpets");
    public static final Tag<Block> WOODEN_DOORS = create("wooden_doors");
    public static final Tag<Block> MOB_INTERACTABLE_DOORS = create("mob_interactable_doors");
    public static final Tag<Block> WOODEN_STAIRS = create("wooden_stairs");
    public static final Tag<Block> WOODEN_SLABS = create("wooden_slabs");
    public static final Tag<Block> WOODEN_FENCES = create("wooden_fences");
    public static final Tag<Block> PRESSURE_PLATES = create("pressure_plates");
    public static final Tag<Block> WOODEN_PRESSURE_PLATES = create("wooden_pressure_plates");
    public static final Tag<Block> STONE_PRESSURE_PLATES = create("stone_pressure_plates");
    public static final Tag<Block> WOODEN_TRAPDOORS = create("wooden_trapdoors");
    public static final Tag<Block> DOORS = create("doors");
    public static final Tag<Block> SAPLINGS = create("saplings");
    public static final Tag<Block> LOGS_THAT_BURN = create("logs_that_burn");
    public static final Tag<Block> OVERWORLD_NATURAL_LOGS = create("overworld_natural_logs");
    public static final Tag<Block> LOGS = create("logs");
    public static final Tag<Block> DARK_OAK_LOGS = create("dark_oak_logs");
    public static final Tag<Block> OAK_LOGS = create("oak_logs");
    public static final Tag<Block> BIRCH_LOGS = create("birch_logs");
    public static final Tag<Block> ACACIA_LOGS = create("acacia_logs");
    public static final Tag<Block> CHERRY_LOGS = create("cherry_logs");
    public static final Tag<Block> JUNGLE_LOGS = create("jungle_logs");
    public static final Tag<Block> SPRUCE_LOGS = create("spruce_logs");
    public static final Tag<Block> MANGROVE_LOGS = create("mangrove_logs");
    public static final Tag<Block> CRIMSON_STEMS = create("crimson_stems");
    public static final Tag<Block> WARPED_STEMS = create("warped_stems");
    public static final Tag<Block> BAMBOO_BLOCKS = create("bamboo_blocks");
    public static final Tag<Block> WART_BLOCKS = create("wart_blocks");
    public static final Tag<Block> BANNERS = create("banners");
    public static final Tag<Block> SAND = create("sand");
    public static final Tag<Block> SMELTS_TO_GLASS = create("smelts_to_glass");
    public static final Tag<Block> STAIRS = create("stairs");
    public static final Tag<Block> SLABS = create("slabs");
    public static final Tag<Block> WALLS = create("walls");
    public static final Tag<Block> ANVIL = create("anvil");
    public static final Tag<Block> RAILS = create("rails");
    public static final Tag<Block> LEAVES = create("leaves");
    public static final Tag<Block> TRAPDOORS = create("trapdoors");
    public static final Tag<Block> SMALL_FLOWERS = create("small_flowers");
    public static final Tag<Block> BEDS = create("beds");
    public static final Tag<Block> FENCES = create("fences");
    public static final Tag<Block> TALL_FLOWERS = create("tall_flowers");
    public static final Tag<Block> FLOWERS = create("flowers");
    public static final Tag<Block> PIGLIN_REPELLENTS = create("piglin_repellents");
    public static final Tag<Block> GOLD_ORES = create("gold_ores");
    public static final Tag<Block> IRON_ORES = create("iron_ores");
    public static final Tag<Block> DIAMOND_ORES = create("diamond_ores");
    public static final Tag<Block> REDSTONE_ORES = create("redstone_ores");
    public static final Tag<Block> LAPIS_ORES = create("lapis_ores");
    public static final Tag<Block> COAL_ORES = create("coal_ores");
    public static final Tag<Block> EMERALD_ORES = create("emerald_ores");
    public static final Tag<Block> COPPER_ORES = create("copper_ores");
    public static final Tag<Block> CANDLES = create("candles");
    public static final Tag<Block> DIRT = create("dirt");
    public static final Tag<Block> TERRACOTTA = create("terracotta");
    public static final Tag<Block> BADLANDS_TERRACOTTA = create("badlands_terracotta");
    public static final Tag<Block> CONCRETE_POWDER = create("concrete_powder");
    public static final Tag<Block> COMPLETES_FIND_TREE_TUTORIAL = create("completes_find_tree_tutorial");
    public static final Tag<Block> FLOWER_POTS = create("flower_pots");
    public static final Tag<Block> ENDERMAN_HOLDABLE = create("enderman_holdable");
    public static final Tag<Block> ICE = create("ice");
    public static final Tag<Block> VALID_SPAWN = create("valid_spawn");
    public static final Tag<Block> IMPERMEABLE = create("impermeable");
    public static final Tag<Block> UNDERWATER_BONEMEALS = create("underwater_bonemeals");
    public static final Tag<Block> CORAL_BLOCKS = create("coral_blocks");
    public static final Tag<Block> WALL_CORALS = create("wall_corals");
    public static final Tag<Block> CORAL_PLANTS = create("coral_plants");
    public static final Tag<Block> CORALS = create("corals");
    public static final Tag<Block> BAMBOO_PLANTABLE_ON = create("bamboo_plantable_on");
    public static final Tag<Block> STANDING_SIGNS = create("standing_signs");
    public static final Tag<Block> WALL_SIGNS = create("wall_signs");
    public static final Tag<Block> SIGNS = create("signs");
    public static final Tag<Block> CEILING_HANGING_SIGNS = create("ceiling_hanging_signs");
    public static final Tag<Block> WALL_HANGING_SIGNS = create("wall_hanging_signs");
    public static final Tag<Block> ALL_HANGING_SIGNS = create("all_hanging_signs");
    public static final Tag<Block> ALL_SIGNS = create("all_signs");
    public static final Tag<Block> DRAGON_IMMUNE = create("dragon_immune");
    public static final Tag<Block> DRAGON_TRANSPARENT = create("dragon_transparent");
    public static final Tag<Block> WITHER_IMMUNE = create("wither_immune");
    public static final Tag<Block> WITHER_SUMMON_BASE_BLOCKS = create("wither_summon_base_blocks");
    public static final Tag<Block> BEEHIVES = create("beehives");
    public static final Tag<Block> CROPS = create("crops");
    public static final Tag<Block> BEE_GROWABLES = create("bee_growables");
    public static final Tag<Block> PORTALS = create("portals");
    public static final Tag<Block> FIRE = create("fire");
    public static final Tag<Block> NYLIUM = create("nylium");
    public static final Tag<Block> BEACON_BASE_BLOCKS = create("beacon_base_blocks");
    public static final Tag<Block> SOUL_SPEED_BLOCKS = create("soul_speed_blocks");
    public static final Tag<Block> WALL_POST_OVERRIDE = create("wall_post_override");
    public static final Tag<Block> CLIMBABLE = create("climbable");
    public static final Tag<Block> FALL_DAMAGE_RESETTING = create("fall_damage_resetting");
    public static final Tag<Block> SHULKER_BOXES = create("shulker_boxes");
    public static final Tag<Block> HOGLIN_REPELLENTS = create("hoglin_repellents");
    public static final Tag<Block> SOUL_FIRE_BASE_BLOCKS = create("soul_fire_base_blocks");
    public static final Tag<Block> STRIDER_WARM_BLOCKS = create("strider_warm_blocks");
    public static final Tag<Block> CAMPFIRES = create("campfires");
    public static final Tag<Block> GUARDED_BY_PIGLINS = create("guarded_by_piglins");
    public static final Tag<Block> PREVENT_MOB_SPAWNING_INSIDE = create("prevent_mob_spawning_inside");
    public static final Tag<Block> FENCE_GATES = create("fence_gates");
    public static final Tag<Block> UNSTABLE_BOTTOM_CENTER = create("unstable_bottom_center");
    public static final Tag<Block> MUSHROOM_GROW_BLOCK = create("mushroom_grow_block");
    public static final Tag<Block> INFINIBURN_OVERWORLD = create("infiniburn_overworld");
    public static final Tag<Block> INFINIBURN_NETHER = create("infiniburn_nether");
    public static final Tag<Block> INFINIBURN_END = create("infiniburn_end");
    public static final Tag<Block> BASE_STONE_OVERWORLD = create("base_stone_overworld");
    public static final Tag<Block> STONE_ORE_REPLACEABLES = create("stone_ore_replaceables");
    public static final Tag<Block> DEEPSLATE_ORE_REPLACEABLES = create("deepslate_ore_replaceables");
    public static final Tag<Block> BASE_STONE_NETHER = create("base_stone_nether");
    public static final Tag<Block> OVERWORLD_CARVER_REPLACEABLES = create("overworld_carver_replaceables");
    public static final Tag<Block> NETHER_CARVER_REPLACEABLES = create("nether_carver_replaceables");
    public static final Tag<Block> CANDLE_CAKES = create("candle_cakes");
    public static final Tag<Block> CAULDRONS = create("cauldrons");
    public static final Tag<Block> CRYSTAL_SOUND_BLOCKS = create("crystal_sound_blocks");
    public static final Tag<Block> INSIDE_STEP_SOUND_BLOCKS = create("inside_step_sound_blocks");
    public static final Tag<Block> COMBINATION_STEP_SOUND_BLOCKS = create("combination_step_sound_blocks");
    public static final Tag<Block> CAMEL_SAND_STEP_SOUND_BLOCKS = create("camel_sand_step_sound_blocks");
    public static final Tag<Block> OCCLUDES_VIBRATION_SIGNALS = create("occludes_vibration_signals");
    public static final Tag<Block> DAMPENS_VIBRATIONS = create("dampens_vibrations");
    public static final Tag<Block> DRIPSTONE_REPLACEABLE_BLOCKS = create("dripstone_replaceable_blocks");
    public static final Tag<Block> CAVE_VINES = create("cave_vines");
    public static final Tag<Block> MOSS_REPLACEABLE = create("moss_replaceable");
    public static final Tag<Block> LUSH_GROUND_REPLACEABLE = create("lush_ground_replaceable");
    public static final Tag<Block> AZALEA_ROOT_REPLACEABLE = create("azalea_root_replaceable");
    public static final Tag<Block> SMALL_DRIPLEAF_PLACEABLE = create("small_dripleaf_placeable");
    public static final Tag<Block> BIG_DRIPLEAF_PLACEABLE = create("big_dripleaf_placeable");
    public static final Tag<Block> SNOW = create("snow");
    public static final Tag<Block> MINEABLE_AXE = create("mineable/axe");
    public static final Tag<Block> MINEABLE_HOE = create("mineable/hoe");
    public static final Tag<Block> MINEABLE_PICKAXE = create("mineable/pickaxe");
    public static final Tag<Block> MINEABLE_SHOVEL = create("mineable/shovel");
    public static final Tag<Block> SWORD_EFFICIENT = create("sword_efficient");
    public static final Tag<Block> NEEDS_DIAMOND_TOOL = create("needs_diamond_tool");
    public static final Tag<Block> NEEDS_IRON_TOOL = create("needs_iron_tool");
    public static final Tag<Block> NEEDS_STONE_TOOL = create("needs_stone_tool");
    public static final Tag<Block> INCORRECT_FOR_NETHERITE_TOOL = create("incorrect_for_netherite_tool");
    public static final Tag<Block> INCORRECT_FOR_DIAMOND_TOOL = create("incorrect_for_diamond_tool");
    public static final Tag<Block> INCORRECT_FOR_IRON_TOOL = create("incorrect_for_iron_tool");
    public static final Tag<Block> INCORRECT_FOR_STONE_TOOL = create("incorrect_for_stone_tool");
    public static final Tag<Block> INCORRECT_FOR_GOLD_TOOL = create("incorrect_for_gold_tool");
    public static final Tag<Block> INCORRECT_FOR_WOODEN_TOOL = create("incorrect_for_wooden_tool");
    public static final Tag<Block> FEATURES_CANNOT_REPLACE = create("features_cannot_replace");
    public static final Tag<Block> LAVA_POOL_STONE_CANNOT_REPLACE = create("lava_pool_stone_cannot_replace");
    public static final Tag<Block> GEODE_INVALID_BLOCKS = create("geode_invalid_blocks");
    public static final Tag<Block> FROG_PREFER_JUMP_TO = create("frog_prefer_jump_to");
    public static final Tag<Block> SCULK_REPLACEABLE = create("sculk_replaceable");
    public static final Tag<Block> SCULK_REPLACEABLE_WORLD_GEN = create("sculk_replaceable_world_gen");
    public static final Tag<Block> ANCIENT_CITY_REPLACEABLE = create("ancient_city_replaceable");
    public static final Tag<Block> VIBRATION_RESONATORS = create("vibration_resonators");
    public static final Tag<Block> ANIMALS_SPAWNABLE_ON = create("animals_spawnable_on");
    public static final Tag<Block> ARMADILLO_SPAWNABLE_ON = create("armadillo_spawnable_on");
    public static final Tag<Block> AXOLOTLS_SPAWNABLE_ON = create("axolotls_spawnable_on");
    public static final Tag<Block> GOATS_SPAWNABLE_ON = create("goats_spawnable_on");
    public static final Tag<Block> MOOSHROOMS_SPAWNABLE_ON = create("mooshrooms_spawnable_on");
    public static final Tag<Block> PARROTS_SPAWNABLE_ON = create("parrots_spawnable_on");
    public static final Tag<Block> POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = create("polar_bears_spawnable_on_alternate");
    public static final Tag<Block> RABBITS_SPAWNABLE_ON = create("rabbits_spawnable_on");
    public static final Tag<Block> FOXES_SPAWNABLE_ON = create("foxes_spawnable_on");
    public static final Tag<Block> WOLVES_SPAWNABLE_ON = create("wolves_spawnable_on");
    public static final Tag<Block> FROGS_SPAWNABLE_ON = create("frogs_spawnable_on");
    public static final Tag<Block> AZALEA_GROWS_ON = create("azalea_grows_on");
    public static final Tag<Block> CONVERTABLE_TO_MUD = create("convertable_to_mud");
    public static final Tag<Block> MANGROVE_LOGS_CAN_GROW_THROUGH = create("mangrove_logs_can_grow_through");
    public static final Tag<Block> MANGROVE_ROOTS_CAN_GROW_THROUGH = create("mangrove_roots_can_grow_through");
    public static final Tag<Block> DEAD_BUSH_MAY_PLACE_ON = create("dead_bush_may_place_on");
    public static final Tag<Block> SNAPS_GOAT_HORN = create("snaps_goat_horn");
    public static final Tag<Block> REPLACEABLE_BY_TREES = create("replaceable_by_trees");
    public static final Tag<Block> SNOW_LAYER_CANNOT_SURVIVE_ON = create("snow_layer_cannot_survive_on");
    public static final Tag<Block> SNOW_LAYER_CAN_SURVIVE_ON = create("snow_layer_can_survive_on");
    public static final Tag<Block> INVALID_SPAWN_INSIDE = create("invalid_spawn_inside");
    public static final Tag<Block> SNIFFER_DIGGABLE_BLOCK = create("sniffer_diggable_block");
    public static final Tag<Block> SNIFFER_EGG_HATCH_BOOST = create("sniffer_egg_hatch_boost");
    public static final Tag<Block> TRAIL_RUINS_REPLACEABLE = create("trail_ruins_replaceable");
    public static final Tag<Block> REPLACEABLE = create("replaceable");
    public static final Tag<Block> ENCHANTMENT_POWER_PROVIDER = create("enchantment_power_provider");
    public static final Tag<Block> ENCHANTMENT_POWER_TRANSMITTER = create("enchantment_power_transmitter");
    public static final Tag<Block> MAINTAINS_FARMLAND = create("maintains_farmland");
    public static final Tag<Block> BLOCKS_WIND_CHARGE_EXPLOSIONS = create("blocks_wind_charge_explosions");
    public static final Tag<Block> DOES_NOT_BLOCK_HOPPERS = create("does_not_block_hoppers");
    public static final Tag<Block> AIR = create("air");

    private BlockTag() {}

    private static Tag<Block> create(String name) {
        return new Tag<>(JavaRegistries.BLOCK, MinecraftKey.key(name));
    }
}
