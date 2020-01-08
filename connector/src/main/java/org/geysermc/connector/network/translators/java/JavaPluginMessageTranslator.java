package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class JavaPluginMessageTranslator extends PacketTranslator<ServerPluginMessagePacket> {
    @Override
    public void translate(ServerPluginMessagePacket packet, GeyserSession session) {
        if (packet.getChannel().equals("minecraft:brand")) {
            session.getDownstream().getSession().send(
                    new ClientPluginMessagePacket(packet.getChannel(), GeyserConnector.NAME.getBytes())
            );
        }
    }
}
