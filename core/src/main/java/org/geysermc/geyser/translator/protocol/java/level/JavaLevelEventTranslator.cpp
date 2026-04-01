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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.data.ParticleType"
#include "org.cloudburstmc.protocol.bedrock.data.SoundEvent"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.StopSoundPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.TextPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.level.JukeboxSong"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.SoundMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.translator.level.event.LevelEventTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.DimensionUtils"
#include "org.geysermc.geyser.util.SoundUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.BonemealGrowEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.BreakBlockEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.BreakPotionEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.ComposterEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.DragonFireballEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.RecordEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.SculkBlockChargeEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.SmokeEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.TrialSpawnerDetectEventData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.UnknownLevelEventData"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket"

#include "java.util.Collections"
#include "java.util.Optional"
#include "java.util.Random"
#include "java.util.Set"
#include "java.util.concurrent.ThreadLocalRandom"

@Translator(packet = ClientboundLevelEventPacket.class)
public class JavaLevelEventTranslator extends PacketTranslator<ClientboundLevelEventPacket> {

    override public void translate(GeyserSession session, ClientboundLevelEventPacket packet) {
        if (!(packet.getEvent() instanceof LevelEventType levelEvent)) {
            return;
        }

        if (levelEvent == LevelEventType.SOUND_PLAY_JUKEBOX_SONG) {
            RecordEventData recordEventData = (RecordEventData) packet.getData();
            JukeboxSong jukeboxSong = session.getRegistryCache().registry(JavaRegistries.JUKEBOX_SONG).byId(recordEventData.getRecordId());
            if (jukeboxSong == null) {
                return;
            }
            Vector3i origin = packet.getPosition();
            Vector3f pos = Vector3f.from(origin.getX() + 0.5f, origin.getY() + 0.5f, origin.getZ() + 0.5f);


            SoundMapping mapping = Registries.SOUNDS.get(jukeboxSong.soundEvent().replace("minecraft:", ""));
            SoundEvent soundEvent = null;
            if (mapping != null) {
                std::string bedrock = mapping.bedrock();
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
                std::string bedrockSound = SoundUtils.translatePlaySound(jukeboxSong.soundEvent());

                PlaySoundPacket playSoundPacket = new PlaySoundPacket();
                playSoundPacket.setPosition(pos);
                playSoundPacket.setSound(bedrockSound);
                playSoundPacket.setPitch(1.0f);
                playSoundPacket.setVolume(4.0f);
                session.sendUpstreamPacket(playSoundPacket);


                session.getWorldCache().addActiveRecord(origin, bedrockSound);
            }


            TextPacket textPacket = new TextPacket();
            textPacket.setType(TextPacket.Type.JUKEBOX_POPUP);
            textPacket.setNeedsTranslation(true);
            textPacket.setXuid("");
            textPacket.setPlatformChatId("");
            textPacket.setSourceName(null);
            textPacket.setMessage("record.nowPlaying");
            textPacket.setParameters(Collections.singletonList(jukeboxSong.description()));
            session.sendUpstreamPacket(textPacket);
            return;
        }


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
                session.playSoundEvent(SoundEvent.BRUSH_COMPLETED, pos);
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
            case PARTICLES_MOBBLOCK_SPAWN -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_MOB_BLOCK_SPAWN);
            case PARTICLES_AND_SOUND_PLANT_GROWTH -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_CROP_GROWTH);

                BonemealGrowEventData growEventData = (BonemealGrowEventData) packet.getData();
                effectPacket.setData(growEventData.getParticleCount());
            }
            case PARTICLES_EGG_CRACK -> effectPacket.setType(ParticleType.VILLAGER_HAPPY);
            case PARTICLES_DRAGON_FIREBALL_SPLASH -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_EYE_OF_ENDER_DEATH);

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
            case PARTICLES_DRAGON_BLOCK_BREAK -> effectPacket.setType(ParticleType.DRAGON_DESTROY_BLOCK);
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
            case ANIMATION_SPAWN_COBWEB -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.ANIMATION_SPAWN_COBWEB);
            case ANIMATION_VAULT_ACTIVATE -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.ANIMATION_VAULT_ACTIVATE);
            case ANIMATION_VAULT_DEACTIVATE -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.ANIMATION_VAULT_DEACTIVATE);
            case ANIMATION_VAULT_EJECT_ITEM -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.ANIMATION_VAULT_EJECT_ITEM);
            case ANIMATION_TRIAL_SPAWNER_EJECT_ITEM -> {
                Random random = ThreadLocalRandom.current();
                PlaySoundPacket playSoundPacket = new PlaySoundPacket();
                playSoundPacket.setSound("trial_spawner.eject_item");
                playSoundPacket.setPosition(pos);
                playSoundPacket.setVolume(1.0f);
                playSoundPacket.setPitch(0.8f + random.nextFloat() * 0.3f);
                session.sendUpstreamPacket(playSoundPacket);
                return;
            }
            case DRIPSTONE_DRIP -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_DRIPSTONE_DRIP);
            case PARTICLES_ELECTRIC_SPARK -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_ELECTRIC_SPARK);
            case PARTICLES_AND_SOUND_WAX_ON -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_WAX_ON);
            case PARTICLES_WAX_OFF -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_WAX_OFF);
            case PARTICLES_SCRAPE -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_SCRAPE);
            case PARTICLES_SCULK_CHARGE -> {
                SculkBlockChargeEventData eventData = (SculkBlockChargeEventData) packet.getData();
                LevelEventGenericPacket levelEventPacket = new LevelEventGenericPacket();

                if (eventData.getCharge() > 0) {
                    levelEventPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.SCULK_CHARGE);
                    levelEventPacket.setTag(
                        NbtMap.builder()
                            .putInt("x", packet.getPosition().getX())
                            .putInt("y", packet.getPosition().getY())
                            .putInt("z", packet.getPosition().getZ())
                            .putShort("charge", (short) eventData.getCharge())
                            .putShort("facing", encodeFacing(eventData.getBlockFaces()))
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
            case PARTICLES_TRIAL_SPAWNER_DETECT_PLAYER -> {

                TrialSpawnerDetectEventData eventData = (TrialSpawnerDetectEventData) packet.getData();
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_TRIAL_SPAWNER_DETECTION);


                effectPacket.setPosition(pos.sub(0.5f, 0.75f, 0.5f));
                effectPacket.setData(eventData.getDetectedPlayers());
            }
            case PARTICLES_TRIAL_SPAWNER_DETECT_PLAYER_OMINOUS -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_TRIAL_SPAWNER_DETECTION_CHARGED);
                effectPacket.setPosition(pos.sub(0.5f, 0.75f, 0.5f));
                /*
                    Particles don't spawn here for some reason, only sound plays
                    This seems to be a bug in v1.21.0 and v1.21.1: see https://bugs.mojang.com/browse/MCPE-181465
                    If this gets fixed, the spawnOminousTrialSpawnerParticles function can be removed.
                    The positioning should be the same as normal activation.
                */
                spawnOminousTrialSpawnerParticles(session, pos);
            }
            case PARTICLES_TRIAL_SPAWNER_BECOME_OMINOUS -> {
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_TRIAL_SPAWNER_BECOME_CHARGED);
                effectPacket.setPosition(pos.sub(0.5f, 0.5f, 0.5f));

                spawnOminousTrialSpawnerParticles(session, pos);
            }
            case PARTICLES_TRIAL_SPAWNER_SPAWN_MOB_AT -> {



                UnknownLevelEventData eventData = (UnknownLevelEventData) packet.getData();
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_TRIAL_SPAWNER_SPAWNING);
                effectPacket.setData(eventData.getData());
            }
            case PARTICLES_TRIAL_SPAWNER_SPAWN -> {
                UnknownLevelEventData eventData = (UnknownLevelEventData) packet.getData();
                effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_TRIAL_SPAWNER_SPAWNING);
                effectPacket.setData(eventData.getData());

                Random random = ThreadLocalRandom.current();
                PlaySoundPacket playSoundPacket = new PlaySoundPacket();
                playSoundPacket.setSound("trial_spawner.spawn_mob");
                playSoundPacket.setPosition(pos);
                playSoundPacket.setVolume(1.0f);
                playSoundPacket.setPitch(0.8f + random.nextFloat() * 0.3f);
                session.sendUpstreamPacket(playSoundPacket);
            }
            case PARTICLES_TRIAL_SPAWNER_SPAWN_ITEM -> effectPacket.setType(org.cloudburstmc.protocol.bedrock.data.LevelEvent.PARTICLE_TRIAL_SPAWNER_EJECTING);
            case SOUND_STOP_JUKEBOX_SONG -> {
                std::string bedrockSound = session.getWorldCache().removeActiveRecord(origin);
                if (bedrockSound == null) {

                    LevelSoundEventPacket levelSoundEvent = new LevelSoundEventPacket();
                    levelSoundEvent.setIdentifier("");
                    levelSoundEvent.setSound(SoundEvent.STOP_RECORD);
                    levelSoundEvent.setPosition(pos);
                    levelSoundEvent.setRelativeVolumeDisabled(false);
                    levelSoundEvent.setExtraData(-1);
                    levelSoundEvent.setBabySound(false);
                    session.sendUpstreamPacket(levelSoundEvent);
                } else {

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

    private static void spawnOminousTrialSpawnerParticles(GeyserSession session, Vector3f pos) {
        int dimensionId = DimensionUtils.javaToBedrock(session);
        SpawnParticleEffectPacket stringPacket = new SpawnParticleEffectPacket();
        stringPacket.setIdentifier("minecraft:trial_spawner_detection_ominous");
        stringPacket.setDimensionId(dimensionId);
        stringPacket.setPosition(pos.sub(0.5f, 0.75f, 0.5f));
        stringPacket.setMolangVariablesJson(Optional.empty());
        stringPacket.setUniqueEntityId(-1);
        session.sendUpstreamPacket(stringPacket);
    }
}
