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

package org.geysermc.geyser.translator.protocol.java.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreboardPosition;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.WorldCache;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundSetScorePacket.class)
public class JavaSetScoreTranslator extends PacketTranslator<ClientboundSetScorePacket> {
    private static final boolean SHOW_SCOREBOARD_LOGS = Boolean.parseBoolean(System.getProperty("Geyser.ShowScoreboardLogs", "true"));

    private final GeyserLogger logger;

    public JavaSetScoreTranslator() {
        logger = GeyserImpl.getInstance().getLogger();
    }

    @Override
    public void translate(GeyserSession session, ClientboundSetScorePacket packet) {
        // todo 1.20.3 unused display and number format?
        WorldCache worldCache = session.getWorldCache();
        Scoreboard scoreboard = worldCache.getScoreboard();
        int pps = worldCache.increaseAndGetScoreboardPacketsPerSecond();

        Objective objective = scoreboard.getObjective(packet.getObjective());
        if (objective == null) {
            if (SHOW_SCOREBOARD_LOGS) {
                logger.info(GeyserLocale.getLocaleStringLog("geyser.network.translator.score.failed_objective", packet.getObjective()));
            }
            return;
        }

        // If this is the objective that is in use to show the below name text, we need to update the player
        // attached to this score.
        boolean isBelowName = objective == scoreboard.getObjectiveSlots().get(ScoreboardPosition.BELOW_NAME);

        objective.setScore(packet.getOwner(), packet.getValue());
        if (isBelowName) {
            // Update the below name score on this player
            setBelowName(session, objective, packet.getOwner(), packet.getValue());
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher than the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }

    /**
     * @param objective the objective that currently resides on the below name display slot
     */
    static void setBelowName(GeyserSession session, Objective objective, String username, int count) {
        PlayerEntity entity = getOtherPlayerEntity(session, username);
        if (entity == null) {
            return;
        }

        String displayString = count + " " + objective.getDisplayName();

        // Of note: unlike Bedrock, if there is an objective in the below name slot, everyone has a display
        entity.getDirtyMetadata().put(EntityDataTypes.SCORE, displayString);
        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
        entityDataPacket.getMetadata().put(EntityDataTypes.SCORE, displayString);
        session.sendUpstreamPacket(entityDataPacket);
    }

    private static @Nullable PlayerEntity getOtherPlayerEntity(GeyserSession session, String username) {
        // We don't care about the session player, because... they're not going to be seeing their own score
        if (session.getPlayerEntity().getUsername().equals(username)) {
            return null;
        }

        for (PlayerEntity entity : session.getEntityCache().getAllPlayerEntities()) {
            if (entity.getUsername().equals(username)) {
                if (entity.isValid()) {
                    return entity;
                } else {
                    // The below name text will be applied on spawn
                    return null;
                }
            }
        }

        return null;
    }
}
