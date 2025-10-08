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

package org.geysermc.geyser.registry.type;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.loader.RegistryLoader;
import org.geysermc.geyser.util.MinecraftKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Used to load and store the {@code mappings/util.json} file. When adding new fields to util mappings, be sure to create a proper registry for them
 * in {@link org.geysermc.geyser.registry.Registries}. Use {@link org.geysermc.geyser.registry.loader.RegistryLoaders#UTIL_MAPPINGS_KEYS} (or create a new loader if loading something
 * other than keys).
 */
public record UtilMappings(List<Key> gameMasterBlocks, List<Key> dangerousBlockEntities, List<Key> dangerousEntities) {
    private static final String INPUT = "mappings/util.json";
    private static UtilMappings loaded = null;

    /**
     * Gets the loaded util mappings, or loads them if they weren't yet.
     */
    private static UtilMappings get() {
        if (loaded == null) {
            try (InputStream utilInput = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(INPUT)) {
                //noinspection deprecation - 1.16.5 breaks otherwise
                JsonObject utilJson = new JsonParser().parse(new InputStreamReader(utilInput)).getAsJsonObject();

                List<Key> gameMasterBlocks = new ArrayList<>();
                List<Key> dangerousBlockEntities = new ArrayList<>();
                List<Key> dangerousEntities = new ArrayList<>();

                utilJson.get("game_master_blocks").getAsJsonArray()
                    .forEach(element -> gameMasterBlocks.add(MinecraftKey.key(element.getAsString())));
                utilJson.get("dangerous_block_entities").getAsJsonArray()
                    .forEach(element -> dangerousBlockEntities.add(MinecraftKey.key(element.getAsString())));
                utilJson.get("dangerous_entities").getAsJsonArray()
                    .forEach(element -> dangerousEntities.add(MinecraftKey.key(element.getAsString())));

                loaded = new UtilMappings(List.copyOf(gameMasterBlocks), List.copyOf(dangerousBlockEntities), List.copyOf(dangerousEntities));
            } catch (IOException e) {
                throw new AssertionError("Failed to load " + INPUT);
            }
        }
        return loaded;
    }

    /**
     * Simply gets a field of the loaded {@link UtilMappings} object. Instead of re-opening the util mappings file every time a field is loaded,
     * the mappings are parsed once by {@link UtilMappings#get()} and kept in a static variable.
     * Loader input is a function that extracts the field to get from the {@link UtilMappings} object.
     */
    public static class Loader<T> implements RegistryLoader<Function<UtilMappings, T>, T> {

        @Override
        public T load(Function<UtilMappings, T> input) {
            return input.apply(UtilMappings.get());
        }
    }
}
