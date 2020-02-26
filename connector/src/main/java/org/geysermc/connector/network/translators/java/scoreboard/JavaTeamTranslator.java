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

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.scoreboard.Scoreboard;
import org.geysermc.connector.scoreboard.UpdateType;
import org.geysermc.connector.utils.MessageUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JavaTeamTranslator extends PacketTranslator<ServerTeamPacket> {

    @Override
    public void translate(ServerTeamPacket packet, GeyserSession session) {
        GeyserConnector.getInstance().getLogger().debug("Team packet " + packet.getTeamName() + " " + packet.getAction()+" "+ Arrays.toString(packet.getPlayers()));

        Scoreboard scoreboard = session.getScoreboardCache().getScoreboard();
        switch (packet.getAction()) {
            case CREATE:
                scoreboard.registerNewTeam(packet.getTeamName(), toPlayerSet(packet.getPlayers()))
                        .setName(MessageUtils.getBedrockMessage(packet.getDisplayName()))
                        .setPrefix(MessageUtils.getBedrockMessage(packet.getPrefix()))
                        .setSuffix(MessageUtils.getBedrockMessage(packet.getSuffix()));
                break;
            case UPDATE:
                scoreboard.getTeam(packet.getTeamName())
                        .setName(MessageUtils.getBedrockMessage(packet.getDisplayName()))
                        .setPrefix(MessageUtils.getBedrockMessage(packet.getPrefix()))
                        .setSuffix(MessageUtils.getBedrockMessage(packet.getSuffix()))
                        .setUpdateType(UpdateType.UPDATE);
                break;
            case ADD_PLAYER:
                scoreboard.getTeam(packet.getTeamName()).addEntities(packet.getPlayers());
                break;
            case REMOVE_PLAYER:
                scoreboard.getTeam(packet.getTeamName()).removeEntities(packet.getPlayers());
                break;
            case REMOVE:
               scoreboard.removeTeam(packet.getTeamName());
               break;
        }
        scoreboard.onUpdate();
    }

    private Set<String> toPlayerSet(String[] players) {
        return new HashSet<>(Arrays.asList(players));
    }
}
