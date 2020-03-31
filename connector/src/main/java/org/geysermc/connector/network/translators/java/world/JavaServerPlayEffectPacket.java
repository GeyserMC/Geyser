package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.effect.ParticleEffect;
import com.github.steveice10.mc.protocol.data.game.world.effect.WorldEffect;
import com.github.steveice10.mc.protocol.data.game.world.particle.BlockParticleData;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.block.BlockTranslator;

@Translator(packet = ServerPlayEffectPacket.class)
public class JavaServerPlayEffectPacket extends PacketTranslator<ServerPlayEffectPacket> {

    @Override
    public void translate(ServerPlayEffectPacket packet, GeyserSession session) {
        System.out.println("Translating: " + packet.getEffect());
        WorldEffect effect = packet.getEffect();
        LevelEventPacket particle = new LevelEventPacket();
        // Currently sends no particle but does send a sound event
        // TODO: Make it not sound like stone breaking
        if (effect == ParticleEffect.BREAK_BLOCK) {
            particle.setType(LevelEventType.DESTROY);
            //particle.setData(BlockTranslator.getBedrockBlockId((packet.getData().getBlockState()));
            particle.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.getUpstream().sendPacket(particle);
        }

    }
}
