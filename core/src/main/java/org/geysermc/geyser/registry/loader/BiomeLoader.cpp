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

#include "com.google.gson.Gson"
#include "com.google.gson.GsonBuilder"
#include "com.google.gson.TypeAdapter"
#include "com.google.gson.reflect.TypeToken"
#include "com.google.gson.stream.JsonReader"
#include "com.google.gson.stream.JsonToken"
#include "com.google.gson.stream.JsonWriter"
#include "org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData"
#include "org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions"
#include "org.geysermc.geyser.GeyserImpl"

#include "java.awt.*"
#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.io.InputStreamReader"
#include "java.lang.reflect.Type"
#include "java.util.Map"

public class BiomeLoader implements RegistryLoader<std::string, BiomeDefinitions> {
    private final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Color.class, new ColorTypeAdapter())
        .create();

    override public BiomeDefinitions load(std::string input) {
        Type type = new TypeToken<Map<std::string, BiomeDefinitionData>>() {}.getType();
        Map<std::string, BiomeDefinitionData> biomes;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input)) {
            biomes = GSON.fromJson(new InputStreamReader(stream), type);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Bedrock biomes!", e);
        }

        return new BiomeDefinitions(biomes);
    }

    public static class ColorTypeAdapter extends TypeAdapter<Color> {

        override public void write(JsonWriter out, Color color) throws IOException {
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

        override public Color read(JsonReader in) throws IOException {
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
