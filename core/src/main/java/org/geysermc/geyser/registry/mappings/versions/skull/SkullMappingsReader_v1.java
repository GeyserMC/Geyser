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

package org.geysermc.geyser.registry.mappings.versions.skull;

import com.google.gson.JsonObject;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.MappingsReader;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public class SkullMappingsReader_v1 implements MappingsReader<GeyserDefineCustomSkullsEvent.SkullTextureType, List<String>> {

    @Override
    public void read(Path file, JsonObject mappings, BiConsumer<GeyserDefineCustomSkullsEvent.SkullTextureType, List<String>> consumer) {
        mappings.entrySet().forEach(entry -> {
            if (entry.getValue().isJsonArray()) {
                try {
                    GeyserDefineCustomSkullsEvent.SkullTextureType textureType = MappingsUtil.readOrThrowKey(entry.getKey(), NodeReader.SKULL_TEXTURE_TYPE, "skull mappings");
                    List<String> textures = MappingsUtil.readArrayOrThrow(mappings, entry.getKey(), NodeReader.NON_EMPTY_STRING, "skull mappings");
                    consumer.accept(textureType, textures);
                } catch (InvalidCustomMappingsFileException | IllegalArgumentException exception) {
                    GeyserImpl.getInstance().getLogger().error("Error reading skull mappings for " + entry.getKey() + " in custom mappings file: " + file.toString(), exception);
                }
            } else {
                GeyserImpl.getInstance().getLogger().error("Skull mappings key " + entry.getKey() + " in custom mappings file " + file.toString() + " was not an array!");
            }
        });
    }
}
