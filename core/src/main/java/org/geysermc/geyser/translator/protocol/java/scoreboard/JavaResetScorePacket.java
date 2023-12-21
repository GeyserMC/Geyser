/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundResetScorePacket;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.WorldCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundResetScorePacket.class)
public class JavaResetScorePacket extends PacketTranslator<ClientboundResetScorePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundResetScorePacket packet) {
        WorldCache worldCache = session.getWorldCache();
        Scoreboard scoreboard = worldCache.getScoreboard();
        int pps = worldCache.increaseAndGetScoreboardPacketsPerSecond();

        Objective belowName = scoreboard.getObjectiveSlots().get(ScoreboardPosition.BELOW_NAME);

        if (packet.getObjective() == null) {
            // No objective name means all scores are reset for that player (/scoreboard players reset PLAYERNAME)
            for (Objective otherObjective : scoreboard.getObjectives()) {
                otherObjective.removeScore(packet.getOwner());
            }

            // as described below
            if (belowName != null) {
                JavaSetScoreTranslator.setBelowName(session, belowName, packet.getOwner(), 0);
            }
        } else {
            Objective objective = scoreboard.getObjective(packet.getObjective());
            objective.removeScore(packet.getOwner());

            // If this is the objective that is in use to show the below name text, we need to update the player
            // attached to this score.
            if (objective == belowName) {
                // Update the score on this player to now reflect 0
                JavaSetScoreTranslator.setBelowName(session, objective, packet.getOwner(), 0);
            }
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher than the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}
