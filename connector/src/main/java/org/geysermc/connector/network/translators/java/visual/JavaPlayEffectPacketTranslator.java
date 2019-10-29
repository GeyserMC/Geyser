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

        if(packet.getEffect() instanceof ParticleEffect) {
            ParticleEffect particleEffect = (ParticleEffect) packet.getEffect();

            levelEventPacket.setEvent(LevelEventPacket.Event.PARTICLE_SPAWN);

            Position pos = packet.getPosition();

            levelEventPacket.setPosition(Vector3f.from(pos.getX(), pos.getY(), pos.getZ()));
            //System.out.println(particleEffect);


            //TODO: Not hard coded
            switch (particleEffect) {
                case SMOKE:
                    levelEventPacket.setData(Particle.SMOKE);
                    break;
                case EXPLOSION:
                    levelEventPacket.setData(Particle.EXPLODE);
                    break;
                case ENDERDRAGON_FIREBALL_EXPLODE:
                    levelEventPacket.setData(Particle.DRAGONS_BREATH);
                    break;
            }

        }

        if(packet.getEffect() instanceof SoundEffect) {
            SoundEffect soundEffect = (SoundEffect) packet.getEffect();

            Position pos = packet.getPosition();

            levelEventPacket.setPosition(Vector3f.from(pos.getX(), pos.getY(), pos.getZ()));

            levelEventPacket.setEvent(TranslatorsInit.SOUNDS.get(soundEffect));

            if(levelEventPacket.getEvent() == null) {
                levelEventPacket.setData(5);
                levelEventPacket.setEvent(LevelEventPacket.Event.BLOCK_START_BREAK);
            }

        }

        session.getUpstream().sendPacketImmediately(levelEventPacket);
    }
}
