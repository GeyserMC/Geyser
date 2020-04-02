package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.effect.ParticleEffect;
import com.github.steveice10.mc.protocol.data.game.world.effect.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEvent2Packet;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.block.BlockTranslator;

@Translator(packet = ServerPlayEffectPacket.class)
public class JavaServerPlayEffectTranslator extends PacketTranslator<ServerPlayEffectPacket> {

    @Override
    public void translate(ServerPlayEffectPacket packet, GeyserSession session) {
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
                    // TODO: Block break doesn't work when you're the player
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
                case BLOCK_ANVIL_DESTROY:
                    effect.setType(LevelEventType.SOUND_ANVIL_BREAK);
                    break;
                case BLOCK_ANVIL_LAND:
                    effect.setType(LevelEventType.SOUND_ANVIL_FALL);
                    break;
                case BLOCK_ANVIL_USE:
                    effect.setType(LevelEventType.SOUND_ANVIL_USE);
                    break;
                //TODO: We can probably shorten this
                case BLOCK_IRON_DOOR_OPEN:
                case BLOCK_IRON_DOOR_CLOSE:
                case BLOCK_IRON_TRAPDOOR_OPEN:
                case BLOCK_IRON_TRAPDOOR_CLOSE:
                case BLOCK_WOODEN_DOOR_OPEN:
                case BLOCK_WOODEN_DOOR_CLOSE:
                case BLOCK_WOODEN_TRAPDOOR_OPEN:
                case BLOCK_WOODEN_TRAPDOOR_CLOSE:
                    effect.setType(LevelEventType.SOUND_DOOR);
                    break;
                default:
                    LevelSoundEvent2Packet soundEvent = new LevelSoundEvent2Packet();
                    soundEvent.setExtraData(-1);
                    switch (soundEffect) {
                        case BLOCK_END_PORTAL_SPAWN:
                            soundEvent.setSound(SoundEvent.BLOCK_END_PORTAL_SPAWN);
                            break;
                        case BLOCK_FIRE_EXTINGUISH:
                            soundEvent.setSound(SoundEvent.EXTINGUISH_FIRE);
                            break;
                        case ENTITY_BAT_TAKEOFF:
                            soundEvent.setSound(SoundEvent.TAKEOFF);
                            soundEvent.setIdentifier("minecraft:bat");
                            break;
                        case ENTITY_ENDEREYE_LAUNCH:
                            soundEvent.setSound(SoundEvent.THROW);
                            soundEvent.setIdentifier("minecraft:player");
                            break;
                        case ENTITY_FIREWORK_SHOOT:
                            soundEvent.setSound(SoundEvent.LAUNCH);
                            break;
                        case ENTITY_ZOMBIE_CONVERTED_TO_DROWNED:
                            soundEvent.setSound(SoundEvent.CONVERT_TO_DROWNED);
                            break;
                        case RECORD:
                            RecordEffectData recordEffectData = (RecordEffectData) packet.getData();
                            // This should absolutely be a mapping
                            switch (recordEffectData.getRecordId()) {
                                case 0:
                                    soundEvent.setSound(SoundEvent.STOP_RECORD);
                                    break;
                                case 841:
                                    soundEvent.setSound(SoundEvent.RECORD_13);
                                    break;
                                case 842:
                                    soundEvent.setSound(SoundEvent.RECORD_CAT);
                                    break;
                                case 843:
                                    soundEvent.setSound(SoundEvent.RECORD_BLOCKS);
                                    break;
                                case 844:
                                    soundEvent.setSound(SoundEvent.RECORD_CHIRP);
                                    break;
                                case 845:
                                    soundEvent.setSound(SoundEvent.RECORD_FAR);
                                    break;
                                case 846:
                                    soundEvent.setSound(SoundEvent.RECORD_MALL);
                                    break;
                                case 847:
                                    soundEvent.setSound(SoundEvent.RECORD_MELLOHI);
                                    break;
                                case 848:
                                    soundEvent.setSound(SoundEvent.RECORD_STAL);
                                    break;
                                case 849:
                                    soundEvent.setSound(SoundEvent.RECORD_STRAD);
                                    break;
                                case 850:
                                    soundEvent.setSound(SoundEvent.RECORD_WARD);
                                    break;
                                case 851:
                                    soundEvent.setSound(SoundEvent.RECORD_11);
                                    break;
                                case 852:
                                    soundEvent.setSound(SoundEvent.RECORD_WAIT);
                                    break;
                                default:
                                    GeyserConnector.getInstance().getLogger().debug("Unknown record ID found: " + recordEffectData.getRecordId());
                                    break;
                            }
                    }
                    if (soundEvent.getSound() != null) {
                        if (soundEvent.getIdentifier() == null) soundEvent.setIdentifier("");
                        soundEvent.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
                        session.getUpstream().sendPacket(soundEvent);
                    }
            }
            if (effect.getType() != null) {
                effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
                session.getUpstream().sendPacket(effect);
            }

        }
    }
}
