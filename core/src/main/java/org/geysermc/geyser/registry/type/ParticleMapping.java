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

package org.geysermc.geyser.registry.type;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventGenericPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.particle.ParticleHelper;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.BlockParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ColorParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.DustParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.GeyserBaseParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.GeyserParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ItemParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.SpellParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.TrailParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.VibrationParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.BlockPositionSource;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.EntityPositionSource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@FunctionalInterface
public interface ParticleMapping {

    BedrockPacket createPacket(GeyserSession session, Vector3f position, @Nullable ParticleData particleData);

    static ParticleMapping ofIdentifier(String identifier) {
        return (session, position, data) -> {
            SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
            packet.setIdentifier(identifier);
            packet.setDimensionId(dimensionId(session));
            packet.setPosition(position);
            packet.setMolangVariablesJson(Optional.empty());
            return packet;
        };
    }

    static ParticleMapping ofEvent(LevelEventType event) {
        return new LevelEventTypeBased(event);
    }

    static ParticleMapping builtIn(ParticleType type) {
        return switch (type) {
            case BLOCK -> (session, position, data) -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((BlockParticleData) data).getBlockState());

                LevelEventPacket packet = new LevelEventPacket();
                packet.setType(LevelEvent.PARTICLE_CRACK_BLOCK);
                packet.setPosition(position);
                packet.setData(blockState);
                return packet;
            };
            case GEYSER, GEYSER_BASE, GEYSER_PLUME, GEYSER_POOF -> (session, position, particleData) -> {
                // This isn't a perfect translation - GEYSER_PLUME only displays the plume on Java, but does the full Geyser on bedrock, for example

                float waterBlocks = 0.0F;
                if (particleData instanceof GeyserBaseParticleData geyserParticleData) { // GEYSER_BASE, GEYSER_POOF
                    waterBlocks = geyserParticleData.waterBlocks();
                } else if (particleData instanceof GeyserParticleData(int blocks)) { // GEYSER, GEYSER_PLUME
                    waterBlocks = blocks;
                }

                // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GeyserPlumeParticle#L42-45
                float plumeHeight = 5.0F * Math.max(1.0F, waterBlocks);
                float lifetime = waterBlocks * 5.0F;

                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier(geyserParticle(type));
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                packet.setMolangVariablesJson(Optional.of(ParticleHelper.serializeVariables(List.of(
                    ParticleHelper.createVariable("variable.plume_height", ParticleHelper.FLOAT, plumeHeight),
                    ParticleHelper.createVariable("variable.active_time", ParticleHelper.FLOAT, lifetime)
                )).toString()));
                return packet;
            };
            case FALLING_DUST -> (session, position, data) -> {
                int blockState = session.getBlockMappings().getBedrockBlockId(((BlockParticleData) data).getBlockState());

                LevelEventPacket packet = new LevelEventPacket();
                // In fact, FallingDustParticle should have data like DustParticle,
                // but in MCProtocol, its data is BlockState(1).
                packet.setType(org.cloudburstmc.protocol.bedrock.data.ParticleType.FALLING_DUST);
                packet.setData(blockState);
                packet.setPosition(position);
                return packet;
            };
            case ITEM -> (session, position, particleData) -> {
                ItemStack javaItem = ((ItemParticleData) particleData).getItemStack();
                ItemData bedrockItem = ItemTranslator.translateToBedrock(session, javaItem);
                int data = bedrockItem.getDefinition().getRuntimeId() << 16 | bedrockItem.getDamage();

                LevelEventPacket packet = new LevelEventPacket();
                packet.setType(org.cloudburstmc.protocol.bedrock.data.ParticleType.ICON_CRACK);
                packet.setData(data);
                packet.setPosition(position);
                return packet;
            };
            case DUST, DUST_COLOR_TRANSITION -> (session, position, data) -> { // TODO
                int rgbData = ((DustParticleData) data).getColor();

                LevelEventPacket packet = new LevelEventPacket();
                packet.setType(org.cloudburstmc.protocol.bedrock.data.ParticleType.FALLING_DUST);
                packet.setData(rgbData);
                packet.setPosition(position);
                return packet;
            };
            case EFFECT, INSTANT_EFFECT -> (session, position, particleData) -> {
                SpellParticleData data = (SpellParticleData) particleData;

                // power option might not be possible?
                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:mobspell_emitter");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                packet.setMolangVariablesJson(Optional.of(colorMolang(data.getColor())));
                return packet;
            };
            case VIBRATION -> (session, position, particleData) -> {
                VibrationParticleData data = (VibrationParticleData) particleData;

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
            case FIREWORK -> (session, position, data) -> {
                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:sparkler_emitter");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                packet.setMolangVariablesJson(Optional.of("[{ \"name\": \"variable.color\", \"value\": { \"type\": \"member_array\", \"value\": [{\"name\": \".r\", \"value\": { \"type\": \"float\", \"value\": 1.0}},{\"name\": \".g\", \"value\": {\"type\": \"float\", \"value\": 1.0}},{\"name\": \".b\", \"value\": {\"type\": \"float\", \"value\": 1.0}},{\"name\": \".a\", \"value\": {\"type\": \"float\", \"value\": 1.0}}]}}]"));
                return packet;
            };
            case TINTED_LEAVES -> (session, position, particleData) -> {
                ColorParticleData data = (ColorParticleData) particleData;
                int rgb = data.getColor();

                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:biome_tinted_leaves_particle");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                packet.setMolangVariablesJson(Optional.of(colorMolang(rgb)));
                return packet;
            };
            case GLOW -> (session, position, data) -> {
                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:glow_particle");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                // The Java client randomly picks a light or dark cyan for each particle
                packet.setMolangVariablesJson(Optional.of(ThreadLocalRandom.current().nextBoolean()
                    ? colorMolang(0.6f, 1.0f, 0.8f)
                    : colorMolang(0.08f, 0.4f, 0.4f)));
                return packet;
            };
            case WAX_ON -> (session, position, data) -> {
                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:wax_particle");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                packet.setMolangVariablesJson(Optional.of(colorMolang(0.91f, 0.55f, 0.08f)));
                return packet;
            };
            case WAX_OFF -> (session, position, data) -> {
                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:wax_particle");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                packet.setMolangVariablesJson(Optional.of(colorMolang(1.0f, 0.9f, 1.0f)));
                return packet;
            };
            case SCRAPE -> (session, position, data) -> {
                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:wax_particle");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                // The Java client randomly picks a dark or light teal for each particle
                packet.setMolangVariablesJson(Optional.of(ThreadLocalRandom.current().nextBoolean()
                    ? colorMolang(0.29f, 0.58f, 0.51f)
                    : colorMolang(0.43f, 0.77f, 0.62f)));
                return packet;
            };
            case TRAIL -> (session, position, particleData) -> {
                TrailParticleData data = (TrailParticleData) particleData;
                Vector3f target = data.target().toFloat();
                int rgbData = data.color();
                float red = ((rgbData >> 16) & 0xFF) / 255f;
                float green = ((rgbData >> 8) & 0xFF) / 255f;
                float blue = (rgbData & 0xFF) / 255f;
                float lifetime = Math.max(data.duration(), 1) / 20f;

                Vector3f direction = target.sub(position);
                float distance = direction.length();
                Vector3f normalized = distance > 0 ? direction.div(distance) : Vector3f.ZERO;
                SpawnParticleEffectPacket packet = new SpawnParticleEffectPacket();
                packet.setIdentifier("minecraft:creaking_heart_trail");
                packet.setDimensionId(dimensionId(session));
                packet.setPosition(position);
                packet.setMolangVariablesJson(Optional.of("[{ \"name\": \"variable.direction\", \"value\": { \"type\": \"member_array\", \"value\": [{\"name\": \".x\", \"value\": { \"type\": \"float\", \"value\": " + normalized.getX() + "}},{\"name\": \".y\", \"value\": {\"type\": \"float\", \"value\": " + normalized.getY() + "}},{\"name\": \".z\", \"value\": {\"type\": \"float\", \"value\": " + normalized.getZ() + "}}]}},{ \"name\": \"variable.color\", \"value\": { \"type\": \"member_array\", \"value\": [{\"name\": \".r\", \"value\": { \"type\": \"float\", \"value\": " + red + "}},{\"name\": \".g\", \"value\": {\"type\": \"float\", \"value\": " + green + "}},{\"name\": \".b\", \"value\": {\"type\": \"float\", \"value\": " + blue + "}}]}},{ \"name\": \"variable.max_lifetime\", \"value\": { \"type\": \"float\", \"value\": " + lifetime + "}},{ \"name\": \"variable.particle_initial_speed\", \"value\": { \"type\": \"float\", \"value\": " + (distance / lifetime) + "}}]"));
                return packet;
            };
            default -> throw new AssertionError("No built-in mapping exists for particle: " + type);
        };
    }

    // Have to make a record impl for some shit bedrock does with AEC, see the code there
    record LevelEventTypeBased(LevelEventType event) implements ParticleMapping {

        @Override
        public BedrockPacket createPacket(GeyserSession session, Vector3f position, @Nullable ParticleData particleData) {
            LevelEventPacket packet = new LevelEventPacket();
            packet.setType(event);
            packet.setPosition(position);
            return packet;
        }
    }

    private static int dimensionId(GeyserSession session) {
        return DimensionUtils.javaToBedrock(session);
    }

    private static String geyserParticle(ParticleType type) {
        return switch (type) {
            case GEYSER_BASE -> "minecraft:geyser_base";
            case GEYSER_PLUME -> "minecraft:geyser_plume";
            case GEYSER_POOF -> "minecraft:geyser_poof";
            default -> throw new AssertionError(type + " is not a geyser particle");
        };
    }

    private static String colorMolang(int rgb) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        return colorMolang(red, green, blue);
    }

    private static String colorMolang(float red, float green, float blue) {
        return "[{ \"name\": \"variable.color\", \"value\": { \"type\": \"member_array\", \"value\": [{\"name\": \".r\", \"value\": { \"type\": \"float\", \"value\": " + red + "}},{\"name\": \".g\", \"value\": {\"type\": \"float\", \"value\": " + green + "}},{\"name\": \".b\", \"value\": {\"type\": \"float\", \"value\": " + blue + "}}]}}]";
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
