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

/**
 * This enum stores each gamerule along with the value type and the default.
 * It is used to construct the list for the settings menu
 */
// TODO gamerules with feature flags (e.g. minecart speed with minecart experiment)
public enum GameRule {
    COMMANDBLOCKOUTPUT("command_block_output", true),
    LOGADMINCOMMANDS("log_admin_commands", true), // JE only
    SENDCOMMANDFEEDBACK("send_command_feedback", true),
    SHOWADVANCEMENTMESSAGES("show_advancement_messages", true), // JE only
    SHOWDEATHMESSAGES("show_death_messages", true),
    BLOCKDROPS("block_drops", true),
    BLOCKEXPLOSIONDROPDECAY("block_explosion_drop_decay", true), // JE only
    ENTITYDROPS("entity_drops", true),
    MOBDROPS("mob_drops", true),
    MOBEXPLOSIONDROPDECAY("mob_explosion_drop_decay", true), // JE only
    PROJECTILESCANBREAKBLOCKS("projectiles_can_break_blocks", true),
    TNTEXPLOSIONDROPDECAY("tnt_explosion_drop_decay", false),
    COMMANDBLOCKSWORK("command_blocks_work", true),
    GLOBALSOUNDEVENTS("global_sound_events", true), // JE only
    MAXBLOCKMODIFICATIONS("max_block_modifications", 32768), // JE only
    MAXCOMMANDFORKS("max_command_forks", 65536), // JE only
    MAXCOMMANDSEQUENCELENGTH("max_command_sequence_length", 65536),
    REDUCEDDEBUGINFO("reduced_debug_info", false), // JE only
    TNTEXPLODES("tnt_explodes", true),
    FORGIVEDEADPLAYERS("forgive_dead_players", true), // JE only
    MAXENTITYCRAMMING("max_entity_cramming", 24), // JE only
    MOBGRIEFING("mob_griefing", true),
    RAIDS("raids", true), // JE only
    UNIVERSALANGER("universal_anger", false), // JE only
    ALLOWENTERINGNETHERUSINGPORTALS("allow_entering_nether_using_portals", true), // JE only
    DROWNINGDAMAGE("drowning_damage", true),
    ELYTRAMOVEMENTCHECK("elytra_movement_check", true), // JE only
    ENDERPEARLSVANISHONDEATH("ender_pearls_vanish_on_death", true), // JE only
    FALLDAMAGE("fall_damage", true),
    FIREDAMAGE("fire_damage", true),
    FIRESPREADRADIUSAROUNDPLAYER("fire_spread_radius_around_player", 128), // JE only
    FREEZEDAMAGE("freeze_damage", true),
    IMMEDIATERESPAWN("immediate_respawn", false),
    KEEPINVENTORY("keep_inventory", false),
    LIMITEDCRAFTING("limited_crafting", false),
    LOCATORBAR("locator_bar", true),
    NATURALHEALTHREGENERATION("natural_health_regeneration", true),
    PLAYERMOVEMENTCHECK("player_movement_check", true), // JE only
    PLAYERSNETHERPORTALCREATIVEDELAY("players_nether_portal_creative_delay", 0), // JE only
    PLAYERSNETHERPORTALDEFAULTDELAY("players_nether_portal_default_delay", 80), // JE only
    PLAYERSSLEEPINGPERCENTAGE("players_sleeping_percentage", 100),
    PVP("pvp", true),
    RESPAWNRADIUS("respawn_radius", 10),
    SPAWNMONSTERS("spawn_monsters", true), // JE only
    SPECTATORSGENERATECHUNKS("spectators_generate_chunks", true), // JE only
    SPAWNMOBS("spawn_mobs", true),
    SPAWNPATROLS("spawn_patrols", true), // JE only
    SPAWNPHANTOMS("spawn_phantoms", true),
    SPAWNWANDERINGTRADERS("spawn_wandering_traders", true), // JE only
    SPAWNWARDENS("spawn_wardens", true), // JE only
    SPAWNERBLOCKSWORK("spawner_blocks_work", true), // JE only
    ADVANCETIME("advance_time", true),
    ADVANCEWEATHER("advance_weather", true),
    LAVASOURCECONVERSION("lava_source_conversion", false), // JE only
    MAXSNOWACCUMULATIONHEIGHT("max_snow_accumulation_height", 1), // JE only
    RANDOMTICKSPEED("random_tick_speed", 3),
    SPREADVINES("spread_vines", true), // JE only
    WATERSOURCECONVERSION("water_source_conversion", true); // JE only

    public static final GameRule[] VALUES = values();

    @Getter
    private final String javaID;

    @Getter
    private final Class<?> type;

    private final int defaultValue;

    GameRule(String javaID, boolean defaultValue) {
        this.javaID = javaID;
        this.type = Boolean.class;
        this.defaultValue = defaultValue ? 1 : 0;
    }

    GameRule(String javaID, int defaultValue) {
        this.javaID = javaID;
        this.type = Integer.class;
        this.defaultValue = defaultValue;
    }

    public boolean getDefaultBooleanValue() {
        return defaultValue != 0;
    }

    public int getDefaultIntValue() {
        return defaultValue;
    }
}
