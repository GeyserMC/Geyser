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

package org.geysermc.geyser.translator.protocol.java.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamAction;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.scoreboard.UpdateType;

import java.util.Arrays;
import java.util.Set;

@Translator(packet = ClientboundSetPlayerTeamPacket.class)
public class JavaSetPlayerTeamTranslator extends PacketTranslator<ClientboundSetPlayerTeamPacket> {
    private final GeyserLogger logger = GeyserImpl.getInstance().getLogger();

    @Override
    public void translate(GeyserSession session, ClientboundSetPlayerTeamPacket packet) {
        if (logger.isDebug()) {
            logger.debug("Team packet " + packet.getTeamName() + " " + packet.getAction() + " " + Arrays.toString(packet.getPlayers()));
        }

        if ((packet.getAction() == TeamAction.ADD_PLAYER || packet.getAction() == TeamAction.REMOVE_PLAYER) && packet.getPlayers().length == 0) {
            return;
        }

        int pps = session.getWorldCache().increaseAndGetScoreboardPacketsPerSecond();

        Scoreboard scoreboard = session.getWorldCache().getScoreboard();
        Team team = scoreboard.getTeam(packet.getTeamName());
        switch (packet.getAction()) {
            case CREATE -> {
                team = scoreboard.registerNewTeam(packet.getTeamName(), packet.getPlayers())
                        .setName(MessageTranslator.convertMessage(packet.getDisplayName()))
                        .setColor(packet.getColor())
                        .setNameTagVisibility(packet.getNameTagVisibility())
                        .setPrefix(MessageTranslator.convertMessage(packet.getPrefix(), session.getLocale()))
                        .setSuffix(MessageTranslator.convertMessage(packet.getSuffix(), session.getLocale()));

                if (packet.getPlayers().length != 0) {
                    if ((team.getNameTagVisibility() != NameTagVisibility.ALWAYS && !team.isVisibleFor(session.getPlayerEntity().getUsername()))
                            || team.getColor() != TeamColor.NONE
                            || !team.getCurrentData().getPrefix().isEmpty()
                            || !team.getCurrentData().getSuffix().isEmpty()) {
                        // Something is here that would modify entity names
                        scoreboard.updateEntityNames(team, true);
                    }
                }
            }
            case UPDATE -> {
                if (team == null) {
                    if (logger.isDebug()) {
                        logger.debug("Error while translating Team Packet " + packet.getAction()
                                + "! Scoreboard Team " + packet.getTeamName() + " is not registered."
                        );
                    }
                    return;
                }

                TeamColor oldColor = team.getColor();
                NameTagVisibility oldVisibility = team.getNameTagVisibility();
                String oldPrefix = team.getCurrentData().getPrefix();
                String oldSuffix = team.getCurrentData().getSuffix();

                team.setName(MessageTranslator.convertMessage(packet.getDisplayName()))
                        .setColor(packet.getColor())
                        .setNameTagVisibility(packet.getNameTagVisibility())
                        .setPrefix(MessageTranslator.convertMessage(packet.getPrefix(), session.getLocale()))
                        .setSuffix(MessageTranslator.convertMessage(packet.getSuffix(), session.getLocale()))
                        .setUpdateType(UpdateType.UPDATE);

                if (oldVisibility != team.getNameTagVisibility()
                        || oldColor != team.getColor()
                        || !oldPrefix.equals(team.getCurrentData().getPrefix())
                        || !oldSuffix.equals(team.getCurrentData().getSuffix())) {
                    // Update entities attached to this team as something about their nameplates have changed
                    scoreboard.updateEntityNames(team, false);
                }
            }
            case ADD_PLAYER -> {
                if (team == null) {
                    if (logger.isDebug()) {
                        logger.debug("Error while translating Team Packet " + packet.getAction()
                                + "! Scoreboard Team " + packet.getTeamName() + " is not registered."
                        );
                    }
                    return;
                }
                Set<String> added = team.addEntities(packet.getPlayers());
                scoreboard.updateEntityNames(team, added, true);
            }
            case REMOVE_PLAYER -> {
                if (team == null) {
                    if (logger.isDebug()) {
                        logger.debug("Error while translating Team Packet " + packet.getAction()
                                + "! Scoreboard Team " + packet.getTeamName() + " is not registered."
                        );
                    }
                    return;
                }
                Set<String> removed = team.removeEntities(packet.getPlayers());
                scoreboard.updateEntityNames(null, removed, true);
            }
            case REMOVE -> scoreboard.removeTeam(packet.getTeamName());
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher than the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}
