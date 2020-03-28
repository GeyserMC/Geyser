package org.geysermc.connector.utils;

import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public class SoundUtils {

    private static Map<String, String> soundMap = new HashMap<>();

    public static void setIdentifier(String javaSound, String bedrockSound) {
        soundMap.put(javaSound, bedrockSound);
    }

    public static String getIdentifier(@NonNull String javaSound) {
        return soundMap.getOrDefault(javaSound, "");
    }

    public static boolean hasIdentifier(@NonNull String javaSound) {
        return soundMap.containsKey(javaSound);
    }



}
