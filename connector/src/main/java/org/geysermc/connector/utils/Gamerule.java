/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public enum Gamerule {
    ANNOUNCEADVANCEMENTS("announceAdvancements", Boolean.class), // JE only
    COMMANDBLOCKOUTPUT("commandBlockOutput", Boolean.class),
    DISABLEELYTRAMOVEMENTCHECK("disableElytraMovementCheck", Boolean.class), // JE only
    DISABLERAIDS("disableRaids", Boolean.class), // JE only
    DODAYLIGHTCYCLE("doDaylightCycle", Boolean.class),
    DOENTITYDROPS("doEntityDrops", Boolean.class),
    DOFIRETICK("doFireTick", Boolean.class),
    DOIMMEDIATERESPAWN("doImmediateRespawn", Boolean.class),
    DOINSOMNIA("doInsomnia", Boolean.class),
    DOLIMITEDCRAFTING("doLimitedCrafting", Boolean.class), // JE only
    DOMOBLOOT("doMobLoot", Boolean.class),
    DOMOBSPAWNING("doMobSpawning", Boolean.class),
    DOPATROLSPAWNING("doPatrolSpawning", Boolean.class), // JE only
    DOTILEDROPS("doTileDrops", Boolean.class),
    DOTRADERSPAWNING("doTraderSpawning", Boolean.class), // JE only
    DOWEATHERCYCLE("doWeatherCycle", Boolean.class),
    DROWNINGDAMAGE("drowningDamage", Boolean.class),
    FALLDAMAGE("fallDamage", Boolean.class),
    FIREDAMAGE("fireDamage", Boolean.class),
    FORGIVEDEADPLAYERS("forgiveDeadPlayers", Boolean.class), // JE only
    KEEPINVENTORY("keepInventory", Boolean.class),
    LOGADMINCOMMANDS("logAdminCommands", Boolean.class), // JE only
    MAXCOMMANDCHAINLENGTH("maxCommandChainLength", Integer.class),
    MAXENTITYCRAMMING("maxEntityCramming", Integer.class), // JE only
    MOBGRIEFING("mobGriefing", Boolean.class),
    NATURALREGENERATION("naturalRegeneration", Boolean.class),
    RANDOMTICKSPEED("randomTickSpeed", Integer.class),
    REDUCEDDEBUGINFO("reducedDebugInfo", Boolean.class), // JE only
    SENDCOMMANDFEEDBACK("sendCommandFeedback", Boolean.class),
    SHOWCOORDINATES("showCoordinates", Boolean.class), // Handled separately
    SHOWDEATHMESSAGES("showDeathMessages", Boolean.class),
    SHOWTAGS("showTags", Boolean.class), // JE only
    SPAWNRADIUS("spawnRadius", Boolean.class),
    SPECTATORSGENERATECHUNKS("spectatorsGenerateChunks", Boolean.class), // JE only
    UNIVERSALANGER("universalAnger", Boolean.class), // JE only

    UNKNOWN("unknown", Boolean.class);

    private static final Gamerule[] VALUES = values();

    @Getter
    private String javaID;

    @Getter
    private Class<?> type;

    Gamerule(String javaID, Class<?> type) {
        this.javaID = javaID;
        this.type = type;
    }

    public String getBedrockID() {
        return this.name().toLowerCase();
    }

    public Object convertValue(String value) {
        if (type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (type.equals(Integer.class)) {
            return Integer.parseInt(value);
        }

        return null;
    }

    public static Gamerule fromJavaID(String id) {
        for (Gamerule gamerule : VALUES) {
            if (gamerule.javaID.equals(id)) {
                return gamerule;
            }
        }

        return UNKNOWN;
    }

    public static Gamerule fromBedrockID(String id) {
        for (Gamerule gamerule : VALUES) {
            if (gamerule.getBedrockID().equals(id)) {
                return gamerule;
            }
        }

        return UNKNOWN;
    }
}
