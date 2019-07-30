package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.ScoreboardCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.scoreboard.Scoreboard;
import org.geysermc.connector.network.translators.scoreboard.ScoreboardObjective;

public class JavaUpdateScoreTranslator extends PacketTranslator<ServerUpdateScorePacket> {
    @Override
    public void translate(ServerUpdateScorePacket packet, GeyserSession session) {
        try {
            ScoreboardCache cache = session.getScoreboardCache();
            Scoreboard scoreboard = new Scoreboard(session);
            if (cache.getScoreboard() != null)
                scoreboard = cache.getScoreboard();

            ScoreboardObjective objective = scoreboard.getObjective(packet.getObjective());
            if (objective == null) {
                objective = scoreboard.registerNewObjective(packet.getObjective());
            }

            switch (packet.getAction()) {
                case REMOVE:
                    objective.registerScore(packet.getEntry(), packet.getEntry(), packet.getValue(), SetScorePacket.Action.REMOVE);
                    break;
                case ADD_OR_UPDATE:
                    objective.registerScore(packet.getEntry(), packet.getEntry(), packet.getValue(), SetScorePacket.Action.SET);
                    break;
            }
            cache.setScoreboard(scoreboard);
            scoreboard.onUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
