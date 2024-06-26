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
import org.geysermc.geyser.util.Ordered;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class BlockTag implements Ordered {
    public static final Map<Key, BlockTag> ALL_BLOCK_TAGS = new HashMap<>();

    public static final BlockTag WOOL = new BlockTag("wool");
    public static final BlockTag PLANKS = new BlockTag("planks");
    public static final BlockTag STONE_BRICKS = new BlockTag("stone_bricks");
    public static final BlockTag WOODEN_BUTTONS = new BlockTag("wooden_buttons");
    public static final BlockTag STONE_BUTTONS = new BlockTag("stone_buttons");
    public static final BlockTag BUTTONS = new BlockTag("buttons");
    public static final BlockTag WOOL_CARPETS = new BlockTag("wool_carpets");
    public static final BlockTag WOODEN_DOORS = new BlockTag("wooden_doors");
    public static final BlockTag MOB_INTERACTABLE_DOORS = new BlockTag("mob_interactable_doors");
    public static final BlockTag WOODEN_STAIRS = new BlockTag("wooden_stairs");
    public static final BlockTag WOODEN_SLABS = new BlockTag("wooden_slabs");
    public static final BlockTag WOODEN_FENCES = new BlockTag("wooden_fences");
    public static final BlockTag PRESSURE_PLATES = new BlockTag("pressure_plates");
    public static final BlockTag WOODEN_PRESSURE_PLATES = new BlockTag("wooden_pressure_plates");
    public static final BlockTag STONE_PRESSURE_PLATES = new BlockTag("stone_pressure_plates");
    public static final BlockTag WOODEN_TRAPDOORS = new BlockTag("wooden_trapdoors");
    public static final BlockTag DOORS = new BlockTag("doors");
    public static final BlockTag SAPLINGS = new BlockTag("saplings");
    public static final BlockTag LOGS_THAT_BURN = new BlockTag("logs_that_burn");
    public static final BlockTag OVERWORLD_NATURAL_LOGS = new BlockTag("overworld_natural_logs");
    public static final BlockTag LOGS = new BlockTag("logs");
    public static final BlockTag DARK_OAK_LOGS = new BlockTag("dark_oak_logs");
    public static final BlockTag OAK_LOGS = new BlockTag("oak_logs");
    public static final BlockTag BIRCH_LOGS = new BlockTag("birch_logs");
    public static final BlockTag ACACIA_LOGS = new BlockTag("acacia_logs");
    public static final BlockTag CHERRY_LOGS = new BlockTag("cherry_logs");
    public static final BlockTag JUNGLE_LOGS = new BlockTag("jungle_logs");
    public static final BlockTag SPRUCE_LOGS = new BlockTag("spruce_logs");
    public static final BlockTag MANGROVE_LOGS = new BlockTag("mangrove_logs");
    public static final BlockTag CRIMSON_STEMS = new BlockTag("crimson_stems");
    public static final BlockTag WARPED_STEMS = new BlockTag("warped_stems");
    public static final BlockTag BAMBOO_BLOCKS = new BlockTag("bamboo_blocks");
    public static final BlockTag WART_BLOCKS = new BlockTag("wart_blocks");
    public static final BlockTag BANNERS = new BlockTag("banners");
    public static final BlockTag SAND = new BlockTag("sand");
    public static final BlockTag SMELTS_TO_GLASS = new BlockTag("smelts_to_glass");
    public static final BlockTag STAIRS = new BlockTag("stairs");
    public static final BlockTag SLABS = new BlockTag("slabs");
    public static final BlockTag WALLS = new BlockTag("walls");
    public static final BlockTag ANVIL = new BlockTag("anvil");
    public static final BlockTag RAILS = new BlockTag("rails");
    public static final BlockTag LEAVES = new BlockTag("leaves");
    public static final BlockTag TRAPDOORS = new BlockTag("trapdoors");
    public static final BlockTag SMALL_FLOWERS = new BlockTag("small_flowers");
    public static final BlockTag BEDS = new BlockTag("beds");
    public static final BlockTag FENCES = new BlockTag("fences");
    public static final BlockTag TALL_FLOWERS = new BlockTag("tall_flowers");
    public static final BlockTag FLOWERS = new BlockTag("flowers");
    public static final BlockTag PIGLIN_REPELLENTS = new BlockTag("piglin_repellents");
    public static final BlockTag GOLD_ORES = new BlockTag("gold_ores");
    public static final BlockTag IRON_ORES = new BlockTag("iron_ores");
    public static final BlockTag DIAMOND_ORES = new BlockTag("diamond_ores");
    public static final BlockTag REDSTONE_ORES = new BlockTag("redstone_ores");
    public static final BlockTag LAPIS_ORES = new BlockTag("lapis_ores");
    public static final BlockTag COAL_ORES = new BlockTag("coal_ores");
    public static final BlockTag EMERALD_ORES = new BlockTag("emerald_ores");
    public static final BlockTag COPPER_ORES = new BlockTag("copper_ores");
    public static final BlockTag CANDLES = new BlockTag("candles");
    public static final BlockTag DIRT = new BlockTag("dirt");
    public static final BlockTag TERRACOTTA = new BlockTag("terracotta");
    public static final BlockTag BADLANDS_TERRACOTTA = new BlockTag("badlands_terracotta");
    public static final BlockTag CONCRETE_POWDER = new BlockTag("concrete_powder");
    public static final BlockTag COMPLETES_FIND_TREE_TUTORIAL = new BlockTag("completes_find_tree_tutorial");
    public static final BlockTag FLOWER_POTS = new BlockTag("flower_pots");
    public static final BlockTag ENDERMAN_HOLDABLE = new BlockTag("enderman_holdable");
    public static final BlockTag ICE = new BlockTag("ice");
    public static final BlockTag VALID_SPAWN = new BlockTag("valid_spawn");
    public static final BlockTag IMPERMEABLE = new BlockTag("impermeable");
    public static final BlockTag UNDERWATER_BONEMEALS = new BlockTag("underwater_bonemeals");
    public static final BlockTag CORAL_BLOCKS = new BlockTag("coral_blocks");
    public static final BlockTag WALL_CORALS = new BlockTag("wall_corals");
    public static final BlockTag CORAL_PLANTS = new BlockTag("coral_plants");
    public static final BlockTag CORALS = new BlockTag("corals");
    public static final BlockTag BAMBOO_PLANTABLE_ON = new BlockTag("bamboo_plantable_on");
    public static final BlockTag STANDING_SIGNS = new BlockTag("standing_signs");
    public static final BlockTag WALL_SIGNS = new BlockTag("wall_signs");
    public static final BlockTag SIGNS = new BlockTag("signs");
    public static final BlockTag CEILING_HANGING_SIGNS = new BlockTag("ceiling_hanging_signs");
    public static final BlockTag WALL_HANGING_SIGNS = new BlockTag("wall_hanging_signs");
    public static final BlockTag ALL_HANGING_SIGNS = new BlockTag("all_hanging_signs");
    public static final BlockTag ALL_SIGNS = new BlockTag("all_signs");
    public static final BlockTag DRAGON_IMMUNE = new BlockTag("dragon_immune");
    public static final BlockTag DRAGON_TRANSPARENT = new BlockTag("dragon_transparent");
    public static final BlockTag WITHER_IMMUNE = new BlockTag("wither_immune");
    public static final BlockTag WITHER_SUMMON_BASE_BLOCKS = new BlockTag("wither_summon_base_blocks");
    public static final BlockTag BEEHIVES = new BlockTag("beehives");
    public static final BlockTag CROPS = new BlockTag("crops");
    public static final BlockTag BEE_GROWABLES = new BlockTag("bee_growables");
    public static final BlockTag PORTALS = new BlockTag("portals");
    public static final BlockTag FIRE = new BlockTag("fire");
    public static final BlockTag NYLIUM = new BlockTag("nylium");
    public static final BlockTag BEACON_BASE_BLOCKS = new BlockTag("beacon_base_blocks");
    public static final BlockTag SOUL_SPEED_BLOCKS = new BlockTag("soul_speed_blocks");
    public static final BlockTag WALL_POST_OVERRIDE = new BlockTag("wall_post_override");
    public static final BlockTag CLIMBABLE = new BlockTag("climbable");
    public static final BlockTag FALL_DAMAGE_RESETTING = new BlockTag("fall_damage_resetting");
    public static final BlockTag SHULKER_BOXES = new BlockTag("shulker_boxes");
    public static final BlockTag HOGLIN_REPELLENTS = new BlockTag("hoglin_repellents");
    public static final BlockTag SOUL_FIRE_BASE_BLOCKS = new BlockTag("soul_fire_base_blocks");
    public static final BlockTag STRIDER_WARM_BLOCKS = new BlockTag("strider_warm_blocks");
    public static final BlockTag CAMPFIRES = new BlockTag("campfires");
    public static final BlockTag GUARDED_BY_PIGLINS = new BlockTag("guarded_by_piglins");
    public static final BlockTag PREVENT_MOB_SPAWNING_INSIDE = new BlockTag("prevent_mob_spawning_inside");
    public static final BlockTag FENCE_GATES = new BlockTag("fence_gates");
    public static final BlockTag UNSTABLE_BOTTOM_CENTER = new BlockTag("unstable_bottom_center");
    public static final BlockTag MUSHROOM_GROW_BLOCK = new BlockTag("mushroom_grow_block");
    public static final BlockTag INFINIBURN_OVERWORLD = new BlockTag("infiniburn_overworld");
    public static final BlockTag INFINIBURN_NETHER = new BlockTag("infiniburn_nether");
    public static final BlockTag INFINIBURN_END = new BlockTag("infiniburn_end");
    public static final BlockTag BASE_STONE_OVERWORLD = new BlockTag("base_stone_overworld");
    public static final BlockTag STONE_ORE_REPLACEABLES = new BlockTag("stone_ore_replaceables");
    public static final BlockTag DEEPSLATE_ORE_REPLACEABLES = new BlockTag("deepslate_ore_replaceables");
    public static final BlockTag BASE_STONE_NETHER = new BlockTag("base_stone_nether");
    public static final BlockTag OVERWORLD_CARVER_REPLACEABLES = new BlockTag("overworld_carver_replaceables");
    public static final BlockTag NETHER_CARVER_REPLACEABLES = new BlockTag("nether_carver_replaceables");
    public static final BlockTag CANDLE_CAKES = new BlockTag("candle_cakes");
    public static final BlockTag CAULDRONS = new BlockTag("cauldrons");
    public static final BlockTag CRYSTAL_SOUND_BLOCKS = new BlockTag("crystal_sound_blocks");
    public static final BlockTag INSIDE_STEP_SOUND_BLOCKS = new BlockTag("inside_step_sound_blocks");
    public static final BlockTag COMBINATION_STEP_SOUND_BLOCKS = new BlockTag("combination_step_sound_blocks");
    public static final BlockTag CAMEL_SAND_STEP_SOUND_BLOCKS = new BlockTag("camel_sand_step_sound_blocks");
    public static final BlockTag OCCLUDES_VIBRATION_SIGNALS = new BlockTag("occludes_vibration_signals");
    public static final BlockTag DAMPENS_VIBRATIONS = new BlockTag("dampens_vibrations");
    public static final BlockTag DRIPSTONE_REPLACEABLE_BLOCKS = new BlockTag("dripstone_replaceable_blocks");
    public static final BlockTag CAVE_VINES = new BlockTag("cave_vines");
    public static final BlockTag MOSS_REPLACEABLE = new BlockTag("moss_replaceable");
    public static final BlockTag LUSH_GROUND_REPLACEABLE = new BlockTag("lush_ground_replaceable");
    public static final BlockTag AZALEA_ROOT_REPLACEABLE = new BlockTag("azalea_root_replaceable");
    public static final BlockTag SMALL_DRIPLEAF_PLACEABLE = new BlockTag("small_dripleaf_placeable");
    public static final BlockTag BIG_DRIPLEAF_PLACEABLE = new BlockTag("big_dripleaf_placeable");
    public static final BlockTag SNOW = new BlockTag("snow");
    public static final BlockTag MINEABLE_AXE = new BlockTag("mineable/axe");
    public static final BlockTag MINEABLE_HOE = new BlockTag("mineable/hoe");
    public static final BlockTag MINEABLE_PICKAXE = new BlockTag("mineable/pickaxe");
    public static final BlockTag MINEABLE_SHOVEL = new BlockTag("mineable/shovel");
    public static final BlockTag SWORD_EFFICIENT = new BlockTag("sword_efficient");
    public static final BlockTag NEEDS_DIAMOND_TOOL = new BlockTag("needs_diamond_tool");
    public static final BlockTag NEEDS_IRON_TOOL = new BlockTag("needs_iron_tool");
    public static final BlockTag NEEDS_STONE_TOOL = new BlockTag("needs_stone_tool");
    public static final BlockTag INCORRECT_FOR_NETHERITE_TOOL = new BlockTag("incorrect_for_netherite_tool");
    public static final BlockTag INCORRECT_FOR_DIAMOND_TOOL = new BlockTag("incorrect_for_diamond_tool");
    public static final BlockTag INCORRECT_FOR_IRON_TOOL = new BlockTag("incorrect_for_iron_tool");
    public static final BlockTag INCORRECT_FOR_STONE_TOOL = new BlockTag("incorrect_for_stone_tool");
    public static final BlockTag INCORRECT_FOR_GOLD_TOOL = new BlockTag("incorrect_for_gold_tool");
    public static final BlockTag INCORRECT_FOR_WOODEN_TOOL = new BlockTag("incorrect_for_wooden_tool");
    public static final BlockTag FEATURES_CANNOT_REPLACE = new BlockTag("features_cannot_replace");
    public static final BlockTag LAVA_POOL_STONE_CANNOT_REPLACE = new BlockTag("lava_pool_stone_cannot_replace");
    public static final BlockTag GEODE_INVALID_BLOCKS = new BlockTag("geode_invalid_blocks");
    public static final BlockTag FROG_PREFER_JUMP_TO = new BlockTag("frog_prefer_jump_to");
    public static final BlockTag SCULK_REPLACEABLE = new BlockTag("sculk_replaceable");
    public static final BlockTag SCULK_REPLACEABLE_WORLD_GEN = new BlockTag("sculk_replaceable_world_gen");
    public static final BlockTag ANCIENT_CITY_REPLACEABLE = new BlockTag("ancient_city_replaceable");
    public static final BlockTag VIBRATION_RESONATORS = new BlockTag("vibration_resonators");
    public static final BlockTag ANIMALS_SPAWNABLE_ON = new BlockTag("animals_spawnable_on");
    public static final BlockTag ARMADILLO_SPAWNABLE_ON = new BlockTag("armadillo_spawnable_on");
    public static final BlockTag AXOLOTLS_SPAWNABLE_ON = new BlockTag("axolotls_spawnable_on");
    public static final BlockTag GOATS_SPAWNABLE_ON = new BlockTag("goats_spawnable_on");
    public static final BlockTag MOOSHROOMS_SPAWNABLE_ON = new BlockTag("mooshrooms_spawnable_on");
    public static final BlockTag PARROTS_SPAWNABLE_ON = new BlockTag("parrots_spawnable_on");
    public static final BlockTag POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = new BlockTag("polar_bears_spawnable_on_alternate");
    public static final BlockTag RABBITS_SPAWNABLE_ON = new BlockTag("rabbits_spawnable_on");
    public static final BlockTag FOXES_SPAWNABLE_ON = new BlockTag("foxes_spawnable_on");
    public static final BlockTag WOLVES_SPAWNABLE_ON = new BlockTag("wolves_spawnable_on");
    public static final BlockTag FROGS_SPAWNABLE_ON = new BlockTag("frogs_spawnable_on");
    public static final BlockTag AZALEA_GROWS_ON = new BlockTag("azalea_grows_on");
    public static final BlockTag CONVERTABLE_TO_MUD = new BlockTag("convertable_to_mud");
    public static final BlockTag MANGROVE_LOGS_CAN_GROW_THROUGH = new BlockTag("mangrove_logs_can_grow_through");
    public static final BlockTag MANGROVE_ROOTS_CAN_GROW_THROUGH = new BlockTag("mangrove_roots_can_grow_through");
    public static final BlockTag DEAD_BUSH_MAY_PLACE_ON = new BlockTag("dead_bush_may_place_on");
    public static final BlockTag SNAPS_GOAT_HORN = new BlockTag("snaps_goat_horn");
    public static final BlockTag REPLACEABLE_BY_TREES = new BlockTag("replaceable_by_trees");
    public static final BlockTag SNOW_LAYER_CANNOT_SURVIVE_ON = new BlockTag("snow_layer_cannot_survive_on");
    public static final BlockTag SNOW_LAYER_CAN_SURVIVE_ON = new BlockTag("snow_layer_can_survive_on");
    public static final BlockTag INVALID_SPAWN_INSIDE = new BlockTag("invalid_spawn_inside");
    public static final BlockTag SNIFFER_DIGGABLE_BLOCK = new BlockTag("sniffer_diggable_block");
    public static final BlockTag SNIFFER_EGG_HATCH_BOOST = new BlockTag("sniffer_egg_hatch_boost");
    public static final BlockTag TRAIL_RUINS_REPLACEABLE = new BlockTag("trail_ruins_replaceable");
    public static final BlockTag REPLACEABLE = new BlockTag("replaceable");
    public static final BlockTag ENCHANTMENT_POWER_PROVIDER = new BlockTag("enchantment_power_provider");
    public static final BlockTag ENCHANTMENT_POWER_TRANSMITTER = new BlockTag("enchantment_power_transmitter");
    public static final BlockTag MAINTAINS_FARMLAND = new BlockTag("maintains_farmland");
    public static final BlockTag BLOCKS_WIND_CHARGE_EXPLOSIONS = new BlockTag("blocks_wind_charge_explosions");
    public static final BlockTag DOES_NOT_BLOCK_HOPPERS = new BlockTag("does_not_block_hoppers");
    public static final BlockTag AIR = new BlockTag("air");

    private final int id;
    
    private BlockTag(String identifier) {
        this.id = ALL_BLOCK_TAGS.size();
        register(identifier, this);
    }

    @Override
    public int ordinal() {
        return id;
    }

    private static void register(String name, BlockTag tag) {
        ALL_BLOCK_TAGS.put(Key.key(name), tag);
    }
}
