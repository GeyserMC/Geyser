package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.Chunks;

public class JavaChunk extends PacketTranslator<ServerChunkDataPacket> {
    @Override
    public void translate(ServerChunkDataPacket packet, GeyserSession session) {
        LevelChunkPacket p = new LevelChunkPacket();

        Chunks.ChunkData data = Chunks.getData(packet.getColumn());

        p.setSubChunksLength(data.count);

        p.setData(data.bytes);

        p.setChunkX(packet.getColumn().getX());
        p.setChunkZ(packet.getColumn().getZ());

        System.out.println("sent");

        session.getUpstream().sendPacketImmediately(p);
    }
}
