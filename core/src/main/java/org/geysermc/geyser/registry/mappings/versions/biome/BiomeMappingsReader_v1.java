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

package org.geysermc.geyser.registry.mappings.versions.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinitionRegisterException;
import org.geysermc.geyser.api.biome.custom.CustomBiomePrecipitation;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.biome.custom.GeyserCustomBiomeDefinition;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.MappingsReader;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BiomeMappingsReader_v1 implements MappingsReader<Identifier, CustomBiomeDefinition> {

    @Override
    public void read(Path file, JsonObject mappings, BiConsumer<Identifier, CustomBiomeDefinition> consumer) {
        // Sorted so registration conflicts don't depend on the file's property order
        mappings.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            if (entry.getValue().isJsonObject()) {
                try {
                    Identifier javaIdentifier = Identifier.of(entry.getKey());
                    consumer.accept(javaIdentifier, readDefinition(javaIdentifier, entry.getValue().getAsJsonObject(), "biome " + javaIdentifier));
                } catch (InvalidCustomMappingsFileException | IllegalArgumentException | CustomBiomeDefinitionRegisterException exception) {
                    GeyserImpl.getInstance().getLogger().error("Error reading custom biome " + entry.getKey() + " in custom mappings file: " + file.toString(), exception);
                }
            } else {
                GeyserImpl.getInstance().getLogger().error("Custom biome key " + entry.getKey() + " in custom mappings file " + file.toString() + " was not an object!");
            }
        });
    }

    /**
     * Reads one biome. Colors are read from the same {@code effects} and {@code attributes}
     * keys a Java biome uses, so the plain values of a Java biome definition can be copied
     * in as-is; the optional {@code geyser} object holds what Bedrock needs on top of that.
     * When no Bedrock identifier is named, one is derived from the Java identifier.
     */
    private CustomBiomeDefinition readDefinition(Identifier javaIdentifier, JsonObject object, String... context) throws InvalidCustomMappingsFileException {
        JsonObject geyser = readObject(object, "geyser", context);
        GeyserCustomBiomeDefinition.Builder builder;
        if (geyser != null && geyser.has("bedrock_identifier")) {
            builder = new GeyserCustomBiomeDefinition.Builder(
                MappingsUtil.readOrThrow(geyser, "bedrock_identifier", NodeReader.GEYSER_IDENTIFIER, context));
        } else {
            builder = GeyserCustomBiomeDefinition.derivedBuilder(javaIdentifier);
        }
        if (geyser != null) {
            MappingsUtil.readArrayIfPresent(geyser, "tags", tags -> tags.forEach(builder::tag), NodeReader.NON_EMPTY_STRING, context);
        }

        CustomBiomeAppearance.Builder appearance = CustomBiomeAppearance.builder();
        if (readAppearance(appearance, readObject(object, "effects", context), readObject(object, "attributes", context), geyser, context)) {
            builder.appearance(appearance);
        }
        return builder.build();
    }

    /**
     * Reads the appearance values, returning whether any was present, as the API rejects
     * an empty appearance.
     */
    private boolean readAppearance(CustomBiomeAppearance.Builder builder, @Nullable JsonObject effects,
                                   @Nullable JsonObject attributes, @Nullable JsonObject geyser, String... context) throws InvalidCustomMappingsFileException {
        boolean set = false;
        if (effects != null) {
            set |= readValue(effects, "water_color", builder::waterSurfaceColor, NodeReader.COLOR, context);
            set |= readValue(effects, "grass_color", builder::grassColor, NodeReader.COLOR, context);
            set |= readValue(effects, "foliage_color", builder::foliageColor, NodeReader.COLOR, context);
            set |= readValue(effects, "dry_foliage_color", builder::dryFoliageColor, NodeReader.COLOR, context);
        }
        if (attributes != null) {
            set |= readAttribute(attributes, "minecraft:visual/sky_color", builder::skyColor, NodeReader.COLOR, context);
            set |= readAttribute(attributes, "minecraft:visual/fog_color", builder::fogColor, NodeReader.COLOR, context);
            set |= readAttribute(attributes, "minecraft:visual/water_fog_color", builder::waterFogColor, NodeReader.COLOR, context);
            set |= readAttribute(attributes, "minecraft:visual/water_fog_end_distance", builder::waterFogEndDistance, NodeReader.FLOAT, context);
        }

        // The geyser values are read last, so they override the Java-shaped ones
        if (geyser != null) {
            set |= readValue(geyser, "water_surface_opacity", builder::waterSurfaceOpacity, NodeReader.FLOAT, context);
            set |= readValue(geyser, "water_fog_end_distance", builder::waterFogEndDistance, NodeReader.FLOAT, context);
            JsonObject precipitation = readObject(geyser, "precipitation", context);
            if (precipitation != null) {
                builder.precipitation(CustomBiomePrecipitation.of(
                    MappingsUtil.readOrThrow(precipitation, "type", NodeReader.PRECIPITATION_TYPE, context),
                    MappingsUtil.readOrThrow(precipitation, "density", NodeReader.FLOAT, context)));
                set = true;
            }
        }
        return set;
    }

    private <T> boolean readValue(JsonObject object, String key, Consumer<T> consumer, NodeReader<T> reader, String... context) throws InvalidCustomMappingsFileException {
        if (!object.has(key)) {
            return false;
        }
        consumer.accept(MappingsUtil.readOrThrow(object, key, reader, context));
        return true;
    }

    /**
     * Object-form attribute modifiers have no fixed result to translate, so they are
     * skipped rather than failing the biome.
     */
    private <T> boolean readAttribute(JsonObject object, String key, Consumer<T> consumer, NodeReader<T> reader, String... context) throws InvalidCustomMappingsFileException {
        if (object.get(key) instanceof JsonObject) {
            return false;
        }
        return readValue(object, key, consumer, reader, context);
    }

    private @Nullable JsonObject readObject(JsonObject object, String key, String... context) throws InvalidCustomMappingsFileException {
        JsonElement element = object.get(key);
        if (element == null) {
            return null;
        } else if (!element.isJsonObject()) {
            throw new InvalidCustomMappingsFileException("reading " + key, key + " must be an object", context);
        }
        return element.getAsJsonObject();
    }
}
