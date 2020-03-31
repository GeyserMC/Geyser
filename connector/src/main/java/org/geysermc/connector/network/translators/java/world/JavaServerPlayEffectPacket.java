package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.effect.ParticleEffect;
import com.github.steveice10.mc.protocol.data.game.world.effect.*;
import com.github.steveice10.mc.protocol.data.game.world.effect.WorldEffect;
import com.github.steveice10.mc.protocol.data.game.world.particle.BlockParticleData;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.block.BlockTranslator;

@Translator(packet = ServerPlayEffectPacket.class)
public class JavaServerPlayEffectPacket extends PacketTranslator<ServerPlayEffectPacket> {

    @Override
    public void translate(ServerPlayEffectPacket packet, GeyserSession session) {
        LevelEventPacket particle = new LevelEventPacket();
        // Some things here are particles, others are not
        if (packet.getEffect() instanceof ParticleEffect) {
            ParticleEffect particleEffect = (ParticleEffect) packet.getEffect();
            switch (particleEffect) {
                case BONEMEAL_GROW:
                    particle.setType(LevelEventType.BONEMEAL);
                    BonemealGrowEffectData growEffectData = (BonemealGrowEffectData) packet.getData();
                    particle.setData(growEffectData.getParticleCount());
                    break;
                case BREAK_BLOCK:
                    particle.setType(LevelEventType.DESTROY);
                    BreakBlockEffectData breakBlockEffectData = (BreakBlockEffectData) packet.getData();
                    particle.setData(BlockTranslator.getBedrockBlockId(breakBlockEffectData.getBlockState()));
                    break;
                case BREAK_EYE_OF_ENDER:
                    particle.setType(LevelEventType.EYE_DESPAWN);
                    break;
                case BREAK_SPLASH_POTION:
                    // TODO: Check this one especially
                    particle.setType(LevelEventType.SPLASH);
                    break;
                case EXPLOSION:
                    particle.setType(LevelEventType.PARTICLE_EXPLODE);
                    break;
                case MOB_SPAWN:
                    particle.setType(LevelEventType.ENTITY_SPAWN);
                    break;

                default:
                    GeyserConnector.getInstance().getLogger().debug("No effect handling for effect: " + packet.getEffect());
            }

            particle.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.getUpstream().sendPacket(particle);
        }

    }
}
