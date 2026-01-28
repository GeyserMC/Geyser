/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level;

import lombok.Getter;

import java.util.Locale;

/**
 * This enum stores each gamerule along with the value type and the default.
 * It is used to construct the list for the settings menu
 */
// TODO gamerules with feature flags (e.g. minecart speed with minecart experiment)
public enum GameRule {
    ADVANCE_TIME("gamerule.doDaylightCycle", true),
    ADVANCE_WEATHER("gamerule.doWeatherCycle", true),
    ALLOW_ENTERING_NETHER_USING_PORTALS("gamerule.allowEnteringNetherUsingPortals", true),
    BLOCK_DROPS("gamerule.doTileDrops", true),
    BLOCK_EXPLOSION_DROP_DECAY("gamerule.blockExplosionDropDecay", true),
    COMMAND_BLOCKS_WORK("gamerule.commandBlocksEnabled", true),
    COMMAND_BLOCK_OUTPUT("gamerule.commandBlockOutput", true),
    DROWNING_DAMAGE("gamerule.drowningDamage", true),
    ELYTRA_MOVEMENT_CHECK("gamerule.minecraft.elytra_movement_check", true),
    ENDER_PEARLS_VANISH_ON_DEATH("gamerule.enderPearlsVanishOnDeath", true),
    ENTITY_DROPS("gamerule.doEntityDrops", true),
    FALL_DAMAGE("gamerule.fallDamage", true),
    FIRE_DAMAGE("gamerule.fireDamage", true),
    FIRE_SPREAD_RADIUS_AROUND_PLAYER("gamerule.minecraft.fire_spread_radius_around_player", 128),
    FORGIVE_DEAD_PLAYERS("gamerule.forgiveDeadPlayers", true),
    FREEZE_DAMAGE("gamerule.freezeDamage", true),
    GLOBAL_SOUND_EVENTS("gamerule.globalSoundEvents", true),
    IMMEDIATE_RESPAWN("gamerule.doImmediateRespawn", false),
    KEEP_INVENTORY("gamerule.keepInventory", false),
    LAVA_SOURCE_CONVERSION("gamerule.lavaSourceConversion", false),
    LIMITED_CRAFTING("gamerule.doLimitedCrafting", false),
    LOCATOR_BAR("gamerule.locatorBar", true),
    LOG_ADMIN_COMMANDS("gamerule.logAdminCommands", true),
    MAX_BLOCK_MODIFICATIONS("gamerule.commandModificationBlockLimit", 32768),
    MAX_COMMAND_FORKS("gamerule.maxCommandForkCount", 65536),
    MAX_COMMAND_SEQUENCE_LENGTH("gamerule.maxCommandChainLength", 65536),
    MAX_ENTITY_CRAMMING("gamerule.maxEntityCramming", 24),
    MAX_SNOW_ACCUMULATION_HEIGHT("gamerule.snowAccumulationHeight", 1),
    MOB_DROPS("gamerule.doMobLoot", true),
    MOB_EXPLOSION_DROP_DECAY("gamerule.mobExplosionDropDecay", true),
    MOB_GRIEFING("gamerule.mobGriefing", true),
    NATURAL_HEALTH_REGENERATION("gamerule.naturalRegeneration", true),
    PLAYER_MOVEMENT_CHECK("gamerule.minecraft.player_movement_check", true),
    PLAYERS_NETHER_PORTAL_CREATIVE_DELAY("gamerule.playersNetherPortalCreativeDelay", 0),
    PLAYERS_NETHER_PORTAL_DEFAULT_DELAY("gamerule.playersNetherPortalDefaultDelay", 80),
    PLAYERS_SLEEPING_PERCENTAGE("gamerule.playersSleepingPercentage", 100),
    PROJECTILES_CAN_BREAK_BLOCKS("gamerule.projectilesCanBreakBlocks", true),
    PVP("gamerule.pvp", true),
    RAIDS("gamerule.minecraft.raids", true),
    RANDOM_TICK_SPEED("gamerule.randomTickSpeed", 3),
    REDUCED_DEBUG_INFO("gamerule.reducedDebugInfo", false),
    RESPAWN_RADIUS("gamerule.spawnRadius", 10),
    SEND_COMMAND_FEEDBACK("gamerule.sendCommandFeedback", true),
    SHOW_ADVANCEMENT_MESSAGES("gamerule.announceAdvancements", true),
    SHOW_DEATH_MESSAGES("gamerule.showDeathMessages", true),
    SPAWNER_BLOCKS_WORK("gamerule.spawnerBlocksEnabled", true),
    SPAWN_MOBS("gamerule.doMobSpawning", true),
    SPAWN_MONSTERS("gamerule.spawnMonsters", true),
    SPAWN_PATROLS("gamerule.doPatrolSpawning", true),
    SPAWN_PHANTOMS("gamerule.doInsomnia", true),
    SPAWN_WANDERING_TRADERS("gamerule.doTraderSpawning", true),
    SPAWN_WARDENS("gamerule.doWardenSpawning", true),
    SPECTATORS_GENERATE_CHUNKS("gamerule.spectatorsGenerateChunks", true),
    SPREAD_VINES("gamerule.doVinesSpread", true),
    TNT_EXPLODES("gamerule.tntExplodes", true),
    TNT_EXPLOSION_DROP_DECAY("gamerule.tntExplosionDropDecay", false),
    UNIVERSAL_ANGER("gamerule.universalAnger", false),
    WATER_SOURCE_CONVERSION("gamerule.waterSourceConversion", true);

    public static final GameRule[] VALUES = values();

    @Getter
    private final String translation;

    @Getter
    private final Class<?> type;

    private final int defaultValue;

    GameRule(String translation, boolean defaultValue) {
        this.translation = translation;
        this.type = Boolean.class;
        this.defaultValue = defaultValue ? 1 : 0;
    }

    GameRule(String translation, int defaultValue) {
        this.translation = translation;
        this.type = Integer.class;
        this.defaultValue = defaultValue;
    }

    public boolean getDefaultBooleanValue() {
        return defaultValue != 0;
    }

    public int getDefaultIntValue() {
        return defaultValue;
    }

    public String getJavaID() {
        return name().toLowerCase(Locale.ROOT);
    }
}
