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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.level.particle.*;
import com.github.steveice10.mc.protocol.data.game.level.particle.positionsource.BlockPositionSource;
import com.github.steveice10.mc.protocol.data.game.level.particle.positionsource.EntityPositionSource;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelParticlesPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.LevelEventGenericPacket;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ParticleMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.DimensionUtils;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Translator(packet = ClientboundLevelParticlesPacket.class)
public class JavaLevelParticlesTranslator extends PacketTranslator<ClientboundLevelParticlesPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLevelParticlesPacket packet) {
        Function<Vector3f, BedrockPacket> particleCreateFunction = createParticle(session, packet.getParticle());
        if (particleCreateFunction != null) {
            if (packet.getAmount() == 0) {
                // 0 means don't apply the offset
                Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
                session.sendUpstreamPacket(particleCreateFunction.apply(position));
            } else {
                Random random = ThreadLocalRandom.current();
                for (int i = 0; i < packet.getAmount(); i++) {
                    double offsetX = random.nextGaussian() * (double) packet.getOffsetX();
                    double offsetY = random.nextGaussian() * (double) packet.getOffsetY();
                    double offsetZ = random.nextGaussian() * (double) packet.getOffsetZ();
                    Vector3f position = Vector3f.from(packet.getX() + offsetX, packet.getY() + offsetY, packet.getZ() + offsetZ);

                    session.sendUpstreamPacket(particleCreateFunction.apply(position));
                }
            }
        } else {
            // Null is only returned when no particle of this type is found
            session.getGeyser().getLogger().debug("Unhandled particle packet: " + packet);
        }
    }

    /**
     * @param session the Bedrock client session.
     * @param particle the Java particle to translate to a Bedrock equivalent.
     * @return a function to create a packet with a specified particle, in the event we need to spawn multiple particles
     * with different offsets.
     */
    private Function<Vector3f, BedrockPacket> createParticle(GeyserSession session, Particle particle) {
        switch (particle.getType()) {
            case BLOCK -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((BlockParticleData) particle.getData()).getBlockState());
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(LevelEventType.PARTICLE_CRACK_BLOCK);
                    packet.setPosition(position);
                    packet.setData(blockState);
                    return packet;
                };
            }
            case FALLING_DUST -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((FallingDustParticleData) particle.getData()).getBlockState());
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    // In fact, FallingDustParticle should have data like DustParticle,
                    // but in MCProtocol, its data is BlockState(1).
                    packet.setType(LevelEventType.PARTICLE_FALLING_DUST);
                    packet.setData(blockState);
                    packet.setPosition(position);
                    return packet;
                };
            }
            case ITEM -> {
                ItemStack javaItem = ((ItemParticleData) particle.getData()).getItemStack();
                ItemData bedrockItem = ItemTranslator.translateToBedrock(session, javaItem);
                int data = bedrockItem.getId() << 16 | bedrockItem.getDamage();
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(LevelEventType.PARTICLE_ITEM_BREAK);
                    packet.setData(data);
                    packet.setPosition(position);
                    return packet;
                };
            }
            case DUST, DUST_COLOR_TRANSITION -> { //TODO
                DustParticleData data = (DustParticleData) particle.getData();
                int r = (int) (data.getRed() * 255);
                int g = (int) (data.getGreen() * 255);
                int b = (int) (data.getBlue() * 255);
                int rgbData = ((0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(LevelEventType.PARTICLE_FALLING_DUST);
                    packet.setData(rgbData);
                    packet.setPosition(position);
                    return packet;
                };
            }
            case VIBRATION -> {
                VibrationParticleData data = (VibrationParticleData) particle.getData();

                Vector3f target;
                if (data.getPositionSource() instanceof BlockPositionSource blockPositionSource) {
                    target = blockPositionSource.getPosition().toFloat().add(0.5f, 0.5f, 0.5f);
                } else if (data.getPositionSource() instanceof EntityPositionSource entityPositionSource) {
                    Entity entity = session.getEntityCache().getEntityByJavaId(entityPositionSource.getEntityId());
                    if (entity != null) {
                        target = entity.getPosition().up(entityPositionSource.getYOffset());
                    } else {
                        session.getGeyser().getLogger().debug("Unable to find entity with Java Id: " + entityPositionSource.getEntityId() + " for vibration particle.");
                        return null;
                    }
                } else {
                    session.getGeyser().getLogger().debug("Unknown position source " + data.getPositionSource() + " for vibration particle.");
                    return null;
                }

                return (position) -> {
                    LevelEventGenericPacket packet = new LevelEventGenericPacket();
                    packet.setEventId(2027/*LevelEventType.PARTICLE_VIBRATION_SIGNAL*/);
                    packet.setTag(
                            NbtMap.builder()
                                    .putCompound("origin", buildVec3PositionTag(position))
                                    .putCompound("target", buildVec3PositionTag(target)) // There is a way to target an entity but that takes an attachPos instead of a y offset
                                    .putFloat("speed", 20f)
                                    .putFloat("timeToLive", data.getArrivalTicks() / 20f)
                                    .build()
                    );
                    return packet;
                };
            }
            default -> {
                ParticleMapping particleMapping = Registries.PARTICLES.get(particle.getType());
                if (particleMapping == null) { //TODO ensure no particle can be null
                    return null;
                }

                if (particleMapping.levelEventType() != null) {
                    return (position) -> {
                        LevelEventPacket packet = new LevelEventPacket();
                        packet.setType(particleMapping.levelEventType());
                        packet.setPosition(position);
                        return packet;
                    };
                } else if (particleMapping.identifier() != null) {
                    int dimensionId = DimensionUtils.javaToBedrock(session.getDimension());
                    return (position) -> {
                        SpawnParticleEffectPacket stringPacket = new SpawnParticleEffectPacket();
                        stringPacket.setIdentifier(particleMapping.identifier());
                        stringPacket.setDimensionId(dimensionId);
                        stringPacket.setPosition(position);
                        stringPacket.setMolangVariablesJson(Optional.empty());
                        return stringPacket;
                    };
                } else {
                    return null;
                }
            }
        }
    }

    private NbtMap buildVec3PositionTag(Vector3f position) {
        return NbtMap.builder()
                .putString("type", "vec3")
                .putFloat("x", position.getX())
                .putFloat("y", position.getY())
                .putFloat("z", position.getZ())
                .build();
    }
}