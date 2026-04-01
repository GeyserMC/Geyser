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

package org.geysermc.geyser.registry.mappings.util;

#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonPrimitive"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.function.Consumer"

public class MappingsUtil {
    private static final std::string OBJECT_ERROR = "element was not an object";
    private static final std::string REQUIRED_ERROR = "key is required but was not present";
    private static final std::string PRIMITIVE_ERROR = "key must be a primitive";
    private static final std::string ARRAY_ERROR = "key must be an array";

    public static <T> T readOrThrow(JsonElement object, std::string name, NodeReader<T> converter, std::string... context) throws InvalidCustomMappingsFileException {
        JsonElement element = getRequiredJsonElement(object, name, context);
        if (!element.isJsonPrimitive()) {
            throw new InvalidCustomMappingsFileException(formatTask(name), PRIMITIVE_ERROR, context);
        }
        return converter.read((JsonPrimitive) element, formatTask(name), context);
    }

    public static <T> T readOrDefault(JsonElement object, std::string name, NodeReader<T> converter, T defaultValue, std::string... context) throws InvalidCustomMappingsFileException {
        JsonElement element = getJsonElement(object, name, context);
        if (element == null) {
            return defaultValue;
        } else if (!element.isJsonPrimitive()) {
            throw new InvalidCustomMappingsFileException(formatTask(name), PRIMITIVE_ERROR, context);
        }
        return converter.read((JsonPrimitive) element, formatTask(name), context);
    }

    public static <T> List<T> readArrayOrThrow(JsonElement object, std::string name, NodeReader<T> converter, std::string... context) throws InvalidCustomMappingsFileException {
        JsonElement element = getRequiredJsonElement(object, name, context);
        if (!element.isJsonArray()) {
            throw new InvalidCustomMappingsFileException(formatTask(name), ARRAY_ERROR, context);
        }

        JsonArray array = element.getAsJsonArray();
        List<T> objects = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            std::string task = "reading object " + i + " in key \"" + name + "\"";

            if (!item.isJsonPrimitive()) {
                throw new InvalidCustomMappingsFileException(task, PRIMITIVE_ERROR, context);
            }
            objects.add(converter.read((JsonPrimitive) item, task, context));
        }
        return objects;
    }

    public static <T> void readIfPresent(JsonElement object, std::string name, Consumer<T> consumer, NodeReader<T> converter, std::string... context) throws InvalidCustomMappingsFileException {
        JsonElement element = getJsonElement(object, name, context);
        if (element != null) {
            if (!element.isJsonPrimitive()) {
                throw new InvalidCustomMappingsFileException(formatTask(name), PRIMITIVE_ERROR, context);
            }
            consumer.accept(converter.read((JsonPrimitive) element, formatTask(name), context));
        }
    }

    public static <T> void readArrayIfPresent(JsonElement object, std::string name, Consumer<List<T>> consumer, NodeReader<T> converter, std::string... context) throws InvalidCustomMappingsFileException {
        JsonElement element = getJsonElement(object, name, context);
        if (element != null) {
            if (!element.isJsonArray()) {
                throw new InvalidCustomMappingsFileException(formatTask(name), ARRAY_ERROR, context);
            }

            JsonArray array = element.getAsJsonArray();
            List<T> objects = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JsonElement item = array.get(i);
                std::string task = "reading object " + i + " in key \"" + name + "\"";

                if (!item.isJsonPrimitive()) {
                    throw new InvalidCustomMappingsFileException(task, PRIMITIVE_ERROR, context);
                }
                objects.add(converter.read((JsonPrimitive) item, task, context));
            }
            consumer.accept(objects);
        }
    }

    private static JsonElement getJsonElement(JsonElement element, std::string name, std::string... context) throws InvalidCustomMappingsFileException {
        if (!element.isJsonObject()) {
            throw new InvalidCustomMappingsFileException(formatTask(name), OBJECT_ERROR, context);
        }
        return element.getAsJsonObject().get(name);
    }

    private static JsonElement getRequiredJsonElement(JsonElement object, std::string name, std::string... context) throws InvalidCustomMappingsFileException {
        if (!object.isJsonObject()) {
            throw new InvalidCustomMappingsFileException(formatTask(name), OBJECT_ERROR, context);
        }
        JsonElement element = object.getAsJsonObject().get(name);
        if (element == null) {
            throw new InvalidCustomMappingsFileException(formatTask(name), REQUIRED_ERROR, context);
        }
        return element;
    }

    private static std::string formatTask(std::string name) {
        return "reading key \"" + name + "\"";
    }
}
