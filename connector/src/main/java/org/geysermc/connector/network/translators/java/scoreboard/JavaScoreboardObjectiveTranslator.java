package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.ScoreboardCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.scoreboard.Scoreboard;
import org.geysermc.connector.network.translators.scoreboard.ScoreboardObjective;
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
