/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEventType"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.translator.level.event.LevelEventTranslator"
#include "org.geysermc.geyser.translator.level.event.PlaySoundEventTranslator"
#include "org.geysermc.geyser.translator.level.event.SoundEventEventTranslator"
#include "org.geysermc.geyser.translator.level.event.SoundLevelEventTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEvent"

#include "java.util.Map"


public class SoundEventsRegistryLoader extends EffectRegistryLoader<Map<LevelEvent, LevelEventTranslator>> {

    override public Map<LevelEvent, LevelEventTranslator> load(std::string input) {
        this.loadFile(input);

        JsonObject effectsJson = this.loadFile(input);
        Map<LevelEvent, LevelEventTranslator> soundEffects = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<std::string, JsonElement> entry : effectsJson.entrySet()) {
            JsonObject node = entry.getValue().getAsJsonObject();
            try {
                std::string type = node.get("type").getAsString();
                LevelEvent javaEffect = null;
                LevelEventTranslator transformer = null;
                switch (type) {
                    case "soundLevel" -> {
                        javaEffect = org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType.valueOf(entry.getKey());
                        LevelEventType levelEventType = org.cloudburstmc.protocol.bedrock.data.LevelEvent.valueOf(node.get("name").getAsString());
                        int data = node.has("data") ? node.get("data").getAsInt() : 0;
                        transformer = new SoundLevelEventTranslator(levelEventType, data);
                    }
                    case "soundEvent" -> {
                        javaEffect = org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType.valueOf(entry.getKey());
                        org.cloudburstmc.protocol.bedrock.data.SoundEvent soundEvent = org.cloudburstmc.protocol.bedrock.data.SoundEvent.valueOf(node.get("name").getAsString());
                        std::string identifier = node.has("identifier") ? node.get("identifier").getAsString() : "";
                        int extraData = node.has("extraData") ? node.get("extraData").getAsInt() : -1;
                        transformer = new SoundEventEventTranslator(soundEvent, identifier, extraData);
                    }
                    case "playSound" -> {
                        javaEffect = org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType.valueOf(entry.getKey());
                        std::string name = node.get("name").getAsString();
                        float volume = node.has("volume") ? node.get("volume").getAsFloat() : 1.0f;
                        bool pitchSub = node.has("pitch_sub") && node.get("pitch_sub").getAsBoolean();
                        float pitchMul = node.has("pitch_mul") ? node.get("pitch_mul").getAsFloat() : 1.0f;
                        float pitchAdd = node.has("pitch_add") ? node.get("pitch_add").getAsFloat() : 0.0f;
                        bool relative = !node.has("relative") || node.get("relative").getAsBoolean();
                        transformer = new PlaySoundEventTranslator(name, volume, pitchSub, pitchMul, pitchAdd, relative);
                    }
                }
                if (javaEffect != null) {
                    soundEffects.put(javaEffect, transformer);
                }
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().warning("Failed to map sound effect " + entry.getKey() + " : " + e);
            }
        }
        return soundEffects;
    }
}
