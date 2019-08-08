package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.Chunks;

public class JavaChunkDataPacket extends PacketTranslator<ServerChunkDataPacket> {
    @Override
    public void translate(ServerChunkDataPacket packet, GeyserSession session) {
        LevelChunkPacket levelChunkPacket = new LevelChunkPacket();

        Chunks.ChunkData data = Chunks.getData(packet.getColumn());
        levelChunkPacket.setSubChunksLength(data.count);
        levelChunkPacket.setData(data.bytes);
        levelChunkPacket.setChunkX(packet.getColumn().getX());
        levelChunkPacket.setChunkZ(packet.getColumn().getZ());

        GeyserLogger.DEFAULT.info("Sent chunk packet!");
        session.getUpstream().sendPacket(levelChunkPacket);
    }
}
