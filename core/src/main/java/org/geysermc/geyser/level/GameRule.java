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
public enum GameRule {
    ANNOUNCEADVANCEMENTS("announceAdvancements", true), // JE only
    COMMANDBLOCKOUTPUT("commandBlockOutput", true),
    DISABLEELYTRAMOVEMENTCHECK("disableElytraMovementCheck", false), // JE only
    DISABLERAIDS("disableRaids", false), // JE only
    DODAYLIGHTCYCLE("doDaylightCycle", true),
    DOENTITYDROPS("doEntityDrops", true),
    DOFIRETICK("doFireTick", true),
    DOIMMEDIATERESPAWN("doImmediateRespawn", false),
    DOINSOMNIA("doInsomnia", true),
    DOLIMITEDCRAFTING("doLimitedCrafting", false), // JE only
    DOMOBLOOT("doMobLoot", true),
    DOMOBSPAWNING("doMobSpawning", true),
    DOPATROLSPAWNING("doPatrolSpawning", true), // JE only
    DOTILEDROPS("doTileDrops", true),
    DOTRADERSPAWNING("doTraderSpawning", true), // JE only
    DOWEATHERCYCLE("doWeatherCycle", true),
    DROWNINGDAMAGE("drowningDamage", true),
    FALLDAMAGE("fallDamage", true),
    FIREDAMAGE("fireDamage", true),
    FREEZEDAMAGE("freezeDamage", true),
    FORGIVEDEADPLAYERS("forgiveDeadPlayers", true), // JE only
    KEEPINVENTORY("keepInventory", false),
    LOGADMINCOMMANDS("logAdminCommands", true), // JE only
    MAXCOMMANDCHAINLENGTH("maxCommandChainLength", 65536),
    MAXENTITYCRAMMING("maxEntityCramming", 24), // JE only
    MOBGRIEFING("mobGriefing", true),
    NATURALREGENERATION("naturalRegeneration", true),
    PLAYERSSLEEPINGPERCENTAGE("playersSleepingPercentage", 100), // JE only
    RANDOMTICKSPEED("randomTickSpeed", 3),
    REDUCEDDEBUGINFO("reducedDebugInfo", false), // JE only
    SENDCOMMANDFEEDBACK("sendCommandFeedback", true),
    SHOWDEATHMESSAGES("showDeathMessages", true),
    SPAWNRADIUS("spawnRadius", 10),
    SPECTATORSGENERATECHUNKS("spectatorsGenerateChunks", true), // JE only
    UNIVERSALANGER("universalAnger", false); // JE only

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
