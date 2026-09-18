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
    public static final Tag<Block> WOODEN_STAIRS = create("wooden_stairs");
    public static final Tag<Block> WOODEN_SLABS = create("wooden_slabs");
    public static final Tag<Block> WOODEN_FENCES = create("wooden_fences");
    public static final Tag<Block> FENCE_GATES = create("fence_gates");
    public static final Tag<Block> WOODEN_PRESSURE_PLATES = create("wooden_pressure_plates");
    public static final Tag<Block> WOODEN_SHELVES = create("wooden_shelves");
    public static final Tag<Block> DOORS = create("doors");
    public static final Tag<Block> SAPLINGS = create("saplings");
    public static final Tag<Block> BAMBOO_BLOCKS = create("bamboo_blocks");
    public static final Tag<Block> PALE_OAK_LOGS = create("pale_oak_logs");
    public static final Tag<Block> JUNGLE_LOGS = create("jungle_logs");
    public static final Tag<Block> WART_BLOCKS = create("wart_blocks");
    public static final Tag<Block> LOGS = create("logs");
    public static final Tag<Block> SAND = create("sand");
    public static final Tag<Block> SLABS = create("slabs");
    public static final Tag<Block> WALLS = create("walls");
    public static final Tag<Block> STAIRS = create("stairs");
    public static final Tag<Block> ANVIL = create("anvil");
    public static final Tag<Block> RAILS = create("rails");
    public static final Tag<Block> LEAVES = create("leaves");
    public static final Tag<Block> WOODEN_TRAPDOORS = create("wooden_trapdoors");
    public static final Tag<Block> TRAPDOORS = create("trapdoors");
    public static final Tag<Block> SMALL_FLOWERS = create("small_flowers");
    public static final Tag<Block> FLOWERS = create("flowers");
    public static final Tag<Block> BEDS = create("beds");
    public static final Tag<Block> FENCES = create("fences");
    public static final Tag<Block> SOUL_FIRE_BASE_BLOCKS = create("soul_fire_base_blocks");
    public static final Tag<Block> CANDLES = create("candles");
    public static final Tag<Block> DAMPENS_VIBRATIONS = create("dampens_vibrations");
    public static final Tag<Block> GOLD_ORES = create("gold_ores");
    public static final Tag<Block> IRON_ORES = create("iron_ores");
    public static final Tag<Block> COPPER_ORES = create("copper_ores");
    public static final Tag<Block> ORES = create("ores");
    public static final Tag<Block> DIRT = create("dirt");
    public static final Tag<Block> MUD = create("mud");
    public static final Tag<Block> MOSS_BLOCKS = create("moss_blocks");
    public static final Tag<Block> GRASS_BLOCKS = create("grass_blocks");
    public static final Tag<Block> TERRACOTTA = create("terracotta");
    public static final Tag<Block> GLAZED_TERRACOTTA = create("glazed_terracotta");
    public static final Tag<Block> CONCRETE = create("concrete");
    public static final Tag<Block> CONCRETE_SLABS = create("concrete_slabs");
    public static final Tag<Block> CONCRETE_STAIRS = create("concrete_stairs");
    public static final Tag<Block> CONCRETE_POWDERS = create("concrete_powders");
    public static final Tag<Block> COMPLETES_FIND_TREE_TUTORIAL = create("completes_find_tree_tutorial");
    public static final Tag<Block> SHULKER_BOXES = create("shulker_boxes");
    public static final Tag<Block> COPPER_CHESTS = create("copper_chests");
    public static final Tag<Block> LIGHTNING_RODS = create("lightning_rods");
    public static final Tag<Block> COPPER = create("copper");
    public static final Tag<Block> CHAINS = create("chains");
    public static final Tag<Block> COPPER_GOLEM_STATUES = create("copper_golem_statues");
    public static final Tag<Block> LANTERNS = create("lanterns");
    public static final Tag<Block> BARS = create("bars");
    public static final Tag<Block> SKULLS = create("skulls");
    public static final Tag<Block> WOOL_SLABS = create("wool_slabs");
    public static final Tag<Block> WOOL_STAIRS = create("wool_stairs");
    public static final Tag<Block> CEILING_HANGING_SIGNS = create("ceiling_hanging_signs");
    public static final Tag<Block> STANDING_SIGNS = create("standing_signs");
    public static final Tag<Block> BEE_ATTRACTIVE = create("bee_attractive");
    public static final Tag<Block> MOB_INTERACTABLE_DOORS = create("mob_interactable_doors");
    public static final Tag<Block> PRESSURE_PLATES = create("pressure_plates");
    public static final Tag<Block> STONE_PRESSURE_PLATES = create("stone_pressure_plates");
    public static final Tag<Block> OVERWORLD_NATURAL_LOGS = create("overworld_natural_logs");
    public static final Tag<Block> BANNERS = create("banners");
    public static final Tag<Block> PIGLIN_REPELLENTS = create("piglin_repellents");
    public static final Tag<Block> BADLANDS_TERRACOTTA = create("badlands_terracotta");
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
    public static final Tag<Block> WALL_SIGNS = create("wall_signs");
    public static final Tag<Block> SIGNS = create("signs");
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
    public static final Tag<Block> HOGLIN_REPELLENTS = create("hoglin_repellents");
    public static final Tag<Block> STRIDER_WARM_BLOCKS = create("strider_warm_blocks");
    public static final Tag<Block> CAMPFIRES = create("campfires");
    public static final Tag<Block> GUARDED_BY_PIGLINS = create("guarded_by_piglins");
    public static final Tag<Block> PREVENT_MOB_SPAWNING_INSIDE = create("prevent_mob_spawning_inside");
    public static final Tag<Block> UNSTABLE_BOTTOM_CENTER = create("unstable_bottom_center");
    public static final Tag<Block> EDIBLE_FOR_SHEEP = create("edible_for_sheep");
    public static final Tag<Block> CAN_GLIDE_THROUGH = create("can_glide_through");
    public static final Tag<Block> CAUSES_PERIODIC_GEYSER_ERUPTIONS = create("causes_periodic_geyser_eruptions");
    public static final Tag<Block> CAUSES_CONTINUOUS_GEYSER_ERUPTIONS = create("causes_continuous_geyser_eruptions");
    public static final Tag<Block> NETHER_PORTAL_FRAME = create("nether_portal_frame");
    public static final Tag<Block> CONDUIT_EFFECT_BLOCK = create("conduit_effect_block");
    public static final Tag<Block> INFINIBURN_OVERWORLD = create("infiniburn_overworld");
    public static final Tag<Block> INFINIBURN_NETHER = create("infiniburn_nether");
    public static final Tag<Block> INFINIBURN_END = create("infiniburn_end");
    public static final Tag<Block> SUBSTRATE_OVERWORLD = create("substrate_overworld");
    public static final Tag<Block> BASE_STONE_OVERWORLD = create("base_stone_overworld");
    public static final Tag<Block> STONE_ORE_REPLACEABLES = create("stone_ore_replaceables");
    public static final Tag<Block> HEIGHT_SPECIFIC_ORE_REPLACEABLES = create("height_specific_ore_replaceables");
    public static final Tag<Block> DEEPSLATE_ORE_REPLACEABLES = create("deepslate_ore_replaceables");
    public static final Tag<Block> BASE_STONE_NETHER = create("base_stone_nether");
    public static final Tag<Block> BENEATH_TREE_PODZOL_REPLACEABLE = create("beneath_tree_podzol_replaceable");
    public static final Tag<Block> BENEATH_BAMBOO_PODZOL_REPLACEABLE = create("beneath_bamboo_podzol_replaceable");
    public static final Tag<Block> CANNOT_REPLACE_BELOW_TREE_TRUNK = create("cannot_replace_below_tree_trunk");
    public static final Tag<Block> CANNOT_PLACE_BASALT_PILLAR_ON = create("cannot_place_basalt_pillar_on");
    public static final Tag<Block> UNCARVABLE = create("uncarvable");
    public static final Tag<Block> CANDLE_CAKES = create("candle_cakes");
    public static final Tag<Block> CAULDRONS = create("cauldrons");
    public static final Tag<Block> CRYSTAL_SOUND_BLOCKS = create("crystal_sound_blocks");
    public static final Tag<Block> INSIDE_STEP_SOUND_BLOCKS = create("inside_step_sound_blocks");
    public static final Tag<Block> COMBINATION_STEP_SOUND_BLOCKS = create("combination_step_sound_blocks");
    public static final Tag<Block> CAMEL_SAND_STEP_SOUND_BLOCKS = create("camel_sand_step_sound_blocks");
    public static final Tag<Block> HAPPY_GHAST_AVOIDS = create("happy_ghast_avoids");
    public static final Tag<Block> OCCLUDES_VIBRATION_SIGNALS = create("occludes_vibration_signals");
    public static final Tag<Block> CUSHION_USES_COLLISION_SHAPE = create("cushion_uses_collision_shape");
    public static final Tag<Block> DRIPSTONE_REPLACEABLE_BLOCKS = create("dripstone_replaceable_blocks");
    public static final Tag<Block> SULFUR_SPIKE_REPLACEABLE_BLOCKS = create("sulfur_spike_replaceable_blocks");
    public static final Tag<Block> CAVE_VINES = create("cave_vines");
    public static final Tag<Block> MOSS_REPLACEABLE = create("moss_replaceable");
    public static final Tag<Block> LUSH_GROUND_REPLACEABLE = create("lush_ground_replaceable");
    public static final Tag<Block> AZALEA_ROOT_REPLACEABLE = create("azalea_root_replaceable");
    public static final Tag<Block> ICE_SPIKE_REPLACEABLE = create("ice_spike_replaceable");
    public static final Tag<Block> FOREST_ROCK_CAN_PLACE_ON = create("forest_rock_can_place_on");
    public static final Tag<Block> HUGE_BROWN_MUSHROOM_CAN_PLACE_ON = create("huge_brown_mushroom_can_place_on");
    public static final Tag<Block> HUGE_RED_MUSHROOM_CAN_PLACE_ON = create("huge_red_mushroom_can_place_on");
    public static final Tag<Block> SNOW = create("snow");
    public static final Tag<Block> MINEABLE_AXE = create("mineable/axe");
    public static final Tag<Block> MINEABLE_HOE = create("mineable/hoe");
    public static final Tag<Block> MINEABLE_PICKAXE = create("mineable/pickaxe");
    public static final Tag<Block> MINEABLE_SHOVEL = create("mineable/shovel");
    public static final Tag<Block> SWORD_EFFICIENT = create("sword_efficient");
    public static final Tag<Block> SWORD_INSTANTLY_MINES = create("sword_instantly_mines");
    public static final Tag<Block> SHEARS_EXTREME_BREAKING_SPEED = create("shears_extreme_breaking_speed");
    public static final Tag<Block> SHEARS_MAJOR_BREAKING_SPEED = create("shears_major_breaking_speed");
    public static final Tag<Block> SHEARS_MINOR_BREAKING_SPEED = create("shears_minor_breaking_speed");
    public static final Tag<Block> TURNS_INTO_DIRT_PATH = create("turns_into_dirt_path");
    public static final Tag<Block> TURNS_INTO_FARMLAND = create("turns_into_farmland");
    public static final Tag<Block> NEEDS_DIAMOND_TOOL = create("needs_diamond_tool");
    public static final Tag<Block> NEEDS_IRON_TOOL = create("needs_iron_tool");
    public static final Tag<Block> NEEDS_STONE_TOOL = create("needs_stone_tool");
    public static final Tag<Block> INCORRECT_FOR_NETHERITE_TOOL = create("incorrect_for_netherite_tool");
    public static final Tag<Block> INCORRECT_FOR_DIAMOND_TOOL = create("incorrect_for_diamond_tool");
    public static final Tag<Block> INCORRECT_FOR_IRON_TOOL = create("incorrect_for_iron_tool");
    public static final Tag<Block> INCORRECT_FOR_COPPER_TOOL = create("incorrect_for_copper_tool");
    public static final Tag<Block> INCORRECT_FOR_STONE_TOOL = create("incorrect_for_stone_tool");
    public static final Tag<Block> INCORRECT_FOR_GOLD_TOOL = create("incorrect_for_gold_tool");
    public static final Tag<Block> INCORRECT_FOR_WOODEN_TOOL = create("incorrect_for_wooden_tool");
    public static final Tag<Block> FEATURES_CANNOT_REPLACE = create("features_cannot_replace");
    public static final Tag<Block> LAVA_POOL_STONE_CANNOT_REPLACE = create("lava_pool_stone_cannot_replace");
    public static final Tag<Block> GEODE_INVALID_BLOCKS = create("geode_invalid_blocks");
    public static final Tag<Block> FROG_PREFER_JUMP_TO = create("frog_prefer_jump_to");
    public static final Tag<Block> SCULK_GROWTH_INHIBITORS = create("sculk_growth_inhibitors");
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
    public static final Tag<Block> BATS_SPAWNABLE_ON = create("bats_spawnable_on");
    public static final Tag<Block> CAMELS_SPAWNABLE_ON = create("camels_spawnable_on");
    public static final Tag<Block> AZALEA_GROWS_ON = create("azalea_grows_on");
    public static final Tag<Block> CONVERTIBLE_TO_MUD = create("convertible_to_mud");
    public static final Tag<Block> MANGROVE_LOGS_CAN_GROW_THROUGH = create("mangrove_logs_can_grow_through");
    public static final Tag<Block> MANGROVE_ROOTS_CAN_GROW_THROUGH = create("mangrove_roots_can_grow_through");
    public static final Tag<Block> SNAPS_GOAT_HORN = create("snaps_goat_horn");
    public static final Tag<Block> REPLACEABLE_BY_TREES = create("replaceable_by_trees");
    public static final Tag<Block> REPLACEABLE_BY_MUSHROOMS = create("replaceable_by_mushrooms");
    public static final Tag<Block> ENABLES_BUBBLE_COLUMN_DRAG_DOWN = create("enables_bubble_column_drag_down");
    public static final Tag<Block> ENABLES_BUBBLE_COLUMN_PUSH_UP = create("enables_bubble_column_push_up");
    public static final Tag<Block> CATS_CAN_SIT_ON = create("cats_can_sit_on");
    public static final Tag<Block> CATS_CAN_LIE_ON = create("cats_can_lie_on");
    public static final Tag<Block> SPEEDS_UP_ZOMBIE_VILLAGER_CURING = create("speeds_up_zombie_villager_curing");
    public static final Tag<Block> VILLAGERS_CAN_SLEEP_ON_BED = create("villagers_can_sleep_on_bed");
    public static final Tag<Block> VILLAGER_BABIES_CAN_JUMP_ON_BED = create("villager_babies_can_jump_on_bed");
    public static final Tag<Block> SUPPORTS_VEGETATION = create("supports_vegetation");
    public static final Tag<Block> SUPPORTS_DRY_VEGETATION = create("supports_dry_vegetation");
    public static final Tag<Block> SUPPORTS_CROPS = create("supports_crops");
    public static final Tag<Block> SUPPORTS_STEM_CROPS = create("supports_stem_crops");
    public static final Tag<Block> SUPPORTS_STEM_FRUIT = create("supports_stem_fruit");
    public static final Tag<Block> SUPPORTS_PUMPKIN_STEM = create("supports_pumpkin_stem");
    public static final Tag<Block> SUPPORTS_MELON_STEM = create("supports_melon_stem");
    public static final Tag<Block> SUPPORTS_PUMPKIN_STEM_FRUIT = create("supports_pumpkin_stem_fruit");
    public static final Tag<Block> SUPPORTS_MELON_STEM_FRUIT = create("supports_melon_stem_fruit");
    public static final Tag<Block> SUPPORTS_SUGAR_CANE = create("supports_sugar_cane");
    public static final Tag<Block> SUPPORTS_SUGAR_CANE_ADJACENTLY = create("supports_sugar_cane_adjacently");
    public static final Tag<Block> SUPPORTS_BAMBOO = create("supports_bamboo");
    public static final Tag<Block> SUPPORTS_SMALL_DRIPLEAF = create("supports_small_dripleaf");
    public static final Tag<Block> SUPPORTS_BIG_DRIPLEAF = create("supports_big_dripleaf");
    public static final Tag<Block> SUPPORTS_CACTUS = create("supports_cactus");
    public static final Tag<Block> SUPPORTS_CHORUS_PLANT = create("supports_chorus_plant");
    public static final Tag<Block> SUPPORTS_CHORUS_FLOWER = create("supports_chorus_flower");
    public static final Tag<Block> SUPPORTS_NETHER_SPROUTS = create("supports_nether_sprouts");
    public static final Tag<Block> SUPPORTS_AZALEA = create("supports_azalea");
    public static final Tag<Block> SUPPORTS_WARPED_FUNGUS = create("supports_warped_fungus");
    public static final Tag<Block> SUPPORTS_CRIMSON_FUNGUS = create("supports_crimson_fungus");
    public static final Tag<Block> SUPPORTS_MANGROVE_PROPAGULE = create("supports_mangrove_propagule");
    public static final Tag<Block> SUPPORTS_HANGING_MANGROVE_PROPAGULE = create("supports_hanging_mangrove_propagule");
    public static final Tag<Block> SUPPORTS_NETHER_WART = create("supports_nether_wart");
    public static final Tag<Block> SUPPORTS_CRIMSON_ROOTS = create("supports_crimson_roots");
    public static final Tag<Block> SUPPORTS_WARPED_ROOTS = create("supports_warped_roots");
    public static final Tag<Block> SUPPORTS_WITHER_ROSE = create("supports_wither_rose");
    public static final Tag<Block> SUPPORTS_COCOA = create("supports_cocoa");
    public static final Tag<Block> SUPPORTS_LILY_PAD = create("supports_lily_pad");
    public static final Tag<Block> SUPPORTS_FROGSPAWN = create("supports_frogspawn");
    public static final Tag<Block> PREVENTS_NEARBY_LEAF_DECAY = create("prevents_nearby_leaf_decay");
    public static final Tag<Block> SUPPORT_OVERRIDE_CACTUS_FLOWER = create("support_override_cactus_flower");
    public static final Tag<Block> SUPPORT_OVERRIDE_SNOW_LAYER = create("support_override_snow_layer");
    public static final Tag<Block> CANNOT_SUPPORT_SNOW_LAYER = create("cannot_support_snow_layer");
    public static final Tag<Block> CANNOT_SUPPORT_SEAGRASS = create("cannot_support_seagrass");
    public static final Tag<Block> CANNOT_SUPPORT_KELP = create("cannot_support_kelp");
    public static final Tag<Block> OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT = create("overrides_mushroom_light_requirement");
    public static final Tag<Block> GROWS_CROPS = create("grows_crops");
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
    public static final Tag<Block> SUPPRESSES_BOUNCE = create("suppresses_bounce");
    public static final Tag<Block> TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS = create("triggers_ambient_desert_sand_block_sounds");
    public static final Tag<Block> TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS = create("triggers_ambient_desert_dry_vegetation_block_sounds");
    public static final Tag<Block> TRIGGERS_AMBIENT_DRIED_GHAST_BLOCK_SOUNDS = create("triggers_ambient_dried_ghast_block_sounds");
    public static final Tag<Block> REQUIRED_FOR_POPLAR_LEAF_AMBIENCE = create("required_for_poplar_leaf_ambience");
    public static final Tag<Block> SPELEOTHEMS = create("speleothems");
    public static final Tag<Block> DANGEROUS_FOR_TELEPORTATION = create("dangerous_for_teleportation");
    public static final Tag<Block> CAT_DOES_NOT_TELEPORT_TO = create("cat_does_not_teleport_to");
    public static final Tag<Block> ENDERMAN_DOES_NOT_TELEPORT_TO = create("enderman_does_not_teleport_to");
    public static final Tag<Block> SHULKER_DOES_NOT_TELEPORT_TO = create("shulker_does_not_teleport_to");
    public static final Tag<Block> CONSUMABLE_DOES_NOT_TELEPORT_TO = create("consumable_does_not_teleport_to");
    public static final Tag<Block> FOX_IMMUNE_TO = create("fox_immune_to");
    public static final Tag<Block> POLAR_BEAR_IMMUNE_TO = create("polar_bear_immune_to");
    public static final Tag<Block> SNOW_GOLEM_IMMUNE_TO = create("snow_golem_immune_to");
    public static final Tag<Block> STRAY_IMMUNE_TO = create("stray_immune_to");
    public static final Tag<Block> WITHER_IMMUNE_TO = create("wither_immune_to");
    public static final Tag<Block> WITHER_SKELETON_IMMUNE_TO = create("wither_skeleton_immune_to");
    public static final Tag<Block> DEFAULT_IMMUNE_TO = create("default_immune_to");
    public static final Tag<Block> BLOCKS_MOTION = create("blocks_motion");
    public static final Tag<Block> BLOCKS_MOTION_NO_LEAVES = create("blocks_motion_no_leaves");
    public static final Tag<Block> ENTITIES_CAN_TELEPORT_TO = create("entities_can_teleport_to");
    public static final Tag<Block> BLOCKS_DOLPHIN_JUMP = create("blocks_dolphin_jump");
    public static final Tag<Block> ICE_MELTS_WHEN_DESTROYED_ABOVE = create("ice_melts_when_destroyed_above");
    public static final Tag<Block> CAUSES_SUFFOCATION = create("causes_suffocation");
    public static final Tag<Block> BLOCKS_MOTION_IN_HEIGHTMAP = create("blocks_motion_in_heightmap");
    public static final Tag<Block> BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES = create("blocks_motion_in_heightmap_no_leaves");
    public static final Tag<Block> BLOCKS_LAVA_FIRE_SPREAD = create("blocks_lava_fire_spread");
    public static final Tag<Block> BLOCKS_FLUID_FLOW = create("blocks_fluid_flow");
    public static final Tag<Block> WASHED_AWAY_BY_FLUIDS = create("washed_away_by_fluids");
    public static final Tag<Block> AIR = create("air");

    private BlockTag() {}

    private static Tag<Block> create(String name) {
        return new Tag<>(JavaRegistries.BLOCK, MinecraftKey.key(name));
    }
}
