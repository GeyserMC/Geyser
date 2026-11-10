/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.gamerule;

import org.geysermc.geyser.registry.Registries;

public class GameRules {
    public static final GameRule<Boolean> ADVANCE_TIME = register(new GameRule.Bool("advance_time", GameRuleCategory.UPDATES, true));
    public static final GameRule<Boolean> ADVANCE_WEATHER = register(new GameRule.Bool("advance_weather", GameRuleCategory.UPDATES, true));
    public static final GameRule<Boolean> ALLOW_ENTERING_NETHER_USING_PORTALS = register(new GameRule.Bool("allow_entering_nether_using_portals", GameRuleCategory.MISC, true));
    public static final GameRule<Boolean> BLOCK_DROPS = register(new GameRule.Bool("block_drops", GameRuleCategory.DROPS, true));
    public static final GameRule<Boolean> BLOCK_EXPLOSION_DROP_DECAY = register(new GameRule.Bool("block_explosion_drop_decay", GameRuleCategory.DROPS, true));
    public static final GameRule<Boolean> COMMAND_BLOCKS_WORK = register(new GameRule.Bool("command_blocks_work", GameRuleCategory.MISC, true));
    public static final GameRule<Boolean> COMMAND_BLOCK_OUTPUT = register(new GameRule.Bool("command_block_output", GameRuleCategory.CHAT, true));
    public static final GameRule<Boolean> DROWNING_DAMAGE = register(new GameRule.Bool("drowning_damage", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> ELYTRA_MOVEMENT_CHECK = register(new GameRule.Bool("elytra_movement_check", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> ENDER_PEARLS_VANISH_ON_DEATH = register(new GameRule.Bool("ender_pearls_vanish_on_death", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> ENTITY_DROPS = register(new GameRule.Bool("entity_drops", GameRuleCategory.DROPS, true));
    public static final GameRule<Boolean> FALL_DAMAGE = register(new GameRule.Bool("fall_damage", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> FIRE_DAMAGE = register(new GameRule.Bool("fire_damage", GameRuleCategory.PLAYER, true));
    public static final GameRule<Integer> FIRE_SPREAD_RADIUS_AROUND_PLAYER = register(new GameRule.Int("fire_spread_radius_around_player", GameRuleCategory.UPDATES, -1, Integer.MAX_VALUE, 128));
    public static final GameRule<Boolean> FORGIVE_DEAD_PLAYERS = register(new GameRule.Bool("forgive_dead_players", GameRuleCategory.MOBS, true));
    public static final GameRule<Boolean> FREEZE_DAMAGE = register(new GameRule.Bool("freeze_damage", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> GLOBAL_SOUND_EVENTS = register(new GameRule.Bool("global_sound_events", GameRuleCategory.MISC, true));
    public static final GameRule<Boolean> IMMEDIATE_RESPAWN = register(new GameRule.Bool("immediate_respawn", GameRuleCategory.PLAYER, false));
    public static final GameRule<Boolean> KEEP_INVENTORY = register(new GameRule.Bool("keep_inventory", GameRuleCategory.PLAYER, false));
    public static final GameRule<Boolean> LAVA_SOURCE_CONVERSION = register(new GameRule.Bool("lava_source_conversion", GameRuleCategory.UPDATES, false));
    public static final GameRule<Boolean> LIMITED_CRAFTING = register(new GameRule.Bool("limited_crafting", GameRuleCategory.PLAYER, false));
    public static final GameRule<Boolean> LOCATOR_BAR = register(new GameRule.Bool("locator_bar", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> LOG_ADMIN_COMMANDS = register(new GameRule.Bool("log_admin_commands", GameRuleCategory.CHAT, true));
    public static final GameRule<Integer> MAX_BLOCK_MODIFICATIONS = register(new GameRule.Int("max_block_modifications", GameRuleCategory.MISC, 1, Integer.MAX_VALUE, 32768));
    public static final GameRule<Integer> MAX_COMMAND_FORKS = register(new GameRule.Int("max_command_forks", GameRuleCategory.MISC, 0, Integer.MAX_VALUE, 65536));
    public static final GameRule<Integer> MAX_COMMAND_SEQUENCE_LENGTH = register(new GameRule.Int("max_command_sequence_length", GameRuleCategory.MISC, 0, Integer.MAX_VALUE, 65536));
    public static final GameRule<Integer> MAX_ENTITY_CRAMMING = register(new GameRule.Int("max_entity_cramming", GameRuleCategory.MOBS, 0, Integer.MAX_VALUE, 24));
    public static final GameRule<Integer> MAX_MINECART_SPEED = register(new GameRule.Int("max_minecart_speed", GameRuleCategory.MISC, 1, 1000, 8));
    public static final GameRule<Integer> MAX_SNOW_ACCUMULATION_HEIGHT = register(new GameRule.Int("max_snow_accumulation_height", GameRuleCategory.UPDATES, 0, 8, 1));
    public static final GameRule<Boolean> MOB_DROPS = register(new GameRule.Bool("mob_drops", GameRuleCategory.DROPS, true));
    public static final GameRule<Boolean> MOB_EXPLOSION_DROP_DECAY = register(new GameRule.Bool("mob_explosion_drop_decay", GameRuleCategory.DROPS, true));
    public static final GameRule<Boolean> MOB_GRIEFING = register(new GameRule.Bool("mob_griefing", GameRuleCategory.MOBS, true));
    public static final GameRule<Boolean> NATURAL_HEALTH_REGENERATION = register(new GameRule.Bool("natural_health_regeneration", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> PLAYER_MOVEMENT_CHECK = register(new GameRule.Bool("player_movement_check", GameRuleCategory.PLAYER, true));
    public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = register(new GameRule.Int("players_nether_portal_creative_delay", GameRuleCategory.PLAYER, 0, Integer.MAX_VALUE, 0));
    public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = register(new GameRule.Int("players_nether_portal_default_delay", GameRuleCategory.PLAYER, 0, Integer.MAX_VALUE, 80));
    public static final GameRule<Integer> PLAYERS_SLEEPING_PERCENTAGE = register(new GameRule.Int("players_sleeping_percentage", GameRuleCategory.PLAYER, 0, Integer.MAX_VALUE, 100));
    public static final GameRule<Boolean> PROJECTILES_CAN_BREAK_BLOCKS = register(new GameRule.Bool("projectiles_can_break_blocks", GameRuleCategory.DROPS, true));
    public static final GameRule<Boolean> PVP = register(new GameRule.Bool("pvp", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> RAIDS = register(new GameRule.Bool("raids", GameRuleCategory.MOBS, true));
    public static final GameRule<Integer> RANDOM_TICK_SPEED = register(new GameRule.Int("random_tick_speed", GameRuleCategory.UPDATES, 0, Integer.MAX_VALUE, 3));
    public static final GameRule<Boolean> REDUCED_DEBUG_INFO = register(new GameRule.Bool("reduced_debug_info", GameRuleCategory.MISC, false));
    public static final GameRule<Integer> RESPAWN_RADIUS = register(new GameRule.Int("respawn_radius", GameRuleCategory.PLAYER, 0, Integer.MAX_VALUE, 10));
    public static final GameRule<Boolean> SEND_COMMAND_FEEDBACK = register(new GameRule.Bool("send_command_feedback", GameRuleCategory.CHAT, true));
    public static final GameRule<Boolean> SHOW_ADVANCEMENT_MESSAGES = register(new GameRule.Bool("show_advancement_messages", GameRuleCategory.CHAT, true));
    public static final GameRule<Boolean> SHOW_DEATH_MESSAGES = register(new GameRule.Bool("show_death_messages", GameRuleCategory.CHAT, true));
    public static final GameRule<Boolean> SPAWNER_BLOCKS_WORK = register(new GameRule.Bool("spawner_blocks_work", GameRuleCategory.MISC, true));
    public static final GameRule<Boolean> SPAWN_MOBS = register(new GameRule.Bool("spawn_mobs", GameRuleCategory.SPAWNING, true));
    public static final GameRule<Boolean> SPAWN_MONSTERS = register(new GameRule.Bool("spawn_monsters", GameRuleCategory.SPAWNING, true));
    public static final GameRule<Boolean> SPAWN_PATROLS = register(new GameRule.Bool("spawn_patrols", GameRuleCategory.SPAWNING, true));
    public static final GameRule<Boolean> SPAWN_PHANTOMS = register(new GameRule.Bool("spawn_phantoms", GameRuleCategory.SPAWNING, true));
    public static final GameRule<Boolean> SPAWN_WANDERING_TRADERS = register(new GameRule.Bool("spawn_wandering_traders", GameRuleCategory.SPAWNING, true));
    public static final GameRule<Boolean> SPAWN_WARDENS = register(new GameRule.Bool("spawn_wardens", GameRuleCategory.SPAWNING, true));
    public static final GameRule<Boolean> SPECTATORS_GENERATE_CHUNKS = register(new GameRule.Bool("spectators_generate_chunks", GameRuleCategory.PLAYER, true));
    public static final GameRule<Boolean> SPREAD_VINES = register(new GameRule.Bool("spread_vines", GameRuleCategory.UPDATES, true));
    public static final GameRule<Boolean> TNT_EXPLODES = register(new GameRule.Bool("tnt_explodes", GameRuleCategory.MISC, true));
    public static final GameRule<Boolean> TNT_EXPLOSION_DROP_DECAY = register(new GameRule.Bool("tnt_explosion_drop_decay", GameRuleCategory.DROPS, false));
    public static final GameRule<Boolean> UNIVERSAL_ANGER = register(new GameRule.Bool("universal_anger", GameRuleCategory.MOBS, false));
    public static final GameRule<Boolean> WATER_SOURCE_CONVERSION = register(new GameRule.Bool("water_source_conversion", GameRuleCategory.UPDATES, true));

    public static <T> GameRule<T> register(GameRule<T> gameRule) {
        Registries.GAME_RULES.register(gameRule.key(), gameRule);
        return gameRule;
    }

    public static void init() {
        // no-op
    }
}
