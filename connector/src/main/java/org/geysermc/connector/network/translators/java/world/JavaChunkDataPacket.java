package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.ChunkUtils;
import org.geysermc.connector.world.chunk.ChunkSection;

public class JavaChunkDataPacket extends PacketTranslator<ServerChunkDataPacket> {

    @Override
    public void translate(ServerChunkDataPacket packet, GeyserSession session) {

        try {
            byte[] buffer = new byte[32];
            ChunkUtils.ChunkData chunkData = ChunkUtils.translateToBedrock(packet.getColumn());

            int count = 0;
            ChunkSection[] sections = chunkData.sections;
            for (int i = sections.length - 1; i >= 0; i--) {
                if (sections[i].isEmpty())
                    continue;

                count = i + 1;
                break;
            }

            for (int i = 0; i < count; i++) {
                ChunkUtils.putBytes(count, buffer, new byte[]{(byte) 0});
                ChunkSection section = chunkData.sections[i];

                ByteBuf byteBuf = Unpooled.buffer();
                section.writeToNetwork(byteBuf);
                byte[] byteData = byteBuf.array();
                ChunkUtils.putBytes(count, buffer, byteData);
            }

            LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
            levelChunkPacket.setSubChunksLength(16);
            levelChunkPacket.setCachingEnabled(true);
            levelChunkPacket.setChunkX(packet.getColumn().getX());
            levelChunkPacket.setChunkZ(packet.getColumn().getZ());
            levelChunkPacket.setData(buffer);
            session.getUpstream().sendPacket(levelChunkPacket);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        GeyserLogger.DEFAULT.info("Sent chunk packet!");
    }
}
