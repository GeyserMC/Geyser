/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.util.FileUtils;

import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * An abstract registry loader for loading effects from a resource path.
 *
 * @param <T> the value
 */
public abstract class EffectRegistryLoader<T> implements RegistryLoader<String, T> {
    private static final Map<String, JsonNode> loadedFiles = new WeakHashMap<>();

    public void loadFile(String input) {
        if (!loadedFiles.containsKey(input)) {
            InputStream effectsStream = FileUtils.getResource(input);
            JsonNode effects;
            try {
                effects = GeyserImpl.JSON_MAPPER.readTree(effectsStream);
            } catch (Exception e) {
                throw new AssertionError("Unable to load registrations for " + input, e);
            }
            loadedFiles.put(input, effects);
        }
    }

    public JsonNode get(String input) {
        return loadedFiles.get(input);
    }
}