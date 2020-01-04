package org.geysermc.connector.utils;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;

public class DimensionUtils {
    public static void switchDimension(GeyserSession session, int javaDimension, boolean fake) {
        int bedrockDimension = javaToBedrock(javaDimension);
        Entity player = session.getPlayerEntity();
        if (bedrockDimension == player.getDimension())
            return;
        Vector3i pos = Vector3i.from(0, Short.MAX_VALUE, 0);

        session.getEntityCache().removeAllEntities();

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

        if (fake) {
            ChunkUtils.sendEmptyChunks(session, pos, 2, true);
        }

        session.setSpawned(false);
        session.setSwitchingDimension(true);
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
