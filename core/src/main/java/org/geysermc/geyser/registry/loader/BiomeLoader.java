/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.geysermc.geyser.GeyserImpl;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class BiomeLoader implements RegistryLoader<String, BiomeDefinitions> {
    private final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Color.class, new ColorTypeAdapter())
        .create(); // temporary

    @Override
    public BiomeDefinitions load(String input) {
        Type type = new TypeToken<Map<String, BiomeDefinitionData>>() {}.getType();
        Map<String, BiomeDefinitionData> biomes;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input)) {
            biomes = GSON.fromJson(new InputStreamReader(stream), type);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Bedrock biomes!", e);
        }

        return new BiomeDefinitions(biomes);
    }

    public static class ColorTypeAdapter extends TypeAdapter<Color> {

        @Override
        public void write(JsonWriter out, Color color) throws IOException {
            if (color == null) {
                out.nullValue();
                return;
            }

            out.beginObject();
            out.name("r").value(color.getRed());
            out.name("g").value(color.getGreen());
            out.name("b").value(color.getBlue());
            out.name("a").value(color.getAlpha());
            out.endObject();
        }

        @Override
        public Color read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            int r = 0, g = 0, b = 0, a = 255;
            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "r": r = in.nextInt(); break;
                    case "g": g = in.nextInt(); break;
                    case "b": b = in.nextInt(); break;
                    case "a": a = in.nextInt(); break;
                    default: in.skipValue(); break;
                }
            }
            in.endObject();
            return new Color(r, g, b, a);
        }
    }
}
