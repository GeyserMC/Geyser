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

package org.geysermc.connector.network.translators.sound;

import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.Data;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SoundRegistry {

    private static final Map<String, SoundMapping> SOUNDS;

    private SoundRegistry() {
    }

    public static void init() {
        // no-op
    }

    static {
        /* Load sound mappings */
        InputStream stream  = FileUtils.getResource("mappings/sounds.json");
        JsonNode soundsTree;
        try {
            soundsTree = GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (IOException e) {
            throw new AssertionError("Unable to load sound mappings", e);
        }

        Map<String, SoundMapping> soundMappings = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> soundsIterator = soundsTree.fields();
        while(soundsIterator.hasNext()) {
            Map.Entry<String, JsonNode> next = soundsIterator.next();
            JsonNode brMap = next.getValue();

            soundMappings.put(next.getKey(), new SoundMapping(
                            next.getKey(),
                            brMap.has("bedrock_mapping") && brMap.get("bedrock_mapping").isTextual() ? brMap.get("bedrock_mapping").asText() : null,
                            brMap.has("playsound_mapping") && brMap.get("playsound_mapping").isTextual() ? brMap.get("playsound_mapping").asText() : null,
                            brMap.has("extra_data") && brMap.get("extra_data").isInt() ? brMap.get("extra_data").asInt() : -1,
                            brMap.has("identifier") && brMap.get("identifier").isTextual() ? brMap.get("identifier").asText() : null,
                            brMap.has("level_event") && brMap.get("level_event").isBoolean() ? brMap.get("level_event").asBoolean() : false
                    )
            );
        }
        SOUNDS = soundMappings;
    }

    /**
     * Get's the sound mapping for a Java edition sound identifier
     * @param java Java edition sound identifier
     * @return SoundMapping object with information for bedrock, nukkit, java, etc. null if not found
     */
    public static SoundMapping fromJava(String java) {
        return SOUNDS.get(java);
    }

    /**
     * Maps a sound name to a sound event, null if one
     * does not exist.
     *
     * @param sound the sound name
     * @return a sound event from the given sound
     */
    public static SoundEvent toSoundEvent(String sound) {
        try {
            return SoundEvent.valueOf(sound.toUpperCase().replaceAll("\\.", "_"));
        } catch (Exception ex) {
            return null;
        }
    }

    @Data
    @ToString
    public static class SoundMapping {
        private final String java;
        private final String bedrock;
        private final String playsound;
        private final int extraData;
        private String identifier;
        private boolean levelEvent;

        public SoundMapping(String java, String bedrock, String playsound, int extraData, String identifier, boolean levelEvent) {
            this.java = java;
            this.bedrock = bedrock == null || bedrock.equalsIgnoreCase("") ? null : bedrock;
            this.playsound = playsound == null || playsound.equalsIgnoreCase("") ? null : playsound;
            this.extraData = extraData;
            this.identifier = identifier == null || identifier.equalsIgnoreCase("") ? ":" : identifier;
            this.levelEvent = levelEvent;
        }
    }
}
