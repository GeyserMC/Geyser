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
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.effect.Effect;
import org.geysermc.connector.utils.EffectUtils;

@Translator(packet = ServerPlayEffectPacket.class)
public class JavaPlayEffectTranslator extends PacketTranslator<ServerPlayEffectPacket> {

    @Override
    public void translate(ServerPlayEffectPacket packet, GeyserSession session) {
        LevelEventPacket effect = new LevelEventPacket();
        // Some things here are particles, others are not
        if (packet.getEffect() instanceof ParticleEffect) {
            ParticleEffect particleEffect = (ParticleEffect) packet.getEffect();
            Effect geyserEffect = EffectUtils.EFFECTS.get(particleEffect.name());
            if (geyserEffect != null) {
                String name = geyserEffect.getBedrockName();
                effect.setType(LevelEventType.valueOf(name));
            } else {
                switch (particleEffect) {
                    // TODO: BREAK_SPLASH_POTION has additional data
                    // TODO: Block break doesn't work when you're the player.
                    case BONEMEAL_GROW:
                        effect.setType(LevelEventType.BONEMEAL);
                        BonemealGrowEffectData growEffectData = (BonemealGrowEffectData) packet.getData();
                        effect.setData(growEffectData.getParticleCount());
                        break;
                    //TODO: Block break particles when under fire
                    case BREAK_BLOCK:
                        effect.setType(LevelEventType.DESTROY);
                        BreakBlockEffectData breakBlockEffectData = (BreakBlockEffectData) packet.getData();
                        effect.setData(BlockTranslator.getBedrockBlockId(breakBlockEffectData.getBlockState()));
                        break;
                    // TODO: Check these three
                    case EXPLOSION:
                        effect.setType(LevelEventType.PARTICLE_EXPLODE);
                        break;
                    case MOB_SPAWN:
                        effect.setType(LevelEventType.ENTITY_SPAWN);
                        break;
                        // Done with a dispenser
                    case SMOKE:
                        // Might need to be SHOOT
                        effect.setType(LevelEventType.PARTICLE_SMOKE);
                        break;
                    default:
                        GeyserConnector.getInstance().getLogger().debug("No effect handling for particle effect: " + packet.getEffect());
                }
            }
            effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.getUpstream().sendPacket(effect);
        } else if (packet.getEffect() instanceof SoundEffect) {
            SoundEffect soundEffect = (SoundEffect) packet.getEffect();
            Effect geyserEffect = EffectUtils.EFFECTS.get(soundEffect.name());
            if (geyserEffect != null) {
                // Some events are LevelEventTypes, some are SoundEvents.
                if (geyserEffect.getType().equals("soundLevel")) {
                    // TODO: Opening doors also does not work as the player
                    effect.setType(LevelEventType.valueOf(geyserEffect.getBedrockName()));
                } else if (geyserEffect.getType().equals("soundEvent")) {
                    LevelSoundEventPacket soundEvent = new LevelSoundEventPacket();
                    // Separate case since each RecordEffectData in Java is an individual track in Bedrock
                    if (geyserEffect.getJavaName().equals("RECORD")) {
                        RecordEffectData recordEffectData = (RecordEffectData) packet.getData();
                        soundEvent.setSound(EffectUtils.RECORDS.get(recordEffectData.getRecordId()));
                    } else {
                        soundEvent.setSound(SoundEvent.valueOf(geyserEffect.getBedrockName()));
                    }
                    soundEvent.setExtraData(geyserEffect.getData());
                    soundEvent.setIdentifier(geyserEffect.getIdentifier());
                    soundEvent.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
                    session.getUpstream().sendPacket(soundEvent);
                }
            } else {
                GeyserConnector.getInstance().getLogger().debug("No effect handling for sound effect: " + packet.getEffect());
            }
        }
        if (effect.getType() != null) {
            effect.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            session.getUpstream().sendPacket(effect);
        }

    }
}