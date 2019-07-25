package org.geysermc.connector.network.translators.java.entity.spawn;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import com.nukkitx.protocol.bedrock.packet.SpawnExperienceOrbPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class JavaSpawnExpOrbTranslator extends PacketTranslator<ServerSpawnExpOrbPacket> {

    @Override
    public void translate(ServerSpawnExpOrbPacket packet, GeyserSession session) {
        SpawnExperienceOrbPacket spawnExperienceOrbPacket = new SpawnExperienceOrbPacket();
        spawnExperienceOrbPacket.setPosition(new Vector3f(packet.getX(), packet.getY(), packet.getZ()));
        spawnExperienceOrbPacket.setAmount(packet.getExp());

        session.getUpstream().sendPacket(spawnExperienceOrbPacket);
    }
}
