package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.ScoreboardCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.scoreboard.Scoreboard;

public class JavaScoreboardDisplayTranslator extends PacketTranslator<ServerDisplayScoreboardPacket> {
    @Override
    public void translate(ServerDisplayScoreboardPacket packet, GeyserSession session) {
        try {
            ScoreboardCache cache = session.getScoreboardCache();
            Scoreboard scoreboard = new Scoreboard(session);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
