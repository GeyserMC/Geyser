package org.geysermc.connector.utils;

import java.util.HashMap;
import java.util.Map;

import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;

import lombok.NonNull;

public class ParticleUtils {
    private static Map<ParticleType, String> particleMap = new HashMap<>();
    
    public static void setIdentifier(ParticleType type, String identifier) {
        particleMap.put(type, identifier);
    }
    
    public static String getIdentifier(@NonNull ParticleType type) {
        return particleMap.getOrDefault(type, "minecraft:water_evaporation_actor_emitter");
    }

    public static boolean hasIdentifier(@NonNull ParticleType type) {
        return particleMap.containsKey(type);
    }
    
}
