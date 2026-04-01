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

#include "org.geysermc.geyser.scoreboard.Objective"
#include "org.geysermc.geyser.scoreboard.Scoreboard"
#include "org.geysermc.geyser.scoreboard.ScoreboardUpdater"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.WorldCache"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket"

@Translator(packet = ClientboundSetObjectivePacket.class)
public class JavaSetObjectiveTranslator extends PacketTranslator<ClientboundSetObjectivePacket> {
    override public void translate(GeyserSession session, ClientboundSetObjectivePacket packet) {
        WorldCache worldCache = session.getWorldCache();
        Scoreboard scoreboard = worldCache.getScoreboard();
        int pps = worldCache.increaseAndGetScoreboardPacketsPerSecond();

        Objective objective;
        if (packet.getAction() == ObjectiveAction.ADD) {
            objective = scoreboard.registerNewObjective(packet.getName());
        } else {
            objective = scoreboard.getObjective(packet.getName());
        }


        if (objective == null) {
            return;
        }

        switch (packet.getAction()) {
            case ADD, UPDATE ->
                objective.updateProperties(packet.getDisplayName(), packet.getType(), packet.getNumberFormat());
            case REMOVE -> scoreboard.removeObjective(objective);
        }



        if (!objective.hasDisplaySlot()) {
            return;
        }



        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}
