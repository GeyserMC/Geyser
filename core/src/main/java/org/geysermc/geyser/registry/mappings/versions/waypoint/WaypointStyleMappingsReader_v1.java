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

package org.geysermc.geyser.registry.mappings.versions.waypoint;

import com.google.gson.JsonObject;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyle;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.MappingsReader;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public class WaypointStyleMappingsReader_v1 implements MappingsReader<Identifier, CustomWaypointStyle> {

    @Override
    public void read(Path file, JsonObject mappings, BiConsumer<Identifier, CustomWaypointStyle> consumer) {
        mappings.entrySet().forEach(entry -> {
            if (entry.getValue().isJsonObject()) {
                try {
                    Identifier identifier = Identifier.of(entry.getKey());
                    consumer.accept(identifier, readStyle(entry.getValue().getAsJsonObject(), "waypoint style " + identifier));
                } catch (InvalidCustomMappingsFileException | IllegalArgumentException exception) {
                    GeyserImpl.getInstance().getLogger().error("Error reading waypoint style " + entry.getKey() + " in custom mappings file: " + file.toString(), exception);
                }
            } else {
                GeyserImpl.getInstance().getLogger().error("Waypoint style key " + entry.getKey() + " in custom mappings file " + file.toString() + " was not an object!");
            }
        });
    }

    private CustomWaypointStyle readStyle(JsonObject object, String... context) throws InvalidCustomMappingsFileException {
        int nearDistance = MappingsUtil.readOrDefault(object, "near_distance", NodeReader.NON_NEGATIVE_INT, 128, context);
        int fartDistance = MappingsUtil.readOrDefault(object, "far_distance", NodeReader.NON_NEGATIVE_INT, 332, context);
        List<Identifier> sprites = MappingsUtil.readArrayOrThrow(object, "sprites", NodeReader.IDENTIFIER, context);

        if (sprites.isEmpty()) {
            throw new InvalidCustomMappingsFileException("reading custom waypoint style", "must have at least 1 sprite", context);
        }
        CustomWaypointStyle.VanillaBuilder builder = CustomWaypointStyle.vanillaLike(nearDistance, fartDistance);
        sprites.forEach(builder::withTexture);
        return builder.build();
    }
}
