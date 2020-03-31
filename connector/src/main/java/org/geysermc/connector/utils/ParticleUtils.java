package org.geysermc.connector.utils;

import java.util.HashMap;
import java.util.Map;

import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;

import com.nukkitx.protocol.bedrock.data.LevelEventType;
import lombok.NonNull;

public class ParticleUtils {
    private static Map<ParticleType, LevelEventType> particleTypeMap = new HashMap<>();
    private static Map<ParticleType, String> particleStringMap = new HashMap<>();

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
