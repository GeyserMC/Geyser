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

package org.geysermc.connector.network.translators.effect;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.world.effect.SoundEffect;
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

    public static final Map<SoundEffect, Effect> SOUND_EFFECTS = new HashMap<>();
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
            JsonNode node = entry.getValue();
            try {
                String type = node.get("type").asText();
                SoundEffect javaEffect = null;
                Effect effect = null;
                switch (type) {
                    case "soundLevel": {
                        javaEffect = SoundEffect.valueOf(entry.getKey());
                        LevelEventType levelEventType = LevelEventType.valueOf(node.get("name").asText());
                        int data = node.has("data") ? node.get("data").intValue() : 0;
                        effect = new SoundLevelEffect(levelEventType, data);
                        break;
                    }
                    case "soundEvent": {
                        javaEffect = SoundEffect.valueOf(entry.getKey());
                        SoundEvent soundEvent = SoundEvent.valueOf(node.get("name").asText());
                        String identifier = node.has("identifier") ? node.get("identifier").asText() : "";
                        int extraData = node.has("extraData") ? node.get("extraData").intValue() : -1;
                        effect = new SoundEventEffect(soundEvent, identifier, extraData);
                        break;
                    }
                    case "playSound": {
                        javaEffect = SoundEffect.valueOf(entry.getKey());
                        String name = node.get("name").asText();
                        float volume = node.has("volume") ? node.get("volume").floatValue() : 1.0f;
                        boolean pitchSub = node.has("pitch_sub") ? node.get("pitch_sub").booleanValue() : false;
                        float pitchMul = node.has("pitch_mul") ? node.get("pitch_mul").floatValue() : 1.0f;
                        float pitchAdd = node.has("pitch_add") ? node.get("pitch_add").floatValue() : 0.0f;
                        boolean relative = node.has("relative") ? node.get("relative").booleanValue() : true;
                        effect = new PlaySoundEffect(name, volume, pitchSub, pitchMul, pitchAdd, relative);
                        break;
                    }
                    case "record": {
                        JsonNode records = entry.getValue().get("records");
                        Iterator<Map.Entry<String, JsonNode>> recordsIterator = records.fields();
                        while (recordsIterator.hasNext()) {
                            Map.Entry<String, JsonNode> recordEntry = recordsIterator.next();
                            RECORDS.put(Integer.parseInt(recordEntry.getKey()), SoundEvent.valueOf(recordEntry.getValue().asText()));
                        }
                        break;
                    }
                }
                if (javaEffect != null) {
                    SOUND_EFFECTS.put(javaEffect, effect);
                }
            } catch (Exception e) {
                GeyserConnector.getInstance().getLogger().warning("Failed to map sound effect " + entry.getKey() + " : " + e.toString());
            }
        }
    }

    public static LevelEventType getParticleLevelEventType(@NonNull ParticleType type) {
        return particleTypeMap.getOrDefault(type, null);
    }

    public static String getParticleString(@NonNull ParticleType type){
        return particleStringMap.getOrDefault(type, null);
    }
}
