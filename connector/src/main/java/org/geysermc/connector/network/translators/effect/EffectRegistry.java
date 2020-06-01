/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.effect;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Registry for particles and effects.
 */
public class EffectRegistry {

    public static final Map<String, Effect> EFFECTS = new HashMap<>();
    public static final Int2ObjectMap<SoundEvent> RECORDS = new Int2ObjectOpenHashMap<>();

    private static Map<ParticleType, LevelEventType> particleTypeMap = new HashMap<>();
    private static Map<ParticleType, String> particleStringMap = new HashMap<>();

    public static void init() {
        // no-op
    }

    static {
        /* Load particles */
        InputStream particleStream = FileUtils.getResource("mappings/particles.json");
        JsonNode particleEntries;
        try {
            particleEntries = GeyserConnector.JSON_MAPPER.readTree(particleStream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load particle map", e);
        }

        Iterator<Map.Entry<String, JsonNode>> particlesIterator = particleEntries.fields();
        while (particlesIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = particlesIterator.next();
            try {
                particleTypeMap.put(ParticleType.valueOf(entry.getKey().toUpperCase()), LevelEventType.valueOf(entry.getValue().asText().toUpperCase()));
            } catch (IllegalArgumentException e1) {
                try {
                    particleStringMap.put(ParticleType.valueOf(entry.getKey().toUpperCase()), entry.getValue().asText());
                    GeyserConnector.getInstance().getLogger().debug("Force to map particle "
                            + entry.getKey()
                            + "=>"
                            + entry.getValue().asText()
                            + ", it will take effect.");
                } catch (IllegalArgumentException e2){
                    GeyserConnector.getInstance().getLogger().warning(LanguageUtils.getLocaleStringLog("geyser.particle.failed_map", entry.getKey(), entry.getValue().asText()));
                }
            }
        }

        /* Load effects */
        InputStream effectsStream = FileUtils.getResource("mappings/effects.json");
        JsonNode effects;
        try {
            effects = GeyserConnector.JSON_MAPPER.readTree(effectsStream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load effects mappings", e);
        }

        Iterator<Map.Entry<String, JsonNode>> effectsIterator = effects.fields();
        while (effectsIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = effectsIterator.next();
            // Separate records database since they're handled differently between the two versions
            if (entry.getValue().has("records")) {
                JsonNode records = entry.getValue().get("records");
                Iterator<Map.Entry<String, JsonNode>> recordsIterator = records.fields();
                while (recordsIterator.hasNext()) {
                    Map.Entry<String, JsonNode> recordEntry = recordsIterator.next();
                    RECORDS.put(Integer.parseInt(recordEntry.getKey()), SoundEvent.valueOf(recordEntry.getValue().asText()));
                }
            }
            String identifier = (entry.getValue().has("identifier")) ? entry.getValue().get("identifier").asText() : "";
            int data = (entry.getValue().has("data")) ? entry.getValue().get("data").asInt() : -1;
            Effect effect = new Effect(entry.getKey(), entry.getValue().get("name").asText(), entry.getValue().get("type").asText(), data, identifier);
            EFFECTS.put(entry.getKey(), effect);
        }
    }

    public static LevelEventType getParticleLevelEventType(@NonNull ParticleType type) {
        return particleTypeMap.getOrDefault(type, null);
    }

    public static String getParticleString(@NonNull ParticleType type){
        return particleStringMap.getOrDefault(type, null);
    }
}
