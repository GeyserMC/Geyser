/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
    ANNOUNCEADVANCEMENTS("announceAdvancements", Boolean.class, true), // JE only
    COMMANDBLOCKOUTPUT("commandBlockOutput", Boolean.class, true),
    DISABLEELYTRAMOVEMENTCHECK("disableElytraMovementCheck", Boolean.class, false), // JE only
    DISABLERAIDS("disableRaids", Boolean.class, false), // JE only
    DODAYLIGHTCYCLE("doDaylightCycle", Boolean.class, true),
    DOENTITYDROPS("doEntityDrops", Boolean.class, true),
    DOFIRETICK("doFireTick", Boolean.class, true),
    DOIMMEDIATERESPAWN("doImmediateRespawn", Boolean.class, false),
    DOINSOMNIA("doInsomnia", Boolean.class, true),
    DOLIMITEDCRAFTING("doLimitedCrafting", Boolean.class, false), // JE only
    DOMOBLOOT("doMobLoot", Boolean.class, true),
    DOMOBSPAWNING("doMobSpawning", Boolean.class, true),
    DOPATROLSPAWNING("doPatrolSpawning", Boolean.class, true), // JE only
    DOTILEDROPS("doTileDrops", Boolean.class, true),
    DOTRADERSPAWNING("doTraderSpawning", Boolean.class, true), // JE only
    DOWEATHERCYCLE("doWeatherCycle", Boolean.class, true),
    DROWNINGDAMAGE("drowningDamage", Boolean.class, true),
    FALLDAMAGE("fallDamage", Boolean.class, true),
    FIREDAMAGE("fireDamage", Boolean.class, true),
    FREEZEDAMAGE("freezeDamage", Boolean.class, true),
    FORGIVEDEADPLAYERS("forgiveDeadPlayers", Boolean.class, true), // JE only
    KEEPINVENTORY("keepInventory", Boolean.class, false),
    LOGADMINCOMMANDS("logAdminCommands", Boolean.class, true), // JE only
    MAXCOMMANDCHAINLENGTH("maxCommandChainLength", Integer.class, 65536),
    MAXENTITYCRAMMING("maxEntityCramming", Integer.class, 24), // JE only
    MOBGRIEFING("mobGriefing", Boolean.class, true),
    NATURALREGENERATION("naturalRegeneration", Boolean.class, true),
    PLAYERSSLEEPINGPERCENTAGE("playersSleepingPercentage", Integer.class, 100), // JE only
    RANDOMTICKSPEED("randomTickSpeed", Integer.class, 3),
    REDUCEDDEBUGINFO("reducedDebugInfo", Boolean.class, false), // JE only
    SENDCOMMANDFEEDBACK("sendCommandFeedback", Boolean.class, true),
    SHOWDEATHMESSAGES("showDeathMessages", Boolean.class, true),
    SPAWNRADIUS("spawnRadius", Integer.class, 10),
    SPECTATORSGENERATECHUNKS("spectatorsGenerateChunks", Boolean.class, true), // JE only
    UNIVERSALANGER("universalAnger", Boolean.class, false), // JE only

    UNKNOWN("unknown", Object.class);

    public static final GameRule[] VALUES = values();

    @Getter
    private final String javaID;

    @Getter
    private final Class<?> type;

    @Getter
    private final Object defaultValue;

    GameRule(String javaID, Class<?> type) {
        this(javaID, type, null);
    }

    GameRule(String javaID, Class<?> type, Object defaultValue) {
        this.javaID = javaID;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Convert a string to an object of the correct type for the current gamerule
     *
     * @param value The string value to convert
     * @return The converted and formatted value
     */
    public Object convertValue(String value) {
        if (type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (type.equals(Integer.class)) {
            return Integer.parseInt(value);
        }

        return null;
    }

    /**
     * Fetch a game rule by the given Java ID
     *
     * @param id The ID of the gamerule
     * @return A {@link GameRule} object representing the requested ID or {@link GameRule#UNKNOWN}
     */
    public static GameRule fromJavaID(String id) {
        for (GameRule gamerule : VALUES) {
            if (gamerule.javaID.equals(id)) {
                return gamerule;
            }
        }

        return UNKNOWN;
    }
}
