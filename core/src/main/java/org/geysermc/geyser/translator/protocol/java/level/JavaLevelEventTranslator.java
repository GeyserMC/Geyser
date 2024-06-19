/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.JukeboxSong;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.SoundMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.level.event.LevelEventTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.SoundUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;

import java.util.Collections;
import java.util.Set;

@Translator(packet = ClientboundLevelEventPacket.class)
public class JavaLevelEventTranslator extends PacketTranslator<ClientboundLevelEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLevelEventPacket packet) {
        if (!(packet.getEvent() instanceof LevelEventType levelEvent)) {
            return;
        }
        // Separate case since each RecordEventData in Java is an individual track in Bedrock
        if (levelEvent == LevelEventType.SOUND_PLAY_JUKEBOX_SONG) {
            RecordEventData recordEventData = (RecordEventData) packet.getData();
            JukeboxSong jukeboxSong = session.getRegistryCache().jukeboxSongs().byId(recordEventData.getRecordId());
            if (jukeboxSong == null) {
                return;
            }
            Vector3i origin = packet.getPosition();
            Vector3f pos = Vector3f.from(origin.getX() + 0.5f, origin.getY() + 0.5f, origin.getZ() + 0.5f);

            // Prioritize level events because it makes parrots dance.
            SoundMapping mapping = Registries.SOUNDS.get(jukeboxSong.soundEvent().replace("minecraft:", ""));
            SoundEvent soundEvent = null;
            if (mapping != null) {
                String bedrock = mapping.getBedrock();
                if (bedrock != null && !bedrock.isEmpty()) {
                    soundEvent = SoundUtils.toSoundEvent(bedrock);
                }
            }

            if (soundEvent != null) {
                LevelSoundEventPacket levelSoundEvent = new LevelSoundEventPacket();
                levelSoundEvent.setIdentifier("");
                levelSoundEvent.setSound(soundEvent);
                levelSoundEvent.setPosition(pos);
                levelSoundEvent.setRelativeVolumeDisabled(packet.isBroadcast());
                levelSoundEvent.setExtraData(-1);
                levelSoundEvent.setBabySound(false);
                session.sendUpstreamPacket(levelSoundEvent);
            } else {
                String bedrockSound = SoundUtils.translatePlaySound(jukeboxSong.soundEvent());
                // Pitch and volume from Java 1.21
                PlaySoundPacket playSoundPacket = new PlaySoundPacket();
                playSoundPacket.setPosition(pos);
                playSoundPacket.setSound(bedrockSound);
                playSoundPacket.setPitch(1.0f);
                playSoundPacket.setVolume(4.0f);
                session.sendUpstreamPacket(playSoundPacket);

                // Special behavior so we can cancel the record on our end
                session.getWorldCache().addActiveRecord(origin, bedrockSound);
            }

            // The level event for Java also indicates to show the text packet with the jukebox's description
            TextPacket textPacket = new TextPacket();
            textPacket.setType(TextPacket.Type.JUKEBOX_POPUP);
            textPacket.setNeedsTranslation(true);
            textPacket.setXuid("");
            textPacket.setPlatformChatId("");
            textPacket.setSourceName(null);
            textPacket.setMessage("record.nowPlaying");
            textPacket.setParameters(Collections.singletonList(MinecraftLocale.getLocaleString(jukeboxSong.description(), session.locale())));
            session.sendUpstreamPacket(textPacket);
            return;
        }

        // Check for a sound event translator
        LevelEventTranslator transformer = Registries.SOUND_LEVEL_EVENTS.get(packet.getEvent());
        if (transformer != null) {
            transformer.translate(session, packet);
            return;
        }

        Vector3i origin = packet.getPosition();
        Vector3f pos = Vector3f.from(origin.getX() + 0.5f, origin.getY() + 0.5f, origin.getZ() + 0.5f);

        LevelEventPacket effectPacket = new LevelEventPacket();
        effectPacket.setPosition(pos);
        effectPacket.setData(0);
        switch (levelEvent) {
            case PARTICLES_AND_SOUND_BRUSH_BLOCK_COMPLETE -> {
                effectPacket.setType(ParticleType.BRUSH_DUST);
                session.playSoundEvent(SoundEvent.BRUSH_COMPLETED, pos); // todo 1.20.2 verify this
            }
            case COMPOSTER_FILL -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_CROP_GROWTH);

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
            case LAVA_FIZZ -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EVAPORATE);
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
            case REDSTONE_TORCH_BURNOUT -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EVAPORATE);
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
            case END_PORTAL_FRAME_FILL -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EVAPORATE);
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
            case PARTICLES_SHOOT_SMOKE, PARTICLES_SHOOT_WHITE_SMOKE -> {
                if (levelEvent == LevelEventType.PARTICLES_SHOOT_SMOKE) {
                    effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_SHOOT);
                } else {
                    effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_SHOOT_WHITE_SMOKE);
                }

                SmokeEventData smokeEventData = (SmokeEventData) packet.getData();
                int data = 0;
                switch (smokeEventData.getDirection()) {
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
            case PARTICLES_DESTROY_BLOCK -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_DESTROY_BLOCK);

                BreakBlockEventData breakBlockEventData = (BreakBlockEventData) packet.getData();
                effectPacket.setData(session.getBlockMappings().getBedrockBlockId(breakBlockEventData.getBlockState()));
            }
            case PARTICLES_SPELL_POTION_SPLASH, PARTICLES_INSTANT_POTION_SPLASH -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_POTION_SPLASH);
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
            case PARTICLES_EYE_OF_ENDER_DEATH -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EYE_OF_ENDER_DEATH);
            case PARTICLES_MOBBLOCK_SPAWN -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_MOB_BLOCK_SPAWN); // TODO: Check, but I don't think I really verified this ever went into effect on Java
            case PARTICLES_AND_SOUND_PLANT_GROWTH -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_CROP_GROWTH);

                BonemealGrowEventData growEventData = (BonemealGrowEventData) packet.getData();
                effectPacket.setData(growEventData.getParticleCount());
            }
            case PARTICLES_EGG_CRACK -> effectPacket.setType(ParticleType.VILLAGER_HAPPY); // both the lil green sparkle
            case PARTICLES_DRAGON_FIREBALL_SPLASH -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EYE_OF_ENDER_DEATH); // TODO

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
            case PARTICLES_DRAGON_BLOCK_BREAK -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_GENERIC_SPAWN);
                effectPacket.setData(61);
            }
            case PARTICLES_WATER_EVAPORATING -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EVAPORATE_WATER);
                effectPacket.setPosition(pos.add(-0.5f, 0.5f, -0.5f));
            }
            case ANIMATION_END_GATEWAY_SPAWN -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EXPLOSION);

                LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                soundEventPacket.setSound(SoundEvent.EXPLODE);
                soundEventPacket.setPosition(pos);
                soundEventPacket.setIdentifier("");
                soundEventPacket.setExtraData(-1);
                soundEventPacket.setBabySound(false);
                soundEventPacket.setRelativeVolumeDisabled(false);
                session.sendUpstreamPacket(soundEventPacket);
            }
            case DRIPSTONE_DRIP -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_DRIPSTONE_DRIP);
            case PARTICLES_ELECTRIC_SPARK -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_ELECTRIC_SPARK); // Matches with a Bedrock server but doesn't seem to match up with Java
            case PARTICLES_AND_SOUND_WAX_ON -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_WAX_ON);
            case PARTICLES_WAX_OFF -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_WAX_OFF);
            case PARTICLES_SCRAPE -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_SCRAPE);
            case PARTICLES_SCULK_CHARGE -> {
                SculkBlockChargeEventData eventData = (SculkBlockChargeEventData) packet.getData();
                LevelEventGenericPacket levelEventPacket = new LevelEventGenericPacket();
                // TODO add SCULK_BLOCK_CHARGE sound
                if (eventData.getCharge() > 0) {
                    levelEventPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.SCULK_CHARGE);
                    levelEventPacket.setTag(
                            NbtMap.builder()
                                    .putInt("x", packet.getPosition().getX())
                                    .putInt("y", packet.getPosition().getY())
                                    .putInt("z", packet.getPosition().getZ())
                                    .putShort("charge", (short) eventData.getCharge())
                                    .putShort("facing", encodeFacing(eventData.getBlockFaces())) // TODO check if this is actually correct
                                    .build()
                    );
                } else {
                    levelEventPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.SCULK_CHARGE_POP);
                    levelEventPacket.setTag(
                            NbtMap.builder()
                                    .putInt("x", packet.getPosition().getX())
                                    .putInt("y", packet.getPosition().getY())
                                    .putInt("z", packet.getPosition().getZ())
                                    .build()
                    );
                }
                session.sendUpstreamPacket(levelEventPacket);
                return;
            }
            case PARTICLES_SCULK_SHRIEK -> {
                LevelEventGenericPacket levelEventPacket = new LevelEventGenericPacket();
                levelEventPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_SCULK_SHRIEK);
                levelEventPacket.setTag(
                        NbtMap.builder()
                                .putInt("originX", packet.getPosition().getX())
                                .putInt("originY", packet.getPosition().getY())
                                .putInt("originZ", packet.getPosition().getZ())
                                .build()
                );
                session.sendUpstreamPacket(levelEventPacket);

                LevelSoundEventPacket soundEventPacket = new LevelSoundEventPacket();
                soundEventPacket.setSound(SoundEvent.SCULK_SHRIEKER_SHRIEK);
                soundEventPacket.setPosition(packet.getPosition().toFloat());
                soundEventPacket.setExtraData(-1);
                soundEventPacket.setIdentifier("");
                soundEventPacket.setBabySound(false);
                soundEventPacket.setRelativeVolumeDisabled(false);
                session.sendUpstreamPacket(soundEventPacket);
                return;
            }
            case SOUND_STOP_JUKEBOX_SONG -> {
                String bedrockSound = session.getWorldCache().removeActiveRecord(origin);
                if (bedrockSound == null) {
                    // Vanilla record
                    LevelSoundEventPacket levelSoundEvent = new LevelSoundEventPacket();
                    levelSoundEvent.setIdentifier("");
                    levelSoundEvent.setSound(SoundEvent.STOP_RECORD);
                    levelSoundEvent.setPosition(pos);
                    levelSoundEvent.setRelativeVolumeDisabled(false);
                    levelSoundEvent.setExtraData(-1);
                    levelSoundEvent.setBabySound(false);
                    session.sendUpstreamPacket(levelSoundEvent);
                } else {
                    // Custom record
                    StopSoundPacket stopSound = new StopSoundPacket();
                    stopSound.setSoundName(bedrockSound);
                    session.sendUpstreamPacket(stopSound);
                }
                return;
            }
            default -> {
                GeyserImpl.getInstance().getLogger().debug("Unhandled level event: " + packet.getEvent());
                return;
            }
        }
        session.sendUpstreamPacket(effectPacket);
    }

    private short encodeFacing(Set<Direction> blockFaces) {
        short facing = 0;
        if (blockFaces.contains(Direction.DOWN)) {
            facing |= 1;
        }
        if (blockFaces.contains(Direction.UP)) {
            facing |= 1 << 1;
        }
        if (blockFaces.contains(Direction.SOUTH)) {
            facing |= 1 << 2;
        }
        if (blockFaces.contains(Direction.WEST)) {
            facing |= 1 << 3;
        }
        if (blockFaces.contains(Direction.NORTH)) {
            facing |= 1 << 4;
        }
        if (blockFaces.contains(Direction.EAST)) {
            facing |= 1 << 5;
        }
        return facing;
    }
}