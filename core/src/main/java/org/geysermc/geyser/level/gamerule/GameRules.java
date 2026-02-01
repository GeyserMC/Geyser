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

public class GameRules {
    public static final GameRule<Boolean> ADVANCE_TIME = register(new GameRule.Bool("gamerule.minecraft.advance_time", "gamerule.doDaylightCycle", true));
    public static final GameRule<Boolean> ADVANCE_WEATHER = register(new GameRule.Bool("gamerule.minecraft.advance_weather", "gamerule.doWeatherCycle", true));
    public static final GameRule<Boolean> ALLOW_ENTERING_NETHER_USING_PORTALS = register(new GameRule.Bool("gamerule.minecraft.allow_entering_nether_using_portals", "gamerule.allowEnteringNetherUsingPortals", true));
    public static final GameRule<Boolean> BLOCK_DROPS = register(new GameRule.Bool("gamerule.minecraft.block_drops", "gamerule.doTileDrops", true));
    public static final GameRule<Boolean> BLOCK_EXPLOSION_DROP_DECAY = register(new GameRule.Bool("gamerule.minecraft.block_explosion_drop_decay", "gamerule.blockExplosionDropDecay", true));
    public static final GameRule<Boolean> COMMAND_BLOCKS_WORK = register(new GameRule.Bool("gamerule.minecraft.command_blocks_work", "gamerule.commandBlocksEnabled", true));
    public static final GameRule<Boolean> COMMAND_BLOCK_OUTPUT = register(new GameRule.Bool("gamerule.minecraft.command_block_output", "gamerule.commandBlockOutput", true));
    public static final GameRule<Boolean> DROWNING_DAMAGE = register(new GameRule.Bool("gamerule.minecraft.drowning_damage", "gamerule.drowningDamage", true));
    public static final GameRule<Boolean> ELYTRA_MOVEMENT_CHECK = register(new GameRule.Bool("gamerule.minecraft.elytra_movement_check", "gamerule.minecraft.elytra_movement_check", true));
    public static final GameRule<Boolean> ENDER_PEARLS_VANISH_ON_DEATH = register(new GameRule.Bool("gamerule.minecraft.ender_pearls_vanish_on_death", "gamerule.enderPearlsVanishOnDeath", true));
    public static final GameRule<Boolean> ENTITY_DROPS = register(new GameRule.Bool("gamerule.minecraft.entity_drops", "gamerule.doEntityDrops", true));
    public static final GameRule<Boolean> FALL_DAMAGE = register(new GameRule.Bool("gamerule.minecraft.fall_damage", "gamerule.fallDamage", true));
    public static final GameRule<Boolean> FIRE_DAMAGE = register(new GameRule.Bool("gamerule.minecraft.fire_damage", "gamerule.fireDamage", true));
    public static final GameRule<Integer> FIRE_SPREAD_RADIUS_AROUND_PLAYER = register(new GameRule.Int("gamerule.minecraft.fire_spread_radius_around_player", "gamerule.minecraft.fire_spread_radius_around_player", -1, Integer.MAX_VALUE, 128));
    public static final GameRule<Boolean> FORGIVE_DEAD_PLAYERS = register(new GameRule.Bool("gamerule.minecraft.forgive_dead_players", "gamerule.forgiveDeadPlayers", true));
    public static final GameRule<Boolean> FREEZE_DAMAGE = register(new GameRule.Bool("gamerule.minecraft.freeze_damage", "gamerule.freezeDamage", true));
    public static final GameRule<Boolean> GLOBAL_SOUND_EVENTS = register(new GameRule.Bool("gamerule.minecraft.global_sound_events", "gamerule.globalSoundEvents", true));
    public static final GameRule<Boolean> IMMEDIATE_RESPAWN = register(new GameRule.Bool("gamerule.minecraft.immediate_respawn", "gamerule.doImmediateRespawn", false));
    public static final GameRule<Boolean> KEEP_INVENTORY = register(new GameRule.Bool("gamerule.minecraft.keep_inventory", "gamerule.keepInventory", false));
    public static final GameRule<Boolean> LAVA_SOURCE_CONVERSION = register(new GameRule.Bool("gamerule.minecraft.lava_source_conversion", "gamerule.lavaSourceConversion", false));
    public static final GameRule<Boolean> LIMITED_CRAFTING = register(new GameRule.Bool("gamerule.minecraft.limited_crafting", "gamerule.doLimitedCrafting", false));
    public static final GameRule<Boolean> LOCATOR_BAR = register(new GameRule.Bool("gamerule.minecraft.locator_bar", "gamerule.locatorBar", true));
    public static final GameRule<Boolean> LOG_ADMIN_COMMANDS = register(new GameRule.Bool("gamerule.minecraft.log_admin_commands", "gamerule.logAdminCommands", true));
    public static final GameRule<Integer> MAX_BLOCK_MODIFICATIONS = register(new GameRule.Int("gamerule.minecraft.max_block_modifications", "gamerule.commandModificationBlockLimit", 1, Integer.MAX_VALUE, 32768));
    public static final GameRule<Integer> MAX_COMMAND_FORKS = register(new GameRule.Int("gamerule.minecraft.max_command_forks", "gamerule.maxCommandForkCount", 0, Integer.MAX_VALUE, 65536));
    public static final GameRule<Integer> MAX_COMMAND_SEQUENCE_LENGTH = register(new GameRule.Int("gamerule.minecraft.max_command_sequence_length", "gamerule.maxCommandChainLength", 0, Integer.MAX_VALUE, 65536));
    public static final GameRule<Integer> MAX_ENTITY_CRAMMING = register(new GameRule.Int("gamerule.minecraft.max_entity_cramming", "gamerule.maxEntityCramming", 0, Integer.MAX_VALUE, 24));
    public static final GameRule<Integer> MAX_MINECART_SPEED = register(new GameRule.Int("gamerule.minecraft.max_minecart_speed", "gamerule.minecartMaxSpeed", 1, 1000, 8));
    public static final GameRule<Integer> MAX_SNOW_ACCUMULATION_HEIGHT = register(new GameRule.Int("gamerule.minecraft.max_snow_accumulation_height", "gamerule.snowAccumulationHeight", 0, 8, 1));
    public static final GameRule<Boolean> MOB_DROPS = register(new GameRule.Bool("gamerule.minecraft.mob_drops", "gamerule.doMobLoot", true));
    public static final GameRule<Boolean> MOB_EXPLOSION_DROP_DECAY = register(new GameRule.Bool("gamerule.minecraft.mob_explosion_drop_decay", "gamerule.mobExplosionDropDecay", true));
    public static final GameRule<Boolean> MOB_GRIEFING = register(new GameRule.Bool("gamerule.minecraft.mob_griefing", "gamerule.mobGriefing", true));
    public static final GameRule<Boolean> NATURAL_HEALTH_REGENERATION = register(new GameRule.Bool("gamerule.minecraft.natural_health_regeneration", "gamerule.naturalRegeneration", true));
    public static final GameRule<Boolean> PLAYER_MOVEMENT_CHECK = register(new GameRule.Bool("gamerule.minecraft.player_movement_check", "gamerule.minecraft.player_movement_check", true));
    public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = register(new GameRule.Int("gamerule.minecraft.players_nether_portal_creative_delay", "gamerule.playersNetherPortalCreativeDelay", 0, Integer.MAX_VALUE, 0));
    public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = register(new GameRule.Int("gamerule.minecraft.players_nether_portal_default_delay", "gamerule.playersNetherPortalDefaultDelay", 0, Integer.MAX_VALUE, 80));
    public static final GameRule<Integer> PLAYERS_SLEEPING_PERCENTAGE = register(new GameRule.Int("gamerule.minecraft.players_sleeping_percentage", "gamerule.playersSleepingPercentage", 0, Integer.MAX_VALUE, 100));
    public static final GameRule<Boolean> PROJECTILES_CAN_BREAK_BLOCKS = register(new GameRule.Bool("gamerule.minecraft.projectiles_can_break_blocks", "gamerule.projectilesCanBreakBlocks", true));
    public static final GameRule<Boolean> PVP = register(new GameRule.Bool("gamerule.minecraft.pvp", "gamerule.pvp", true));
    public static final GameRule<Boolean> RAIDS = register(new GameRule.Bool("gamerule.minecraft.raids", "gamerule.minecraft.raids", true));
    public static final GameRule<Integer> RANDOM_TICK_SPEED = register(new GameRule.Int("gamerule.minecraft.random_tick_speed", "gamerule.randomTickSpeed", 0, Integer.MAX_VALUE, 3));
    public static final GameRule<Boolean> REDUCED_DEBUG_INFO = register(new GameRule.Bool("gamerule.minecraft.reduced_debug_info", "gamerule.reducedDebugInfo", false));
    public static final GameRule<Integer> RESPAWN_RADIUS = register(new GameRule.Int("gamerule.minecraft.respawn_radius", "gamerule.spawnRadius", 0, Integer.MAX_VALUE, 10));
    public static final GameRule<Boolean> SEND_COMMAND_FEEDBACK = register(new GameRule.Bool("gamerule.minecraft.send_command_feedback", "gamerule.sendCommandFeedback", true));
    public static final GameRule<Boolean> SHOW_ADVANCEMENT_MESSAGES = register(new GameRule.Bool("gamerule.minecraft.show_advancement_messages", "gamerule.announceAdvancements", true));
    public static final GameRule<Boolean> SHOW_DEATH_MESSAGES = register(new GameRule.Bool("gamerule.minecraft.show_death_messages", "gamerule.showDeathMessages", true));
    public static final GameRule<Boolean> SPAWNER_BLOCKS_WORK = register(new GameRule.Bool("gamerule.minecraft.spawner_blocks_work", "gamerule.spawnerBlocksEnabled", true));
    public static final GameRule<Boolean> SPAWN_MOBS = register(new GameRule.Bool("gamerule.minecraft.spawn_mobs", "gamerule.doMobSpawning", true));
    public static final GameRule<Boolean> SPAWN_MONSTERS = register(new GameRule.Bool("gamerule.minecraft.spawn_monsters", "gamerule.spawnMonsters", true));
    public static final GameRule<Boolean> SPAWN_PATROLS = register(new GameRule.Bool("gamerule.minecraft.spawn_patrols", "gamerule.doPatrolSpawning", true));
    public static final GameRule<Boolean> SPAWN_PHANTOMS = register(new GameRule.Bool("gamerule.minecraft.spawn_phantoms", "gamerule.doInsomnia", true));
    public static final GameRule<Boolean> SPAWN_WANDERING_TRADERS = register(new GameRule.Bool("gamerule.minecraft.spawn_wandering_traders", "gamerule.doTraderSpawning", true));
    public static final GameRule<Boolean> SPAWN_WARDENS = register(new GameRule.Bool("gamerule.minecraft.spawn_wardens", "gamerule.doWardenSpawning", true));
    public static final GameRule<Boolean> SPECTATORS_GENERATE_CHUNKS = register(new GameRule.Bool("gamerule.minecraft.spectators_generate_chunks", "gamerule.spectatorsGenerateChunks", true));
    public static final GameRule<Boolean> SPREAD_VINES = register(new GameRule.Bool("gamerule.minecraft.spread_vines", "gamerule.doVinesSpread", true));
    public static final GameRule<Boolean> TNT_EXPLODES = register(new GameRule.Bool("gamerule.minecraft.tnt_explodes", "gamerule.tntExplodes", true));
    public static final GameRule<Boolean> TNT_EXPLOSION_DROP_DECAY = register(new GameRule.Bool("gamerule.minecraft.tnt_explosion_drop_decay", "gamerule.tntExplosionDropDecay", false));
    public static final GameRule<Boolean> UNIVERSAL_ANGER = register(new GameRule.Bool("gamerule.minecraft.universal_anger", "gamerule.universalAnger", false));
    public static final GameRule<Boolean> WATER_SOURCE_CONVERSION = register(new GameRule.Bool("gamerule.minecraft.water_source_conversion", "gamerule.waterSourceConversion", true));

    public static <T> GameRule<T> register(GameRule<T> gameRule) {
        // TODO gamerule registry, by id pls
        //ALL_RULES.add(gameRule);
        return gameRule;
    }
}
