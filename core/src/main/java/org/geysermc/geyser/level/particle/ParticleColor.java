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

package org.geysermc.geyser.level.particle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public sealed interface ParticleColor {

    String variable();

    int apply(ParticleRandomization randomization);

    static ParticleColor parseColorFromMappings(JsonElement color) {
        if (color.isJsonPrimitive()) {
            return new Constant(ParticleHelper.DEFAULT_COLOR_VARIABLE, color.getAsInt());
        } else if (color.isJsonObject()) {
            JsonObject full = color.getAsJsonObject();
            String type = full.getAsJsonPrimitive("type").getAsString();
            String variable = Optional.ofNullable(full.getAsJsonPrimitive("variable"))
                .map(JsonPrimitive::getAsString)
                .orElse(ParticleHelper.DEFAULT_COLOR_VARIABLE);

            // Constants are usually serialised as just an int, but can be a full object (useful if a different variable name is used)
            if (type.equals("constant")) {
                int value = full.getAsJsonPrimitive("value").getAsInt();
                return new Constant(variable, value);
            } else if (type.equals("random")) {
                JsonArray rawValues = full.getAsJsonArray("values");
                if (rawValues == null) {
                    return new Random(variable, List.of());
                }

                List<Integer> values = new ObjectArrayList<>();
                rawValues.forEach(element -> values.add(element.getAsInt()));
                return new Random(variable, Collections.unmodifiableList(values));
            }
            throw new AssertionError("Expected type of color settings to be \"constant\" or \"random\", was: " + type);
        }
        throw new AssertionError("Expected color key to be JSON object, was: " + color);
    }

    record Constant(String variable, int rgb) implements ParticleColor {

        @Override
        public int apply(ParticleRandomization randomization) {
            return rgb;
        }
    }

    record Random(String variable, List<Integer> values) implements ParticleColor {
        private static final double PHASE = Math.PI * 2.0;

        @Override
        public int apply(ParticleRandomization randomization) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (values.isEmpty()) {
                // Used by the NOTE particle - we need to pick a random colour
                // This estimates https://mcsrc.dev/2/26.4-snapshot-8/net/minecraft/client/particle/NoteParticle#L19-21
                // Presumably Java does this to have a nicer selection of random colours ("true" random would be more boring)

                double baseColor = randomization.xAux();
                int red = (int) (Math.max(0.0, Math.sin(baseColor * PHASE) * 0.65 + 0.35) * 255);
                int green = (int) (Math.max(0.0, Math.sin((baseColor + 0.33333334) * PHASE) * 0.65 + 0.35) * 255);
                int blue = (int) (Math.max(0.0, Math.sin((baseColor + 0.6666667) * PHASE) * 0.65 + 0.35) * 255);
                return red << 16 | green << 8 | blue;
            }
            return values.get(random.nextInt(values.size()));
        }
    }
}
