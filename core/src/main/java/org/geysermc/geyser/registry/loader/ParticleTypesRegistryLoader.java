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

package org.geysermc.geyser.registry.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.particle.ParticleMapping;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;

import java.util.Map;

/**
 * Loads particle types from the given resource path.
 */
public class ParticleTypesRegistryLoader extends EffectRegistryLoader<Map<ParticleType, ParticleMapping>> {

    @Override
    public Map<ParticleType, ParticleMapping> load(String input) {
        JsonObject particlesJson = this.loadFile(input);
        Map<ParticleType, ParticleMapping> particles = new Object2ObjectOpenHashMap<>();
        try {
            for (Map.Entry<String, JsonElement> entry : particlesJson.entrySet()) {
                ParticleType javaType = ParticleType.from(MinecraftKey.key(entry.getKey()));
                if (javaType == null) {
                    throw new AssertionError("Unknown particle type in particle mappings: " + entry.getKey());
                }

                particles.put(javaType, parseParticleMapping(javaType, entry.getValue().getAsJsonObject()));
            }
        } catch (RuntimeException exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to load particle mappings!", exception);
            throw exception;
        }
        return particles;
    }

    private static ParticleMapping parseParticleMapping(ParticleType type, JsonObject mapping) {
        JsonElement bedrockId = mapping.get("bedrock_id");
        if (bedrockId != null) {
            return ParticleMapping.ofIdentifier(bedrockId.getAsString());
        }
        JsonElement particleType = mapping.get("particle_type");
        if (particleType != null) {
            try {
                return ParticleMapping.ofEvent(org.cloudburstmc.protocol.bedrock.data.ParticleType.valueOf(particleType.getAsString()));
            } catch (IllegalArgumentException exception) {
                throw new AssertionError("Unknown bedrock particle type in particle mappings: " + particleType.getAsString(), exception);
            }
        }
        JsonElement levelEvent = mapping.get("level_event");
        if (levelEvent != null) {
            try {
                return ParticleMapping.ofEvent(LevelEvent.valueOf(levelEvent.getAsString()));
            } catch (IllegalArgumentException exception) {
                throw new AssertionError("Unknown level event in particle mappings: " + levelEvent.getAsString(), exception);
            }
        }
        JsonElement builtIn = mapping.get("built_in");
        if (builtIn != null) {
            return ParticleMapping.builtIn(type);
        }
        throw new AssertionError("Don't know how to parse particle mapping for " + type + ": " + mapping);
    }
}
