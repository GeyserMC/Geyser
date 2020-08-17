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

package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.scoreboard.Scoreboard;
import org.geysermc.connector.scoreboard.Team;
import org.geysermc.connector.scoreboard.UpdateType;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.MessageUtils;

import java.util.Arrays;
import java.util.Set;

@Translator(packet = ServerTeamPacket.class)
public class JavaTeamTranslator extends PacketTranslator<ServerTeamPacket> {

    @Override
    public void translate(ServerTeamPacket packet, GeyserSession session) {
        GeyserConnector.getInstance().getLogger().debug("Team packet " + packet.getTeamName() + " " + packet.getAction() + " " + Arrays.toString(packet.getPlayers()));

        Scoreboard scoreboard = session.getWorldCache().getScoreboard();
        Team team = scoreboard.getTeam(packet.getTeamName());
        switch (packet.getAction()) {
            case CREATE:
                scoreboard.registerNewTeam(packet.getTeamName(), toPlayerSet(packet.getPlayers()))
                        .setName(MessageUtils.getBedrockMessage(packet.getDisplayName()))
                        .setColor(packet.getColor())
                        .setPrefix(MessageUtils.getTranslatedBedrockMessage(packet.getPrefix(), session.getClientData().getLanguageCode()))
                        .setSuffix(MessageUtils.getTranslatedBedrockMessage(packet.getSuffix(), session.getClientData().getLanguageCode()));
                break;
            case UPDATE:
                if (team != null) {
                    team.setName(MessageUtils.getBedrockMessage(packet.getDisplayName()))
                            .setColor(packet.getColor())
                            .setPrefix(MessageUtils.getTranslatedBedrockMessage(packet.getPrefix(), session.getClientData().getLanguageCode()))
                            .setSuffix(MessageUtils.getTranslatedBedrockMessage(packet.getSuffix(), session.getClientData().getLanguageCode()))
                            .setUpdateType(UpdateType.UPDATE);
                } else {
                    GeyserConnector.getInstance().getLogger().debug(LanguageUtils.getLocaleStringLog("geyser.network.translator.team.failed_not_registered", packet.getAction(), packet.getTeamName()));
                }
                break;
            case ADD_PLAYER:
                if (team != null) {
                    team.addEntities(packet.getPlayers());
                } else {
                    GeyserConnector.getInstance().getLogger().debug(LanguageUtils.getLocaleStringLog("geyser.network.translator.team.failed_not_registered", packet.getAction(), packet.getTeamName()));
                }
                break;
            case REMOVE_PLAYER:
                if (team != null) {
                    team.removeEntities(packet.getPlayers());
                } else {
                    GeyserConnector.getInstance().getLogger().debug(LanguageUtils.getLocaleStringLog("geyser.network.translator.team.failed_not_registered", packet.getAction(), packet.getTeamName()));
                }
                break;
            case REMOVE:
                scoreboard.removeTeam(packet.getTeamName());
                break;
        }
        scoreboard.onUpdate();
    }

    private Set<String> toPlayerSet(String[] players) {
        return new ObjectOpenHashSet<>(Arrays.asList(players));
    }
}
