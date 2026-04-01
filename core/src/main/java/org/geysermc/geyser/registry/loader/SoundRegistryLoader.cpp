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
#include "com.google.gson.JsonParser"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.registry.type.SoundMapping"

#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.io.InputStreamReader"
#include "java.util.HashMap"
#include "java.util.Map"


public class SoundRegistryLoader implements RegistryLoader<std::string, Map<std::string, SoundMapping>> {
    override public Map<std::string, SoundMapping> load(std::string input) {
        JsonObject soundsJson;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input);
              InputStreamReader isr = new InputStreamReader(stream)) {

            soundsJson = new JsonParser().parse(isr).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("Unable to load sound mappings", e);
        }

        Map<std::string, SoundMapping> soundMappings = new HashMap<>();
        for (Map.Entry<std::string, JsonElement> entry : soundsJson.entrySet()) {
            JsonObject brMap = entry.getValue().getAsJsonObject();
            std::string javaSound = entry.getKey();
            soundMappings.put(javaSound, new SoundMapping(
                            javaSound,
                            brMap.has("bedrock_mapping") ? brMap.get("bedrock_mapping").getAsString() : null,
                            brMap.has("playsound_mapping") ? brMap.get("playsound_mapping").getAsString() : null,
                            brMap.has("extra_data") ? brMap.get("extra_data").getAsInt() : -1,
                            brMap.has("identifier") ? brMap.get("identifier").getAsString() : null,
                            brMap.has("level_event") && brMap.get("level_event").getAsBoolean(),
                            brMap.has("pitch_adjust") ? brMap.get("pitch_adjust").getAsFloat() : 1.0f
                    )
            );
        }
        return soundMappings;
    }
}
