/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.level.event.SoundEvent;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.translator.level.event.LevelEventTranslator;
import org.geysermc.geyser.translator.level.event.PlaySoundEventTranslator;
import org.geysermc.geyser.translator.level.event.SoundEventEventTranslator;
import org.geysermc.geyser.translator.level.event.SoundLevelEventTranslator;

import java.util.Iterator;
import java.util.Map;

/**
 * Loads sound effects from the given resource path.
 */
public class SoundEventsRegistryLoader extends EffectRegistryLoader<Map<SoundEvent, LevelEventTranslator>> {

    @Override
    public Map<SoundEvent, LevelEventTranslator> load(String input) {
        this.loadFile(input);

        Iterator<Map.Entry<String, JsonNode>> effectsIterator = this.get(input).fields();
        Map<SoundEvent, LevelEventTranslator> soundEffects = new Object2ObjectOpenHashMap<>();
        while (effectsIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = effectsIterator.next();
            JsonNode node = entry.getValue();
            try {
                String type = node.get("type").asText();
                SoundEvent javaEffect = null;
                LevelEventTranslator transformer = null;
                switch (type) {
                    case "soundLevel" -> {
                        javaEffect = SoundEvent.valueOf(entry.getKey());
                        LevelEventType levelEventType = LevelEventType.valueOf(node.get("name").asText());
                        int data = node.has("data") ? node.get("data").intValue() : 0;
                        transformer = new SoundLevelEventTranslator(levelEventType, data);
                    }
                    case "soundEvent" -> {
                        javaEffect = SoundEvent.valueOf(entry.getKey());
                        com.nukkitx.protocol.bedrock.data.SoundEvent soundEvent = com.nukkitx.protocol.bedrock.data.SoundEvent.valueOf(node.get("name").asText());
                        String identifier = node.has("identifier") ? node.get("identifier").asText() : "";
                        int extraData = node.has("extraData") ? node.get("extraData").intValue() : -1;
                        transformer = new SoundEventEventTranslator(soundEvent, identifier, extraData);
                    }
                    case "playSound" -> {
                        javaEffect = SoundEvent.valueOf(entry.getKey());
                        String name = node.get("name").asText();
                        float volume = node.has("volume") ? node.get("volume").floatValue() : 1.0f;
                        boolean pitchSub = node.has("pitch_sub") && node.get("pitch_sub").booleanValue();
                        float pitchMul = node.has("pitch_mul") ? node.get("pitch_mul").floatValue() : 1.0f;
                        float pitchAdd = node.has("pitch_add") ? node.get("pitch_add").floatValue() : 0.0f;
                        boolean relative = !node.has("relative") || node.get("relative").booleanValue();
                        transformer = new PlaySoundEventTranslator(name, volume, pitchSub, pitchMul, pitchAdd, relative);
                    }
                }
                if (javaEffect != null) {
                    soundEffects.put(javaEffect, transformer);
                }
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().warning("Failed to map sound effect " + entry.getKey() + " : " + e.toString());
            }
        }
        return soundEffects;
    }
}