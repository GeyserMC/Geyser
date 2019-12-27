package org.geysermc.connector.utils;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;

import java.util.ArrayList;
import java.util.List;

public class DimensionUtils {
    public static void switchDimension(GeyserSession session, int javaDimension, boolean fake) {
        int bedrockDimension = javaToBedrock(javaDimension);
        Entity player = session.getPlayerEntity();
        if (bedrockDimension == player.getDimension())
            return;

        Vector3i pos = Vector3i.from(0, 32767, 0);

        List<Entity> entities = new ArrayList<>(session.getEntityCache().getEntities().values());
        for (Entity entity : entities) {
            session.getEntityCache().removeEntity(entity, false);
        }

        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.setDimension(bedrockDimension);
        changeDimensionPacket.setRespawn(true);
        changeDimensionPacket.setPosition(pos.toFloat());
        session.getUpstream().sendPacket(changeDimensionPacket);
        player.setDimension(bedrockDimension);

        //let java server handle portal travel sound
        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setStoppingAllSound(true);
        stopSoundPacket.setSoundName("");
        session.getUpstream().sendPacket(stopSoundPacket);

        EntityEventPacket eventPacket = new EntityEventPacket();
        eventPacket.setRuntimeEntityId(player.getGeyserId());
        eventPacket.setEvent(EntityEventPacket.Event.RESPAWN);
        eventPacket.setData(0);
        session.getUpstream().sendPacket(eventPacket);

        session.setSpawned(false);
        session.setSwitchingDim(true);

        if (fake) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sendEmptyChunks(session, pos.toInt());
            waitForAck(session);
        }
    }

    public static void waitForAck(GeyserSession session) {
        synchronized (session.getDimensionLock()) {
            try {
                while (session.isSwitchingDim())
                    session.getDimensionLock().wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void sendEmptyChunks(GeyserSession session, Vector3i position) {
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        NetworkChunkPublisherUpdatePacket chunkPublisherUpdatePacket = new NetworkChunkPublisherUpdatePacket();
        chunkPublisherUpdatePacket.setPosition(position);
        chunkPublisherUpdatePacket.setRadius(session.getRenderDistance() << 4);
        session.getUpstream().sendPacket(chunkPublisherUpdatePacket);
        session.setLastChunkPosition(null);
        for (int x = -5; x < 5; x++) {
            for (int z = -5; z < 5; z++) {
                LevelChunkPacket data = new LevelChunkPacket();
                data.setChunkX(chunkX + x);
                data.setChunkZ(chunkZ + z);
                data.setSubChunksLength(0);
                data.setData(TranslatorsInit.EMPTY_LEVEL_CHUNK_DATA);
                data.setCachingEnabled(false);
                session.getUpstream().sendPacket(data);
            }
        }
    }

    public static int javaToBedrock(int javaDimension) {
        switch (javaDimension) {
            case -1:
                return 1;
            case 1:
                return 2;
            default:
                return javaDimension;
        }
    }
}
