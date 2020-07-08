/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.effect.ParticleEffect;
import com.github.steveice10.mc.protocol.data.game.world.effect.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.effect.Effect;
import org.geysermc.connector.network.translators.effect.EffectRegistry;
import org.geysermc.connector.utils.LocaleUtils;

import java.util.ArrayList;
import java.util.List;

@Translator(packet = ServerPlayEffectPacket.class)
public class JavaPlayEffectTranslator extends PacketTranslator<ServerPlayEffectPacket> {

    // TODO: Update mappings since they're definitely all going to be wrong now
    @Override
    public void translate(ServerPlayEffectPacket packet, GeyserSession session) {
        LevelEventPacket effect = new LevelEventPacket();
        // Some things here are particles, others are not
        if (packet.getEffect() instanceof ParticleEffect) {
            ParticleEffect particleEffect = (ParticleEffect) packet.getEffect();
            Effect geyserEffect = EffectRegistry.EFFECTS.get(particleEffect.name());
            if (geyserEffect != null) {
                String name = geyserEffect.getBedrockName();
                effect.setType(LevelEventType.valueOf(name));
            } else {
                switch (particleEffect) {
                    // TODO: BREAK_SPLASH_POTION has additional data
                    case BONEMEAL_GROW:
                        effect.setType(LevelEventType.PARTICLE_CROP_GROWTH);
                        BonemealGrowEffectData growEffectData = (BonemealGrowEffectData) packet.getData();
                        effect.setData(growEffectData.getParticleCount());
                        break;
                    //TODO: Block break particles when under fire
                    case BREAK_BLOCK:
                        effect.setType(LevelEventType.PARTICLE_DESTROY_BLOCK); // TODO: Check to make sure this is right
                        BreakBlockEffectData breakBlockEffectData = (BreakBlockEffectData) packet.getData();
                        effect.setData(BlockTranslator.getBedrockBlockId(breakBlockEffectData.getBlockState()));
                        break;
                    case EXPLOSION:
                        effect.setType(LevelEventType.PARTICLE_EXPLOSION);
                        break;
                    case MOB_SPAWN:
                        effect.setType(LevelEventType.PARTICLE_MOB_BLOCK_SPAWN); // TODO: Check, but I don't think I really verified this ever went into effect on Java
                        break;
                        // Done with a dispenser
                    case SMOKE:
                        // Might need to be SHOOT
                        effect.setType(LevelEventType.PARTICLE_SMOKE);
                        break;
                    case COMPOSTER:
                        effect.setType(LevelEventType.PARTICLE_CROP_GROWTH);

                        ComposterEffectData composterEffectData = (ComposterEffectData) packet.getData();
                        LevelSoundEventPacket soundEvent = new LevelSoundEventPacket();
                        soundEvent.setSound(SoundEvent.valueOf("COMPOSTER_" + composterEffectData.name()));
                        soundEvent.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
                        soundEvent.setIdentifier(":");
                        soundEvent.setExtraData(-1);
                        soundEvent.setBabySound(false);
                        soundEvent.setRelativeVolumeDisabled(false);
                        session.sendUpstreamPacket(soundEvent);
                        break;
                    case BLOCK_LAVA_EXTINGUISH:
                        effect.setType(LevelEventType.PARTICLE_SHOOT);
                        effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY() + 1, packet.getPosition().getZ()));
                        session.sendUpstreamPacket(effect);

                        LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                        soundEventPacket.setSound(SoundEvent.EXTINGUISH_FIRE);
                        soundEventPacket.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
                        soundEventPacket.setIdentifier(":");
                        soundEventPacket.setExtraData(-1);
                        soundEventPacket.setBabySound(false);
                        soundEventPacket.setRelativeVolumeDisabled(false);
                        session.sendUpstreamPacket(soundEventPacket);
                        return;
                    default:
                        GeyserConnector.getInstance().getLogger().debug("No effect handling for particle effect: " + packet.getEffect());
                }
            }
            effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.sendUpstreamPacket(effect);
        } else if (packet.getEffect() instanceof SoundEffect) {
            SoundEffect soundEffect = (SoundEffect) packet.getEffect();
            Effect geyserEffect = EffectRegistry.EFFECTS.get(soundEffect.name());
            if (geyserEffect != null) {
                // Some events are LevelEventTypes, some are SoundEvents.
                if (geyserEffect.getType().equals("soundLevel")) {
                    effect.setType(LevelEventType.valueOf(geyserEffect.getBedrockName()));
                } else if (geyserEffect.getType().equals("soundEvent")) {
                    LevelSoundEventPacket soundEvent = new LevelSoundEventPacket();
                    // Separate case since each RecordEffectData in Java is an individual track in Bedrock
                    if (geyserEffect.getJavaName().equals("RECORD")) {
                        RecordEffectData recordEffectData = (RecordEffectData) packet.getData();
                        soundEvent.setSound(EffectRegistry.RECORDS.get(recordEffectData.getRecordId()));
                        if (EffectRegistry.RECORDS.get(recordEffectData.getRecordId()) != SoundEvent.STOP_RECORD) {
                            // Send text packet as it seems to be handled in Java Edition client-side.
                            TextPacket textPacket = new TextPacket();
                            textPacket.setType(TextPacket.Type.JUKEBOX_POPUP);
                            textPacket.setNeedsTranslation(true);
                            textPacket.setXuid("");
                            textPacket.setPlatformChatId("");
                            textPacket.setSourceName(null);
                            textPacket.setMessage("record.nowPlaying");
                            List<String> params = new ArrayList<>();
                            String recordString = "%item." + EffectRegistry.RECORDS.get(recordEffectData.getRecordId()).name().toLowerCase() + ".desc";
                            params.add(LocaleUtils.getLocaleString(recordString, session.getClientData().getLanguageCode()));
                            textPacket.setParameters(params);
                            session.sendUpstreamPacket(textPacket);
                        }
                    } else {
                        soundEvent.setSound(SoundEvent.valueOf(geyserEffect.getBedrockName()));
                    }
                    soundEvent.setExtraData(geyserEffect.getData());
                    soundEvent.setIdentifier(geyserEffect.getIdentifier());
                    soundEvent.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
                    session.sendUpstreamPacket(soundEvent);
                }
            } else {
                GeyserConnector.getInstance().getLogger().debug("No effect handling for sound effect: " + packet.getEffect());
            }
        }
        if (effect.getType() != null) {
            effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.sendUpstreamPacket(effect);
        }

    }
}