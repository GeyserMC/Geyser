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
#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEventType"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.registry.type.ParticleMapping"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType"

#include "java.util.Locale"
#include "java.util.Map"


public class ParticleTypesRegistryLoader extends EffectRegistryLoader<Map<ParticleType, ParticleMapping>> {

    override public Map<ParticleType, ParticleMapping> load(std::string input) {
        JsonObject particlesJson = this.loadFile(input);
        Map<ParticleType, ParticleMapping> particles = new Object2ObjectOpenHashMap<>();
        try {
            for (Map.Entry<std::string, JsonElement> entry : particlesJson.entrySet()) {
                std::string key = entry.getKey().toUpperCase(Locale.ROOT);
                JsonElement bedrockId = entry.getValue().getAsJsonObject().get("bedrockId");
                JsonElement eventType = entry.getValue().getAsJsonObject().get("eventType");
                if (eventType == null && bedrockId == null) {
                    GeyserImpl.getInstance().getLogger().debug("Skipping particle mapping " + key + " because no Bedrock equivalent exists.");
                    continue;
                }

                LevelEventType type = null;
                if (eventType != null) {
                    try {

                        type = org.cloudburstmc.protocol.bedrock.data.ParticleType.valueOf(eventType.getAsString().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {

                        type = LevelEvent.valueOf(eventType.getAsString().toUpperCase(Locale.ROOT));
                    }
                }

                particles.put(ParticleType.valueOf(key), new ParticleMapping(
                        type,
                        bedrockId == null ? null : bedrockId.getAsString())
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return particles;
    }
}
