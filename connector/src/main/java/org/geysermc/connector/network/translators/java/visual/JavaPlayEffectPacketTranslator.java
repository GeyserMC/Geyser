package org.geysermc.connector.network.translators.java.visual;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.effect.ParticleEffect;
import com.github.steveice10.mc.protocol.data.game.world.effect.SoundEffect;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.effect.Particle;

public class JavaPlayEffectPacketTranslator extends PacketTranslator<ServerPlayEffectPacket> {
    @Override
    public void translate(ServerPlayEffectPacket packet, GeyserSession session) {
        LevelEventPacket levelEventPacket = new LevelEventPacket();

        //session.getUpstream().sendPacketImmediately(levelEventPacket);
    }
}
