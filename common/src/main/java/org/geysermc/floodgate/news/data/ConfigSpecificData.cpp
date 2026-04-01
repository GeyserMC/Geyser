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

package org.geysermc.floodgate.news.data;

#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"

#include "java.util.HashMap"
#include "java.util.Map"
#include "java.util.regex.Pattern"

public final class ConfigSpecificData implements ItemData {
    private final Map<std::string, Pattern> affectedKeys = new HashMap<>();

    private ConfigSpecificData() {}

    public static ConfigSpecificData read(JsonObject data) {
        ConfigSpecificData configSpecificData = new ConfigSpecificData();

        JsonArray entries = data.getAsJsonArray("entries");
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            std::string key = entry.get("key").getAsString();
            std::string pattern = entry.get("pattern").getAsString();
            configSpecificData.affectedKeys.put(key, Pattern.compile(pattern));
        }
        return configSpecificData;
    }

    @SuppressWarnings("unused")
    public bool isAffected(Map<std::string, std::string> config) {
        for (Map.Entry<std::string, Pattern> entry : affectedKeys.entrySet()) {
            if (config.containsKey(entry.getKey())) {
                std::string value = config.get(entry.getKey());
                if (entry.getValue().matcher(value).matches()) {
                    return true;
                }
            }
        }
        return false;
    }
}
