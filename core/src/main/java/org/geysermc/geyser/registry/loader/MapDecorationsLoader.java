/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import com.google.gson.JsonObject;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.BedrockMapIcon;
import org.geysermc.geyser.util.JsonUtils;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.level.map.MapIconType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class MapDecorationsLoader implements RegistryLoader<String, Map<MapIconType, BedrockMapIcon>> {

    @Override
    public Map<MapIconType, BedrockMapIcon> load(String input) {
        JsonObject rawMappings;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input)) {
            rawMappings = JsonUtils.fromJson(stream);
        } catch (IOException exception) {
            throw new AssertionError("Failed to load map decoration mappings!", exception);
        }

        Map<MapIconType, BedrockMapIcon> mappings = new EnumMap<>(MapIconType.class);

        for (String key : rawMappings.keySet()) {
            MapIconType javaType = MapIconType.from(MinecraftKey.key(key));
            if (javaType == null) {
                throw new AssertionError("Unknown map decoration " + javaType + " in map decoration mappings!");
            }
            mappings.put(javaType, BedrockMapIcon.parse(rawMappings.get(key)));
        }
        return Collections.unmodifiableMap(mappings);
    }
}
