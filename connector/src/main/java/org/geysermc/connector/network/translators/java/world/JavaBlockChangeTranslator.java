package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.ChunkUtils;

public class JavaBlockChangeTranslator extends PacketTranslator<ServerBlockChangePacket> {

    @Override
    public void translate(ServerBlockChangePacket packet, GeyserSession session) {
        ChunkUtils.updateBlock(session, packet.getRecord().getBlock(), packet.getRecord().getPosition());
    }
}
