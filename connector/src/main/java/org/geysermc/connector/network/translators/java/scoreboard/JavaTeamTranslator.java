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

package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.scoreboard.Scoreboard;
import org.geysermc.connector.scoreboard.ScoreboardUpdater;
import org.geysermc.connector.scoreboard.Team;
import org.geysermc.connector.scoreboard.UpdateType;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.Arrays;
import java.util.Set;

@Translator(packet = ServerTeamPacket.class)
public class JavaTeamTranslator extends PacketTranslator<ServerTeamPacket> {
    private static final GeyserLogger LOGGER = GeyserConnector.getInstance().getLogger();

    @Override
    public void translate(ServerTeamPacket packet, GeyserSession session) {
        if (LOGGER.isDebug()) {
            LOGGER.debug("Team packet " + packet.getTeamName() + " " + packet.getAction() + " " + Arrays.toString(packet.getPlayers()));
        }

        int pps = session.getWorldCache().increaseAndGetScoreboardPacketsPerSecond();

        Scoreboard scoreboard = session.getWorldCache().getScoreboard();
        Team team = null;

        if (packet.getAction() != TeamAction.CREATE) {
            team = scoreboard.getTeam(packet.getTeamName());
        }
        switch (packet.getAction()) {
            case CREATE:
                scoreboard.registerNewTeam(packet.getTeamName(), toPlayerSet(packet.getPlayers()))
                        .setName(MessageTranslator.convertMessage(packet.getDisplayName()))
                        .setColor(packet.getColor())
                        .setNameTagVisibility(packet.getNameTagVisibility())
                        .setPrefix(MessageTranslator.convertMessage(packet.getPrefix(), session.getLocale()))
                        .setSuffix(MessageTranslator.convertMessage(packet.getSuffix(), session.getLocale()));
                updatePlayerNameTags(session, team, packet.getPlayers(), false);
                break;
            case UPDATE:
                if (team == null) {
                    LOGGER.debug(LanguageUtils.getLocaleStringLog(
                            "geyser.network.translator.team.failed_not_registered",
                            packet.getAction(), packet.getTeamName()
                    ));
                    return;
                }
                NameTagVisibility oldVisibility = team.getNameTagVisibility();

                team.setName(MessageTranslator.convertMessage(packet.getDisplayName()))
                        .setColor(packet.getColor())
                        .setNameTagVisibility(packet.getNameTagVisibility())
                        .setPrefix(MessageTranslator.convertMessage(packet.getPrefix(), session.getLocale()))
                        .setSuffix(MessageTranslator.convertMessage(packet.getSuffix(), session.getLocale()))
                        .setUpdateType(UpdateType.UPDATE);
                if (!oldVisibility.equals(packet.getNameTagVisibility())) {
                    updatePlayerNameTags(session, team, team.getEntities(), false);
                }
                break;
            case ADD_PLAYER:
                if (team == null) {
                    LOGGER.debug(LanguageUtils.getLocaleStringLog(
                            "geyser.network.translator.team.failed_not_registered",
                            packet.getAction(), packet.getTeamName()
                    ));
                    return;
                }
                team.addEntities(packet.getPlayers());
                updatePlayerNameTags(session, team, packet.getPlayers(), false);
                break;
            case REMOVE_PLAYER:
                if (team == null) {
                    LOGGER.debug(LanguageUtils.getLocaleStringLog(
                            "geyser.network.translator.team.failed_not_registered",
                            packet.getAction(), packet.getTeamName()
                    ));
                    return;
                }
                team.removeEntities(packet.getPlayers());
                updatePlayerNameTags(session, team, packet.getPlayers(), true);
                break;
            case REMOVE:
                scoreboard.removeTeam(packet.getTeamName());
                if (team != null) {
                    updatePlayerNameTags(session, team, team.getEntities(), true);
                }
                break;
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher then the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }

    private void updatePlayerNameTags(GeyserSession session, Team team, String[] teamEntities, boolean remove) {
        for (PlayerEntity entity : session.getEntityCache().getPlayerEntities()) {
            for (String name : teamEntities) {
                if (entity.getUsername().equals(name)) {
                    String currentName = entity.getMetadata().getString(EntityData.NAMETAG);
                    String newName;
                    if (remove) {
                        // Set the nametag to their default
                        newName = entity.getUsername();
                    } else {
                        newName = team.getDisplayNameFor(currentName, session.getPlayerEntity().getUsername());
                    }
                    if (!newName.equals(currentName)) {
                        entity.getMetadata().put(EntityData.NAMETAG, newName);
                        SetEntityDataPacket packet = new SetEntityDataPacket();
                        packet.getMetadata().put(EntityData.NAMETAG, newName);
                        packet.setRuntimeEntityId(entity.getGeyserId());
                        session.sendUpstreamPacket(packet);
                    }
                    break;
                }
            }
        }
    }

    private Set<String> toPlayerSet(String[] players) {
        return new ObjectOpenHashSet<>(players);
    }
}
