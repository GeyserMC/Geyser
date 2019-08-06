/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.ScoreboardCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.scoreboard.Scoreboard;
import org.geysermc.connector.scoreboard.ScoreboardObjective;
import org.geysermc.connector.utils.MessageUtils;

public class JavaScoreboardObjectiveTranslator extends PacketTranslator<ServerScoreboardObjectivePacket> {

    @Override
    public void translate(ServerScoreboardObjectivePacket packet, GeyserSession session) {
        try {
            ScoreboardCache cache = session.getScoreboardCache();
            Scoreboard scoreboard = new Scoreboard(session);
            if (cache.getScoreboard() != null)
                scoreboard = cache.getScoreboard();

            switch (packet.getAction()) {
                case ADD:
                    ScoreboardObjective objective = scoreboard.registerNewObjective(packet.getName());
                    objective.setDisplaySlot(ScoreboardObjective.DisplaySlot.SIDEBAR);
                    objective.setDisplayName(MessageUtils.getBedrockMessage(packet.getDisplayName()));
                    break;
                case UPDATE:
                    ScoreboardObjective updateObj = scoreboard.getObjective(packet.getName());
                    updateObj.setDisplayName(MessageUtils.getBedrockMessage(packet.getDisplayName()));
                    break;
                case REMOVE:
                    scoreboard.unregisterObjective(packet.getName());
                    break;
            }

            scoreboard.onUpdate();
            cache.setScoreboard(scoreboard);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
