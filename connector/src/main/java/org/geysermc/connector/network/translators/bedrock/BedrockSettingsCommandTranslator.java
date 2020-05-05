/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.nukkitx.protocol.bedrock.packet.SettingsCommandPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.WorldCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = SettingsCommandPacket.class)
public class BedrockSettingsCommandTranslator extends PacketTranslator<SettingsCommandPacket> {
    private static final Object2ObjectMap<String, String> gameRuleMap = new Object2ObjectOpenHashMap<>();

    static {
        // The new Java Edition command system is complete trash, and case matters; ugh
        gameRuleMap.put("commandblockoutput", "commandBlockOutput");
        gameRuleMap.put("commandblocksenabled", "commandBlocksEnabled");
        gameRuleMap.put("dodaylightcycle", "doDaylightCycle");
        gameRuleMap.put("doentitydrops", "doEntityDrops");
        gameRuleMap.put("dofiretick", "doFireTick");
        gameRuleMap.put("doimmediaterespawn", "doImmediateRespawn");
        gameRuleMap.put("doinsomnia", "doInsomnia");
        gameRuleMap.put("domobloot", "doMobLoot");
        gameRuleMap.put("domobspawning", "doMobSpawning");
        gameRuleMap.put("dotiledrops", "doTileDrops");
        gameRuleMap.put("doweathercycle", "doWeatherCycle");
        gameRuleMap.put("drowningdamage", "drowningDamage");
        gameRuleMap.put("falldamage", "fallDamage");
        gameRuleMap.put("firedamage", "fireDamage");
        // functionCommandLimit
        gameRuleMap.put("keepinventory", "keepInventory");
        gameRuleMap.put("maxcommandchainlength", "maxCommandChainLength");
        gameRuleMap.put("mobgriefing", "mobGriefing");
        gameRuleMap.put("naturalregeneration", "naturalRegeneration");
        gameRuleMap.put("pvp", "pvp");
        gameRuleMap.put("randomtickspeed", "randomTickSpeed");
        gameRuleMap.put("sendcommandfeedback", "sendCommandFeedback");
        // showCoordinates handled seperately
        gameRuleMap.put("showdeathmessages", "showDeathMessages");
        // showTags
        gameRuleMap.put("spawnradius", "spawnRadius");
        // tntExplodes
    }

    @Override
    public void translate(SettingsCommandPacket packet, GeyserSession session) {
        if(!packet.getCommand().startsWith("/gamerule")) {
            return; // We only handle gamerules for now
        }

        String[] args = packet.getCommand().replaceAll("/gamerule ", "").split(" ");

        // Coordinates are a special case as they don't exist as a game rule in java edition
        if(args[0].equalsIgnoreCase("showcoordinates") && !session.isReducedDebugInfo()) {
            session.getWorldCache().setShowCoordinates(Boolean.parseBoolean(args[1]));
            return;
        }

        if(!gameRuleMap.containsKey(args[0])) {
            return;
        }

        session.getConnector().getWorldManager().setGameRule(session, gameRuleMap.get(args[0]), args[1]);

        // TODO: check if the game rule was changed successfully, but this is needed for now
        session.sendGameRule(args[0], args[1]);
    }
}
