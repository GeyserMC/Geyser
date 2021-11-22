/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java.level;

import com.github.steveice10.mc.protocol.data.game.level.event.BonemealGrowEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.BreakBlockEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.BreakPotionEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.ComposterEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.DragonFireballEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.ParticleEvent;
import com.github.steveice10.mc.protocol.data.game.level.event.RecordEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.SmokeEventData;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import com.nukkitx.protocol.bedrock.v465.Bedrock_v465;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.level.event.LevelEventTranslator;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.text.MinecraftLocale;

import java.util.Collections;
import java.util.Locale;

@Translator(packet = ClientboundLevelEventPacket.class)
public class JavaLevelEventTranslator extends PacketTranslator<ClientboundLevelEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLevelEventPacket packet) {
        // Separate case since each RecordEventData in Java is an individual track in Bedrock
        if (packet.getEvent() == com.github.steveice10.mc.protocol.data.game.level.event.SoundEvent.RECORD) {
            RecordEventData recordEventData = (RecordEventData) packet.getData();
            SoundEvent soundEvent = Registries.RECORDS.getOrDefault(recordEventData.getRecordId(), SoundEvent.STOP_RECORD);
            Vector3f pos = Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()).add(0.5f, 0.5f, 0.5f);

            LevelSoundEventPacket levelSoundEvent = new LevelSoundEventPacket();
            levelSoundEvent.setIdentifier("");
            levelSoundEvent.setSound(soundEvent);
            levelSoundEvent.setPosition(pos);
            levelSoundEvent.setRelativeVolumeDisabled(packet.isBroadcast());
            levelSoundEvent.setExtraData(-1);
            levelSoundEvent.setBabySound(false);
            session.sendUpstreamPacket(levelSoundEvent);

            if (soundEvent != SoundEvent.STOP_RECORD) {
                // Send text packet as it seems to be handled in Java Edition client-side.
                TextPacket textPacket = new TextPacket();
                textPacket.setType(TextPacket.Type.JUKEBOX_POPUP);
                textPacket.setNeedsTranslation(true);
                textPacket.setXuid("");
                textPacket.setPlatformChatId("");
                textPacket.setSourceName(null);
                textPacket.setMessage("record.nowPlaying");
                String recordString = "%item." + soundEvent.name().toLowerCase(Locale.ROOT) + ".desc";
                textPacket.setParameters(Collections.singletonList(MinecraftLocale.getLocaleString(recordString, session.getLocale())));
                session.sendUpstreamPacket(textPacket);
            }
            return;
        }

        if (packet.getEvent() instanceof com.github.steveice10.mc.protocol.data.game.level.event.SoundEvent soundEvent) {
            LevelEventTranslator transformer = Registries.SOUND_EVENTS.get(soundEvent);
            if (transformer != null) {
                transformer.translate(session, packet);
                return;
            }
            GeyserImpl.getInstance().getLogger().debug("Unhandled sound event: " + soundEvent.name());
        } else if (packet.getEvent() instanceof ParticleEvent particleEvent) {
            Vector3f pos = Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()).add(0.5f, 0.5f, 0.5f);

            LevelEventPacket effectPacket = new LevelEventPacket();
            effectPacket.setPosition(pos);
            effectPacket.setData(0);
            switch (particleEvent) {
                case COMPOSTER -> {
                    effectPacket.setType(LevelEventType.PARTICLE_CROP_GROWTH);

                    ComposterEventData composterEventData = (ComposterEventData) packet.getData();
                    LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                    switch (composterEventData) {
                        case FILL -> soundEventPacket.setSound(SoundEvent.COMPOSTER_FILL);
                        case FILL_SUCCESS -> soundEventPacket.setSound(SoundEvent.COMPOSTER_FILL_LAYER);
                    }
                    soundEventPacket.setPosition(pos);
                    soundEventPacket.setIdentifier("");
                    soundEventPacket.setExtraData(-1);
                    soundEventPacket.setBabySound(false);
                    soundEventPacket.setRelativeVolumeDisabled(false);
                    session.sendUpstreamPacket(soundEventPacket);
                }
                case BLOCK_LAVA_EXTINGUISH -> {
                    effectPacket.setType(LevelEventType.PARTICLE_EVAPORATE);
                    effectPacket.setPosition(pos.add(-0.5f, 0.7f, -0.5f));

                    LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                    soundEventPacket.setSound(SoundEvent.EXTINGUISH_FIRE);
                    soundEventPacket.setPosition(pos);
                    soundEventPacket.setIdentifier("");
                    soundEventPacket.setExtraData(-1);
                    soundEventPacket.setBabySound(false);
                    soundEventPacket.setRelativeVolumeDisabled(false);
                    session.sendUpstreamPacket(soundEventPacket);
                }
                case BLOCK_REDSTONE_TORCH_BURNOUT -> {
                    effectPacket.setType(LevelEventType.PARTICLE_EVAPORATE);
                    effectPacket.setPosition(pos.add(-0.5f, 0, -0.5f));

                    LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                    soundEventPacket.setSound(SoundEvent.EXTINGUISH_FIRE);
                    soundEventPacket.setPosition(pos);
                    soundEventPacket.setIdentifier("");
                    soundEventPacket.setExtraData(-1);
                    soundEventPacket.setBabySound(false);
                    soundEventPacket.setRelativeVolumeDisabled(false);
                    session.sendUpstreamPacket(soundEventPacket);
                }
                case BLOCK_END_PORTAL_FRAME_FILL -> {
                    effectPacket.setType(LevelEventType.PARTICLE_EVAPORATE);
                    effectPacket.setPosition(pos.add(-0.5f, 0.3125f, -0.5f));

                    LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                    soundEventPacket.setSound(SoundEvent.BLOCK_END_PORTAL_FRAME_FILL);
                    soundEventPacket.setPosition(pos);
                    soundEventPacket.setIdentifier("");
                    soundEventPacket.setExtraData(-1);
                    soundEventPacket.setBabySound(false);
                    soundEventPacket.setRelativeVolumeDisabled(false);
                    session.sendUpstreamPacket(soundEventPacket);
                }
                case SMOKE -> {
                    effectPacket.setType(LevelEventType.PARTICLE_SHOOT);

                    SmokeEventData smokeEventData = (SmokeEventData) packet.getData();
                    int data = 0;
                    switch (smokeEventData) {
                        case DOWN -> {
                            data = 4;
                            pos = pos.add(0, -0.9f, 0);
                        }
                        case UP -> {
                            data = 4;
                            pos = pos.add(0, 0.5f, 0);
                        }
                        case NORTH -> {
                            data = 1;
                            pos = pos.add(0, -0.2f, -0.7f);
                        }
                        case SOUTH -> {
                            data = 7;
                            pos = pos.add(0, -0.2f, 0.7f);
                        }
                        case WEST -> {
                            data = 3;
                            pos = pos.add(-0.7f, -0.2f, 0);
                        }
                        case EAST -> {
                            data = 5;
                            pos = pos.add(0.7f, -0.2f, 0);
                        }
                    }
                    effectPacket.setPosition(pos);
                    effectPacket.setData(data);
                }

                //TODO: Block break particles when under fire
                case BREAK_BLOCK -> {
                    effectPacket.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);

                    BreakBlockEventData breakBlockEventData = (BreakBlockEventData) packet.getData();
                    effectPacket.setData(session.getBlockMappings().getBedrockBlockId(breakBlockEventData.getBlockState()));
                }
                case BREAK_SPLASH_POTION -> {
                    effectPacket.setType(LevelEventType.PARTICLE_POTION_SPLASH);
                    effectPacket.setPosition(pos.add(0, -0.5f, 0));

                    BreakPotionEventData splashPotionData = (BreakPotionEventData) packet.getData();
                    effectPacket.setData(splashPotionData.getPotionId());

                    LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                    soundEventPacket.setSound(SoundEvent.GLASS);
                    soundEventPacket.setPosition(pos);
                    soundEventPacket.setIdentifier("");
                    soundEventPacket.setExtraData(-1);
                    soundEventPacket.setBabySound(false);
                    soundEventPacket.setRelativeVolumeDisabled(false);
                    session.sendUpstreamPacket(soundEventPacket);
                }
                case BREAK_EYE_OF_ENDER -> effectPacket.setType(LevelEventType.PARTICLE_EYE_OF_ENDER_DEATH);
                case MOB_SPAWN -> effectPacket.setType(LevelEventType.PARTICLE_MOB_BLOCK_SPAWN); // TODO: Check, but I don't think I really verified this ever went into effect on Java
                case BONEMEAL_GROW_WITH_SOUND, BONEMEAL_GROW -> {
                    effectPacket.setType((particleEvent == ParticleEvent.BONEMEAL_GROW
                            && session.getUpstream().getProtocolVersion() >= Bedrock_v465.V465_CODEC.getProtocolVersion()) ? LevelEventType.PARTICLE_TURTLE_EGG : LevelEventType.PARTICLE_CROP_GROWTH);

                    BonemealGrowEventData growEventData = (BonemealGrowEventData) packet.getData();
                    effectPacket.setData(growEventData.getParticleCount());
                }
                case ENDERDRAGON_FIREBALL_EXPLODE -> {
                    effectPacket.setType(LevelEventType.PARTICLE_EYE_OF_ENDER_DEATH); // TODO

                    DragonFireballEventData fireballEventData = (DragonFireballEventData) packet.getData();
                    if (fireballEventData == DragonFireballEventData.HAS_SOUND) {
                        LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                        soundEventPacket.setSound(SoundEvent.EXPLODE);
                        soundEventPacket.setPosition(pos);
                        soundEventPacket.setIdentifier("");
                        soundEventPacket.setExtraData(-1);
                        soundEventPacket.setBabySound(false);
                        soundEventPacket.setRelativeVolumeDisabled(false);
                        session.sendUpstreamPacket(soundEventPacket);
                    }
                }
                case EXPLOSION -> {
                    effectPacket.setType(LevelEventType.PARTICLE_GENERIC_SPAWN);
                    effectPacket.setData(61);
                }
                case EVAPORATE -> {
                    effectPacket.setType(LevelEventType.PARTICLE_EVAPORATE_WATER);
                    effectPacket.setPosition(pos.add(-0.5f, 0.5f, -0.5f));
                }
                case END_GATEWAY_SPAWN -> {
                    effectPacket.setType(LevelEventType.PARTICLE_EXPLOSION);

                    LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                    soundEventPacket.setSound(SoundEvent.EXPLODE);
                    soundEventPacket.setPosition(pos);
                    soundEventPacket.setIdentifier("");
                    soundEventPacket.setExtraData(-1);
                    soundEventPacket.setBabySound(false);
                    soundEventPacket.setRelativeVolumeDisabled(false);
                    session.sendUpstreamPacket(soundEventPacket);
                }
                case DRIPSTONE_DRIP -> effectPacket.setType(LevelEventType.PARTICLE_DRIPSTONE_DRIP);
                case ELECTRIC_SPARK -> effectPacket.setType(LevelEventType.PARTICLE_ELECTRIC_SPARK); // Matches with a Bedrock server but doesn't seem to match up with Java
                case WAX_ON -> effectPacket.setType(LevelEventType.PARTICLE_WAX_ON);
                case WAX_OFF -> effectPacket.setType(LevelEventType.PARTICLE_WAX_OFF);
                case SCRAPE -> effectPacket.setType(LevelEventType.PARTICLE_SCRAPE);
                default -> {
                    GeyserImpl.getInstance().getLogger().debug("Unhandled particle event: " + particleEvent.name());
                    return;
                }
            }
            session.sendUpstreamPacket(effectPacket);
        }
    }
}