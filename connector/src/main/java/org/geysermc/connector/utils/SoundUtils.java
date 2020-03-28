package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.world.sound.CustomSound;
import com.github.steveice10.mc.protocol.data.game.world.sound.Sound;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public class SoundUtils {

    private static Map<BuiltinSound, String> soundMap = new HashMap<>();

    public static void setIdentifier(BuiltinSound javaSound, String bedrockSound) {
        soundMap.put(javaSound, bedrockSound);
    }

    public static String getIdentifier(@NonNull BuiltinSound javaSound){
        return soundMap.getOrDefault(javaSound, null);
    }

    public static String getIdentifier(@NonNull Sound javaSound) {
        if(javaSound instanceof BuiltinSound){
            return getIdentifier((BuiltinSound) javaSound);
        }else if(javaSound instanceof CustomSound){
            return ((CustomSound) javaSound).getName();
        }
        return null;
    }

}