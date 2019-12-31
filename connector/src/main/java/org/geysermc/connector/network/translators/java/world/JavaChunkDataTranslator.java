package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket;
import com.nukkitx.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.geysermc.api.Geyser;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.ChunkUtils;
import org.geysermc.connector.world.chunk.ChunkSection;

public class JavaChunkDataTranslator extends PacketTranslator<ServerChunkDataPacket> {

    @Override
    public void translate(ServerChunkDataPacket packet, GeyserSession session) {
        Geyser.getConnector().getGeneralThreadPool().execute(() -> {
            Vector2i chunkPos = session.getLastChunkPosition();
            Vector3f position = session.getPlayerEntity().getPosition();
            Vector2i newChunkPos = Vector2i.from(position.getFloorX() >> 4, position.getFloorZ() >> 4);

            if (chunkPos == null || !chunkPos.equals(newChunkPos)) {
                NetworkChunkPublisherUpdatePacket chunkPublisherUpdatePacket = new NetworkChunkPublisherUpdatePacket();
                chunkPublisherUpdatePacket.setPosition(position.toInt());
                chunkPublisherUpdatePacket.setRadius(session.getRenderDistance() << 4);
                session.getUpstream().sendPacket(chunkPublisherUpdatePacket);
                session.setLastChunkPosition(newChunkPos);
            }

            try {
                ChunkUtils.ChunkData chunkData = ChunkUtils.translateToBedrock(packet.getColumn());
                ByteBuf byteBuf = Unpooled.buffer(32);
                ChunkSection[] sections = chunkData.sections;

                int sectionCount = sections.length - 1;
                while (sectionCount >= 0 && sections[sectionCount].isEmpty()) {
                    sectionCount--;
                }
                sectionCount++;

                for (int i = 0; i < sectionCount; i++) {
                    ChunkSection section = chunkData.sections[i];
                    section.writeToNetwork(byteBuf);
                }

                byteBuf.writeBytes(chunkData.biomes); // Biomes - 256 bytes
                byteBuf.writeByte(0); // Border blocks - Edu edition only
                VarInts.writeUnsignedInt(byteBuf, 0); // extra data length, 0 for now

                ByteBufOutputStream stream = new ByteBufOutputStream(Unpooled.buffer());
                NBTOutputStream nbtStream = NbtUtils.createNetworkWriter(stream);
                for (CompoundTag blockEntity : chunkData.blockEntities) {
                    nbtStream.write(blockEntity);
                }

                byteBuf.writeBytes(stream.buffer());

                byte[] payload = new byte[byteBuf.writerIndex()];
                byteBuf.readBytes(payload);

                LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
                levelChunkPacket.setSubChunksLength(sectionCount);
                levelChunkPacket.setCachingEnabled(false);
                levelChunkPacket.setChunkX(packet.getColumn().getX());
                levelChunkPacket.setChunkZ(packet.getColumn().getZ());
                levelChunkPacket.setData(payload);
                session.getUpstream().sendPacket(levelChunkPacket);

                // Signs have to be sent after the chunk since in later versions they aren't included in the block entities
                for (Int2ObjectMap.Entry<CompoundTag> blockEntityEntry : chunkData.signs.int2ObjectEntrySet()) {
                    int x = blockEntityEntry.getValue().getAsInt("x");
                    int y = blockEntityEntry.getValue().getAsInt("y");
                    int z = blockEntityEntry.getValue().getAsInt("z");

                    ChunkUtils.updateBlock(session, new BlockState(blockEntityEntry.getIntKey()), new Position(x, y, z));
                }
                chunkData.signs.clear();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
