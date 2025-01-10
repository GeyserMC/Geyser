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

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;

import java.util.function.Consumer;

public class MappingsUtil {

    public static <T> T readOrThrow(JsonNode node, String name, NodeReader<T> converter, String... context) throws InvalidCustomMappingsFileException {
        JsonNode object = node.get(name);
        if (object == null) {
            throw new InvalidCustomMappingsFileException(formatTask(name), "key is required but was not present", context);
        }
        return converter.read(object, formatTask(name), context);
    }

    public static <T> T readOrDefault(JsonNode node, String name, NodeReader<T> converter, T defaultValue, String... context) throws InvalidCustomMappingsFileException {
        JsonNode object = node.get(name);
        if (object == null) {
            return defaultValue;
        }
        return converter.read(object, formatTask(name), context);
    }

    public static void readTextIfPresent(JsonNode node, String name, Consumer<String> consumer, String... context) throws InvalidCustomMappingsFileException {
        readIfPresent(node, name, consumer, NodeReader.STRING, context);
    }

    public static <T> void readIfPresent(JsonNode node, String name, Consumer<T> consumer, NodeReader<T> converter, String... context) throws InvalidCustomMappingsFileException {
        if (node.has(name)) {
            consumer.accept(converter.read(node.get(name), formatTask(name), context));
        }
    }

    public static void requireObject(JsonNode node, String task, String... context) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException(task, "expected an object", context);
        }
    }

    private static String formatTask(String name) {
        return "reading key \"" + name + "\"";
    }
}
