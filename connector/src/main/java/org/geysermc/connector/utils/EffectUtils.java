package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import lombok.NonNull;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.effect.Effect;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EffectUtils {

    public static final Map<String, Effect> EFFECTS = new HashMap<>();
    public static final Int2ObjectMap<SoundEvent> RECORDS = new Int2ObjectOpenHashMap<>();

    private static Map<ParticleType, LevelEventType> particleTypeMap = new HashMap<>();
    private static Map<ParticleType, String> particleStringMap = new HashMap<>();

    public static void init() {
        // no-op
    }

    static {
        /* Load particles */
        InputStream particleStream = Toolbox.getResource("mappings/particles.json");
        JsonNode particleEntries;
        try {
            particleEntries = Toolbox.JSON_MAPPER.readTree(particleStream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load particle map", e);
        }

        Iterator<Map.Entry<String, JsonNode>> particlesIterator = particleEntries.fields();
        while (particlesIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = particlesIterator.next();
            try {
                setIdentifier(ParticleType.valueOf(entry.getKey().toUpperCase()), LevelEventType.valueOf(entry.getValue().asText().toUpperCase()));
            } catch (IllegalArgumentException e1) {
                try {
                    setIdentifier(ParticleType.valueOf(entry.getKey().toUpperCase()), entry.getValue().asText());
                    GeyserConnector.getInstance().getLogger().debug("Force to map particle "
                            + entry.getKey()
                            + "=>"
                            + entry.getValue().asText()
                            + ", it will take effect.");
                } catch (IllegalArgumentException e2){
                    GeyserConnector.getInstance().getLogger().warning("Fail to map particle " + entry.getKey() + "=>" + entry.getValue().asText());
                }
            }
        }

        /* Load effects */
        InputStream effectsStream = Toolbox.getResource("mappings/effects.json");
        JsonNode effects;
        try {
            effects = Toolbox.JSON_MAPPER.readTree(effectsStream);
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

    public static void setIdentifier(ParticleType type, LevelEventType identifier) {
        particleTypeMap.put(type, identifier);
    }

    public static void setIdentifier(ParticleType type, String identifier) {
        particleStringMap.put(type, identifier);
    }

    public static LevelEventType getParticleLevelEventType(@NonNull ParticleType type) {
        return particleTypeMap.getOrDefault(type, null);
    }

    public static String getParticleString(@NonNull ParticleType type){
        return particleStringMap.getOrDefault(type, null);
    }

}