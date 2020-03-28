package org.geysermc.connector.sound;

import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.Data;
import lombok.ToString;
import org.geysermc.connector.utils.Toolbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class SoundMap {

    private static SoundMap instance;

    public static SoundMap get() {
        if(instance == null) {
            instance = new SoundMap(make());
        }
        return instance;
    }

    private static ArrayList<SoundMapping> make() {
        /* Load sound mappings */
        InputStream stream  = Toolbox.getResource("mappings/sounds.json");
        JsonNode soundsTree;
        try {
            soundsTree = Toolbox.JSON_MAPPER.readTree(stream);
        } catch (IOException e) {
            throw new AssertionError("Unable to load sound mappings", e);
        }

        ArrayList<SoundMapping> soundMappings = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> soundsIterator = soundsTree.fields();
        while(soundsIterator.hasNext()) {
            Map.Entry<String, JsonNode> next = soundsIterator.next();
            JsonNode brMap = next.getValue();

            soundMappings.add(
                    new SoundMapping(
                            next.getKey(),
                            brMap.has("bedrock_mapping") && brMap.get("bedrock_mapping").isTextual() ? brMap.get("bedrock_mapping").asText() : null,
                            brMap.has("playsound_mapping") && brMap.get("playsound_mapping").isTextual() ? brMap.get("playsound_mapping").asText() : null
                    )
            );
        }


        return soundMappings;
    }

    /**
     * Where:
     *
     * key: Bedrock sound
     * value: Java sound
     */
    private ArrayList<SoundMapping> sounds;

    public SoundMap(ArrayList<SoundMapping> sounds) {
        this.sounds = sounds;
    }

    /**
     * Get's the sound mapping for a Java edition sound identifier
     * @param java Java edition sound identifier
     * @return SoundMapping object with information for bedrock, nukkit, java, etc. null if not found
     */
    public SoundMapping fromJava(String java) {
        for (SoundMapping sound : this.sounds) {
            if(sound.getJava().equals(java)) {
                return sound;
            }
        }
        return null;
    }

    /**
     * Get's the sound mapping for a Bedrock edition sound identifier
     * @param bedrock Bedrock edition sound identifier
     * @return SoundMapping object with information for bedrock, nukkit, java, etc. null if not found
     */
    public SoundMapping fromBedrock(String bedrock) {
        for (SoundMapping sound : this.sounds) {
            if(sound.getJava().equals(bedrock)) {
                return sound;
            }
        }
        return null;
    }

    /**
     * Get's the sound mapping for a Nukkit sound identifier
     * @param nukkit Nukkit sound identifier
     * @return SoundMapping object with information for bedrock, nukkit, java, etc. null if not found
     */
    public SoundMapping fromNukkit(String nukkit) {
        for (SoundMapping sound : this.sounds) {
            if(sound.getJava().equals(nukkit)) {
                return sound;
            }
        }
        return null;
    }



    public void refresh() {
        this.sounds = make();
    }

    //

    public static SoundEvent toSoundEvent(String s) {
        SoundEvent sound;
        try {
            sound = SoundEvent.valueOf(
                    s
                            .toUpperCase()
                            .replaceAll("\\.", "_")
            );
            return sound;
        } catch(Exception e) {
            return null;
        }
    }

    public static boolean validateVolume(float f) {
        return (f >= 0.0F && f <= 1.0F);
    }

    public static boolean validatePitch(float f) {
        return (f >= 0.5F && f <= 2.0F);
    }

    @Data
    @ToString
    public static class SoundMapping {
        private final String java;
        private final String bedrock;
        private final String playsound;

        public SoundMapping(String java, String bedrock, String playsound) {
            this.java = java;
            this.bedrock = bedrock == null || bedrock.equalsIgnoreCase("") ? null : bedrock;
            this.playsound = playsound == null || playsound.equalsIgnoreCase("") ? null : playsound;
        }

        public boolean hasBedrock() {
            return this.bedrock != null;
        }

        public boolean hasPlaysound() {
            return this.playsound != null;
        }

        public boolean hasJava() {
            return this.java != null;
        }
    }

}
