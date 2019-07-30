package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.ScoreboardCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.scoreboard.Scoreboard;
import org.geysermc.connector.network.translators.scoreboard.ScoreboardObjective;
import org.geysermc.connector.utils.MessageUtils;

public class JavaScoreboardTeamTranslator extends PacketTranslator<ServerTeamPacket> {
    @Override
    public void translate(ServerTeamPacket packet, GeyserSession session) {

    }
}
