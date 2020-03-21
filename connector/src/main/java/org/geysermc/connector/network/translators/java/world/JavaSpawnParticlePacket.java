package org.geysermc.connector.network.translators.java.world;

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.ParticleUtils;
import org.slf4j.LoggerFactory;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnParticlePacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.SpawnParticleEffectPacket;

public class JavaSpawnParticlePacket extends PacketTranslator<ServerSpawnParticlePacket> {

    @Override
    public void translate(ServerSpawnParticlePacket packet, GeyserSession session) {
        if (ParticleUtils.hasIdentifier(packet.getParticle().getType())) {
            SpawnParticleEffectPacket particlePacket = new SpawnParticleEffectPacket();
            
            particlePacket.setDimensionId(session.getPlayerEntity().getDimension());
            particlePacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
            particlePacket.setIdentifier(ParticleUtils.getIdentifier(packet.getParticle().getType()));
            
            session.getUpstream().sendPacket(particlePacket);
        } else {
            LoggerFactory.getLogger(this.getClass()).debug("No particle mapping for " + packet.getParticle().getType());
        }
    }

}
