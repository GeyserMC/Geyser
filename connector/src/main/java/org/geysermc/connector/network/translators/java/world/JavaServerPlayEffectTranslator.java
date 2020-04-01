package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.effect.ParticleEffect;
import com.github.steveice10.mc.protocol.data.game.world.effect.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.block.BlockTranslator;

@Translator(packet = ServerPlayEffectPacket.class)
public class JavaServerPlayEffectTranslator extends PacketTranslator<ServerPlayEffectPacket> {

    @Override
    public void translate(ServerPlayEffectPacket packet, GeyserSession session) {
        System.out.println("Translating: " + packet.getEffect());
        System.out.println("Packet type: " + packet.getEffect().getClass());
        System.out.println("Data: " + packet.getData());
        LevelEventPacket effect = new LevelEventPacket();
        // Some things here are particles, others are not
        if (packet.getEffect() instanceof ParticleEffect) {
            ParticleEffect particleEffect = (ParticleEffect) packet.getEffect();
            switch (particleEffect) {
                case BONEMEAL_GROW:
                    effect.setType(LevelEventType.BONEMEAL);
                    BonemealGrowEffectData growEffectData = (BonemealGrowEffectData) packet.getData();
                    effect.setData(growEffectData.getParticleCount());
                    break;
                case BREAK_BLOCK:
                    effect.setType(LevelEventType.DESTROY);
                    BreakBlockEffectData breakBlockEffectData = (BreakBlockEffectData) packet.getData();
                    effect.setData(BlockTranslator.getBedrockBlockId(breakBlockEffectData.getBlockState()));
                    break;
                case BREAK_EYE_OF_ENDER:
                    effect.setType(LevelEventType.EYE_DESPAWN);
                    break;
                case BREAK_SPLASH_POTION:
                    // TODO: Check this one especially
                    effect.setType(LevelEventType.SPLASH);
                    break;
                case EXPLOSION:
                    effect.setType(LevelEventType.PARTICLE_EXPLODE);
                    break;
                case MOB_SPAWN:
                    effect.setType(LevelEventType.ENTITY_SPAWN);
                    break;
                case SMOKE:
                    effect.setType(LevelEventType.PARTICLE_SMOKE);
                    break;
                default:
                    GeyserConnector.getInstance().getLogger().debug("No effect handling for effect: " + packet.getEffect());
            }

            effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.getUpstream().sendPacket(effect);
        } else if (packet.getEffect() instanceof SoundEffect) {
            SoundEffect soundEffect = (SoundEffect) packet.getEffect();
            switch (soundEffect) {
                // TODO: Finish these
                // Also consider: json map for this??
                case BLOCK_ANVIL_LAND:
                    effect.setType(LevelEventType.SOUND_ANVIL_FALL);
                case BLOCK_ANVIL_USE:
                    effect.setType(LevelEventType.SOUND_ANVIL_USE);
                    break;
            }
            effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.getUpstream().sendPacket(effect);
        }
    }
}
