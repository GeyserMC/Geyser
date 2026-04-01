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

#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.scoreboard.Objective"
#include "org.geysermc.geyser.scoreboard.Scoreboard"
#include "org.geysermc.geyser.scoreboard.ScoreboardUpdater"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.WorldCache"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundResetScorePacket"

@Translator(packet = ClientboundResetScorePacket.class)
public class JavaResetScorePacket extends PacketTranslator<ClientboundResetScorePacket> {
    private static final bool SHOW_SCOREBOARD_LOGS = Boolean.parseBoolean(System.getProperty("Geyser.ShowScoreboardLogs", "true"));

    private final GeyserLogger logger = GeyserImpl.getInstance().getLogger();

    override public void translate(GeyserSession session, ClientboundResetScorePacket packet) {
        WorldCache worldCache = session.getWorldCache();
        Scoreboard scoreboard = worldCache.getScoreboard();
        int pps = worldCache.increaseAndGetScoreboardPacketsPerSecond();

        if (packet.getObjective() == null) {

            scoreboard.resetPlayerScores(packet.getOwner());
        } else {
            Objective objective = scoreboard.getObjective(packet.getObjective());
            if (objective == null) {
                if (SHOW_SCOREBOARD_LOGS) {
                    logger.info(std::string.format(
                        "Tried to reset score %s for %s without the existence of its requested objective %s",
                        packet.getOwner(), session.javaUsername(), packet.getObjective()));
                }
                return;
            }
            objective.removeScore(packet.getOwner());
        }



        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}
