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

#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.BlockParticleData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.ColorParticleData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.DustParticleData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.ItemParticleData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.VibrationParticleData"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.BlockPositionSource"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.EntityPositionSource"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelParticlesPacket"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.data.ParticleType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.packet.BedrockPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ParticleMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.DimensionUtils"

#include "java.util.Optional"
#include "java.util.Random"
#include "java.util.concurrent.ThreadLocalRandom"
#include "java.util.function.Function"

@Translator(packet = ClientboundLevelParticlesPacket.class)
public class JavaLevelParticlesTranslator extends PacketTranslator<ClientboundLevelParticlesPacket> {
    private static final int MAX_PARTICLES = 100;

    override public void translate(GeyserSession session, ClientboundLevelParticlesPacket packet) {
        Function<Vector3f, BedrockPacket> particleCreateFunction = createParticle(session, packet.getParticle());
        if (particleCreateFunction != null) {
            if (packet.getAmount() == 0) {

                Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
                session.sendUpstreamPacket(particleCreateFunction.apply(position));
            } else {
                Random random = ThreadLocalRandom.current();
                int amount = Math.min(MAX_PARTICLES, packet.getAmount());
                for (int i = 0; i < amount; i++) {
                    double offsetX = random.nextGaussian() * (double) packet.getOffsetX();
                    double offsetY = random.nextGaussian() * (double) packet.getOffsetY();
                    double offsetZ = random.nextGaussian() * (double) packet.getOffsetZ();
                    Vector3f position = Vector3f.from(packet.getX() + offsetX, packet.getY() + offsetY, packet.getZ() + offsetZ);

                    session.sendUpstreamPacket(particleCreateFunction.apply(position));
                }
            }
        } else {

            session.getGeyser().getLogger().debug("Unhandled particle packet: " + packet);
        }
    }


    public static Function<Vector3f, BedrockPacket> createParticle(GeyserSession session, Particle particle) {
        switch (particle.getType()) {
            case BLOCK -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((BlockParticleData) particle.getData()).getBlockState());
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(LevelEvent.PARTICLE_CRACK_BLOCK);
                    packet.setPosition(position);
                    packet.setData(blockState);
                    return packet;
                };
            }
            case FALLING_DUST -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((BlockParticleData) particle.getData()).getBlockState());
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();


                    packet.setType(ParticleType.FALLING_DUST);
                    packet.setData(blockState);
                    packet.setPosition(position);
                    return packet;
                };
            }
            case ITEM -> {
                ItemStack javaItem = ((ItemParticleData) particle.getData()).getItemStack();
                ItemData bedrockItem = ItemTranslator.translateToBedrock(session, javaItem);
                int data = bedrockItem.getDefinition().getRuntimeId() << 16 | bedrockItem.getDamage();
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(ParticleType.ICON_CRACK);
                    packet.setData(data);
                    packet.setPosition(position);
                    return packet;
                };
            }
            case DUST, DUST_COLOR_TRANSITION -> { //TODO
                DustParticleData data = (DustParticleData) particle.getData();
                int rgbData = data.getColor();
                return (position) -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(ParticleType.FALLING_DUST);
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
                        target = entity.bedrockPosition().up(entityPositionSource.getYOffset());
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
                    packet.setType(LevelEvent.PARTICLE_VIBRATION_SIGNAL);
                    packet.setTag(
                            NbtMap.builder()
                                    .putCompound("origin", buildVec3PositionTag(position))
                                    .putCompound("target", buildVec3PositionTag(target))
                                    .putFloat("speed", 20f)
                                    .putFloat("timeToLive", data.getArrivalTicks() / 20f)
                                    .build()
                    );
                    return packet;
                };
            }
            case FIREWORK -> {
                int dimensionId = DimensionUtils.javaToBedrock(session);
                return (position) -> {
                    SpawnParticleEffectPacket particlePacket = new SpawnParticleEffectPacket();
                    particlePacket.setIdentifier("minecraft:sparkler_emitter");
                    particlePacket.setDimensionId(dimensionId);
                    particlePacket.setPosition(position);
                    particlePacket.setMolangVariablesJson(Optional.of("[{ \"name\": \"variable.color\", \"value\": { \"type\": \"member_array\", \"value\": [{\"name\": \".r\", \"value\": { \"type\": \"float\", \"value\": 1.0}},{\"name\": \".g\", \"value\": {\"type\": \"float\", \"value\": 1.0}},{\"name\": \".b\", \"value\": {\"type\": \"float\", \"value\": 1.0}},{\"name\": \".a\", \"value\": {\"type\": \"float\", \"value\": 1.0}}]}}]"));
                    return particlePacket;
                };
            }
            case TINTED_LEAVES -> {
                int dimensionId = DimensionUtils.javaToBedrock(session);
                ColorParticleData data = (ColorParticleData) particle.getData();
                int rgbData = data.getColor();
                float red = ((rgbData >> 16) & 0xFF) / 255f;
                float green = ((rgbData >> 8) & 0xFF) / 255f;
                float blue = (rgbData & 0xFF) / 255f;
                return (position) -> {
                    SpawnParticleEffectPacket particlePacket = new SpawnParticleEffectPacket();
                    particlePacket.setIdentifier("minecraft:biome_tinted_leaves_particle");
                    particlePacket.setDimensionId(dimensionId);
                    particlePacket.setPosition(position);
                    particlePacket.setMolangVariablesJson(Optional.of("[{ \"name\": \"variable.color\", \"value\": { \"type\": \"member_array\", \"value\": [{\"name\": \".r\", \"value\": { \"type\": \"float\", \"value\": " + red + "}},{\"name\": \".g\", \"value\": {\"type\": \"float\", \"value\": " + green + "}},{\"name\": \".b\", \"value\": {\"type\": \"float\", \"value\": " + blue + "}}]}}]"));
                    return particlePacket;
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
                    int dimensionId = DimensionUtils.javaToBedrock(session);
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

    private static NbtMap buildVec3PositionTag(Vector3f position) {
        return NbtMap.builder()
                .putString("type", "vec3")
                .putFloat("x", position.getX())
                .putFloat("y", position.getY())
                .putFloat("z", position.getZ())
                .build();
    }
}
