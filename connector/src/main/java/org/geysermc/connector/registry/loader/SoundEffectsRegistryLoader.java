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

package org.geysermc.connector.registry.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.protocol.data.game.world.effect.SoundEffect;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.effect.Effect;
import org.geysermc.connector.network.translators.effect.PlaySoundEffect;
import org.geysermc.connector.network.translators.effect.SoundEventEffect;
import org.geysermc.connector.network.translators.effect.SoundLevelEffect;

import java.util.Iterator;
import java.util.Map;

public class SoundEffectsRegistryLoader extends EffectRegistryLoader<Map<SoundEffect, Effect>> {

    @Override
    public Map<SoundEffect, Effect> load(String input) {
        this.loadFile(input);

        Iterator<Map.Entry<String, JsonNode>> effectsIterator = this.get(input).fields();
        Map<SoundEffect, Effect> soundEffects = new Object2ObjectOpenHashMap<>();
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
                        boolean pitchSub = node.has("pitch_sub") && node.get("pitch_sub").booleanValue();
                        float pitchMul = node.has("pitch_mul") ? node.get("pitch_mul").floatValue() : 1.0f;
                        float pitchAdd = node.has("pitch_add") ? node.get("pitch_add").floatValue() : 0.0f;
                        boolean relative = !node.has("relative") || node.get("relative").booleanValue();
                        effect = new PlaySoundEffect(name, volume, pitchSub, pitchMul, pitchAdd, relative);
                        break;
                    }
                }
                if (javaEffect != null) {
                    soundEffects.put(javaEffect, effect);
                }
            } catch (Exception e) {
                GeyserConnector.getInstance().getLogger().warning("Failed to map sound effect " + entry.getKey() + " : " + e.toString());
            }
        }
        return soundEffects;
    }
}