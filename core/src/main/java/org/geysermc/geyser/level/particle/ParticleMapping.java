/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.particle;

import com.google.gson.JsonElement;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.BlockParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ColorParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.DustParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.GeyserBaseParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.GeyserParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ItemParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.SpellParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.TrailParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.VibrationParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.BlockPositionSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public sealed interface ParticleMapping {

    Optional<ParticleType> particleType();

    Function<Vector3f, BedrockPacket> mapParticle(GeyserSession session, @Nullable ParticleData particleData);

    record IdentifierBased(Key identifier) implements ParticleMapping {

        @Override
        public Optional<ParticleType> particleType() {
            try {
                // Most particle types should match
                return Optional.of(ParticleType.valueOf(identifier.value().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        @Override
        public Function<Vector3f, BedrockPacket> mapParticle(GeyserSession session, @Nullable ParticleData particleData) {
            return position -> ParticleMapping.createParticleEffectPacket(session, identifier.toString(), position);
        }
    }

    record LevelEventTypeBased(LevelEventType event) implements ParticleMapping {

        @Override
        public Optional<ParticleType> particleType() {
            if (event instanceof ParticleType type) {
                return Optional.of(type);
            }
            return Optional.empty();
        }

        @Override
        public Function<Vector3f, BedrockPacket> mapParticle(GeyserSession session, @Nullable ParticleData particleData) {
            return position -> {
                LevelEventPacket packet = new LevelEventPacket();
                packet.setType(event);
                packet.setPosition(position);
                return packet;
            };
        }
    }

    @FunctionalInterface
    non-sealed interface BuiltIn extends ParticleMapping {

        @Override
        default Optional<ParticleType> particleType() {
            return Optional.empty();
        }
    }

    static ParticleMapping ofIdentifier(String identifier) {
        return new IdentifierBased(MinecraftKey.key(identifier));
    }

    static ParticleMapping ofEvent(LevelEventType event) {
        return new LevelEventTypeBased(event);
    }

    static BuiltIn builtIn(org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType type) {
        return switch (type) {
            case BLOCK -> (session, data) -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((BlockParticleData) data).getBlockState());

                return position -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(LevelEvent.PARTICLE_CRACK_BLOCK);
                    packet.setPosition(position);
                    packet.setData(blockState);
                    return packet;
                };
            };
            case GEYSER, GEYSER_BASE, GEYSER_PLUME, GEYSER_POOF -> (session, particleData) -> {
                // This isn't a perfect translation - GEYSER_PLUME only displays the plume on Java, but does the full Geyser on bedrock, for example

                float waterBlocks = 0.0F;
                if (particleData instanceof GeyserBaseParticleData geyserParticleData) { // GEYSER_BASE, GEYSER_POOF
                    waterBlocks = geyserParticleData.waterBlocks();
                } else if (particleData instanceof GeyserParticleData(int blocks)) { // GEYSER, GEYSER_PLUME
                    waterBlocks = blocks;
                }

                // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GeyserPlumeParticle#L42-45
                float plumeHeight = 5.0F * Math.max(1.0F, waterBlocks);
                // Is in seconds
                float lifetime = waterBlocks * 5.0F / 20.0F;

                return position -> createParticleEffectPacket(session, geyserParticle(type), position, ParticleHelper.serializeVariables(List.of(
                    ParticleHelper.createVariable("variable.plume_height", ParticleHelper.FLOAT, plumeHeight),
                    ParticleHelper.createVariable("variable.active_time", ParticleHelper.FLOAT, lifetime)
                )));
            };
            // FIXME broken - uses the wrong colours, see https://github.com/GeyserMC/Geyser/issues/4803
            case FALLING_DUST -> (session, data) -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((BlockParticleData) data).getBlockState());

                return position -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(org.cloudburstmc.protocol.bedrock.data.ParticleType.FALLING_DUST);
                    packet.setData(blockState);
                    packet.setPosition(position);
                    return packet;
                };
            };
            case ITEM -> (session, particleData) -> {
                ItemStack javaItem = ((ItemParticleData) particleData).getItemStack();
                ItemData bedrockItem = ItemTranslator.translateToBedrock(session, javaItem);
                int data = bedrockItem.getDefinition().getRuntimeId() << 16 | bedrockItem.getDamage();

                return position -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(org.cloudburstmc.protocol.bedrock.data.ParticleType.ICON_CRACK);
                    packet.setData(data);
                    packet.setPosition(position);
                    return packet;
                };
            };
            // TODO transition particle, might not be possible on bedrock
            case DUST, DUST_COLOR_TRANSITION -> (session, data) -> {
                int rgbData = ((DustParticleData) data).getColor();

                return position -> {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(org.cloudburstmc.protocol.bedrock.data.ParticleType.FALLING_DUST);
                    packet.setData(rgbData);
                    packet.setPosition(position);
                    return packet;
                };
            };
            // power option might not be possible?
            // INSTANT_EFFECT doesn't exist on bedrock, EFFECT comes closest
            case EFFECT, INSTANT_EFFECT -> (session, data) -> position
                -> createParticleEffectPacket(session, "minecraft:mobspell_emitter", position, ParticleHelper.serializeColor(((SpellParticleData) data).getColor()));
            case VIBRATION -> (session, particleData) -> {
                VibrationParticleData data = (VibrationParticleData) particleData;

                Vector3f target;
                // As of Java 26.3, there are only 2 position sources: block and entity.
                // Entity is not allowed, see https://mcsrc.dev/2/26.3/net/minecraft/core/particles/VibrationParticleOption#L14-15
                if (data.getPositionSource() instanceof BlockPositionSource blockPositionSource) {
                    target = blockPositionSource.getPosition().toFloat().add(0.5f, 0.5f, 0.5f);
                } else {
                    throw new IllegalStateException("Received invalid position source for VIBRATION particle: " + data.getPositionSource().getType());
                }

                return position -> {
                    LevelEventGenericPacket packet = new LevelEventGenericPacket();
                    packet.setType(LevelEvent.PARTICLE_VIBRATION_SIGNAL);
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
            };
            case FIREWORK -> (session, data) -> position
                -> createParticleEffectPacket(session, "minecraft:sparkler_emitter", position, ParticleHelper.serializeColor(0xFFFFFF));
            case TINTED_LEAVES -> (session, data) -> position
                -> createParticleEffectPacket(session, "minecraft:biome_tinted_leaves_particle", position, ParticleHelper.serializeColor(((ColorParticleData) data).getColor()));
            case GLOW -> (session, data) -> {
                // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L88-92
                // TODO bedrock reports it wants xyz vars, but the particle shows anyways
                int colour = ThreadLocalRandom.current().nextBoolean() ? 0x99FFCC : 0x146666;
                return position -> createParticleEffectPacket(session, "minecraft:glow_particle", position, ParticleHelper.serializeColor(colour));
            };
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L188
            case WAX_ON -> (session, data) -> position
                -> createParticleEffectPacket(session, "minecraft:wax_particle", position, ParticleHelper.serializeColor(0xE88C14));
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L159
            case WAX_OFF -> (session, data) -> position
                -> createParticleEffectPacket(session, "minecraft:wax_particle", position, ParticleHelper.serializeColor(0xFFE5FF));
            case SCRAPE -> (session, data) -> {
                // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L125-129
                int colour = ThreadLocalRandom.current().nextBoolean() ? 0x499382 : 0x6DC49E;
                return position -> createParticleEffectPacket(session, "minecraft:wax_particle", position, ParticleHelper.serializeColor(colour));
            };
            case TRAIL -> (session, particleData) -> {
                TrailParticleData data = (TrailParticleData) particleData;
                Vector3f target = data.target().toFloat();
                int rgb = data.color();
                float lifetime = Math.max(data.duration(), 1) / 20f;

                return position -> {
                    Vector3f direction = target.sub(position);
                    float distance = direction.length();
                    Vector3f normalized = distance > 0 ? direction.div(distance) : Vector3f.ZERO;

                    return createParticleEffectPacket(session, "minecraft:creaking_heart_trail", position, ParticleHelper.serializeVariables(List.of(
                        ParticleHelper.createVariable("variable.direction", ParticleHelper.VECTOR3F, normalized),
                        ParticleHelper.createVariable("variable.color", ParticleHelper.RGB_COLOR, rgb),
                        ParticleHelper.createVariable("variable.max_lifetime", ParticleHelper.FLOAT, lifetime),
                        ParticleHelper.createVariable("variable.particle_initial_speed", ParticleHelper.FLOAT, distance / lifetime)
                    )));
                };
            };
            default -> throw new AssertionError("No built-in mapping exists for particle: " + type);
        };
    }

    private static int dimensionId(GeyserSession session) {
        return DimensionUtils.javaToBedrock(session);
    }

    private static SpawnParticleEffectPacket createParticleEffectPacket(GeyserSession session, String particle, Vector3f position) {
        return createParticleEffectPacket(session, particle, position, Optional.empty());
    }

    private static SpawnParticleEffectPacket createParticleEffectPacket(GeyserSession session, String particle, Vector3f position,
                                                                        JsonElement json) {
        return createParticleEffectPacket(session, particle, position, Optional.of(json.toString()));
    }

    private static SpawnParticleEffectPacket createParticleEffectPacket(GeyserSession session, String particle, Vector3f position,
                                                                        Optional<String> json) {
        SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
        packet.setIdentifier(particle);
        packet.setDimensionId(dimensionId(session));
        packet.setPosition(position);
        packet.setMolangVariablesJson(json);
        return packet;
    }

    private static String geyserParticle(org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType type) {
        return switch (type) {
            case GEYSER_BASE -> "minecraft:geyser_base";
            case GEYSER, GEYSER_PLUME -> "minecraft:geyser_plume";
            case GEYSER_POOF -> "minecraft:geyser_poof";
            default -> throw new AssertionError(type + " is not a geyser particle");
        };
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
