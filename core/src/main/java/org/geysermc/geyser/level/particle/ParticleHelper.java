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
import org.cloudburstmc.math.vector.Vector3f;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class ParticleHelper {
    public static final ValueSerializer<Float> FLOAT = createSerializer("float", JsonPrimitive::new);
    public static final ValueSerializer<Collection<Variable<?>>> MEMBER_ARRAY = createSerializer("member_array", ParticleHelper::serializeVariables);
    public static final ValueSerializer<Integer> RGB_COLOR = MEMBER_ARRAY.compose(rgb -> {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        return List.of(
            createVariable(".r", FLOAT, red),
            createVariable(".g", FLOAT, green),
            createVariable(".b", FLOAT, blue),
            createVariable(".a", FLOAT, 1.0F));
    });
    public static final ValueSerializer<Vector3f> VECTOR3F = MEMBER_ARRAY.compose(vector -> List.of(
        createVariable(".x", FLOAT, vector.getX()),
        createVariable(".y", FLOAT, vector.getY()),
        createVariable(".z", FLOAT, vector.getZ())
    ));

    private static <T> ValueSerializer<T> createSerializer(String name, Function<T, ? extends JsonElement> serializer) {
        return new ValueSerializer<T>() {
            @Override
            public String type() {
                return name;
            }

            @Override
            public JsonElement serialize(T value) {
                return serializer.apply(value);
            }
        };
    }

    private ParticleHelper() {}

    public static <T> Variable<T> createVariable(String name, ValueSerializer<T> serializer, T value) {
        return new Variable<>(name, value, serializer);
    }

    public static <T> JsonElement serializeVariable(String name, ValueSerializer<T> serializer, T value) {
        return serializeVariables(Collections.singleton(createVariable(name, serializer, value)));
    }

    public static JsonElement serializeVariables(Collection<Variable<?>> variables) {
        JsonArray array = new JsonArray();
        variables.forEach(variable -> array.add(variable.serialize()));
        return array;
    }

    public static JsonElement serializeColor(int rgb) {
        return serializeVariable("variable.color", RGB_COLOR, rgb);
    }

    public interface ValueSerializer<T> {

        String type();

        JsonElement serialize(T value);

        default <C> ValueSerializer<C> compose(Function<C, T> composer) {
            return new ValueSerializer<>() {
                @Override
                public String type() {
                    return ValueSerializer.this.type();
                }

                @Override
                public JsonElement serialize(C value) {
                    return ValueSerializer.this.serialize(composer.apply(value));
                }
            };
        }
    }

    public static final class Variable<T> {
        private final String name;
        private final T value;
        private final ValueSerializer<T> serializer;

        private Variable(String name, T value, ValueSerializer<T> serializer) {
            this.name = name;
            this.value = value;
            this.serializer = serializer;
        }

        private JsonElement serialize() {
            JsonObject variable = new JsonObject();
            variable.addProperty("name", name);

            JsonObject value = new JsonObject();
            value.addProperty("type", serializer.type());
            value.add("value", serializer.serialize(this.value));
            variable.add("value", value);

            return variable;
        }
    }
}
