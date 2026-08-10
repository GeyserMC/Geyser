/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.InvalidKeyException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.MinecraftKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BiomeResourcePackManager {
    private static final String INPUT_FILE = "biome-visuals.json";
    private static final int FORMAT_VERSION = 1;
    private static final int RESOURCE_PACK_VERSION = 1;
    private static final int CUSTOM_BIOME_ID_START = 30_000;

    private BiomeResourcePackManager() {
    }

    public static @Nullable Path createResourcePack() {
        Registries.CUSTOM_BIOME_IDENTIFIERS.get().clear();

        GeyserImpl geyser = GeyserImpl.getInstance();
        Path input = geyser.getBootstrap().getConfigFolder().resolve(INPUT_FILE);
        if (!Files.isRegularFile(input)) {
            return null;
        }

        Path output = geyser.getBootstrap().getConfigFolder().resolve("cache").resolve("GeyserBiomeVisuals.mcpack");
        try {
            Set<String> bedrockIdentifiers = Registries.BIOMES.get().getDefinitions().keySet();
            Files.createDirectories(output.getParent());
            int biomeCount = createResourcePack(input, output, bedrockIdentifiers);
            geyser.getLogger()
                    .info("Generated Bedrock biome visuals resource pack with " + biomeCount + " biome appearance(s).");
            return output;
        } catch (Exception e) {
            geyser.getLogger().error("Failed to generate Bedrock biome visuals resource pack!", e);
            return null;
        }
    }

    private static int createResourcePack(Path input, Path output, Set<String> bedrockIdentifiers) throws IOException {
        Map<String, BiomeVisuals> biomes = load(input);
        Object2IntMap<String> customMappings = customMappings(biomes.keySet(), bedrockIdentifiers);
        write(output, biomes);
        Registries.CUSTOM_BIOME_IDENTIFIERS.set(customMappings);
        return biomes.size();
    }

    private static Map<String, BiomeVisuals> load(Path input) throws IOException {
        BiomeConfig config;
        try (InputStream stream = Files.newInputStream(input)) {
            config = FileUtils.loadJson(stream, BiomeConfig.class);
        }

        if (config == null || config.formatVersion() != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported biome visuals format version");
        }
        if (config.biomes() == null) {
            throw new IllegalArgumentException("No biome visuals were defined");
        }

        return resolve(config.biomes());
    }

    private static Map<String, BiomeVisuals> resolve(Map<String, BiomeDefinition> definitions) {
        Map<String, BiomeVisuals> resolved = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<String, BiomeDefinition> biome : definitions.entrySet()) {
            String javaIdentifier = biome.getKey();
            BiomeDefinition definition = biome.getValue();
            if (definition == null) {
                throw new IllegalArgumentException("No biome visuals were defined for " + javaIdentifier);
            }
            validateIdentifier(javaIdentifier);

            BiomeVisuals visuals = new BiomeVisuals(
                    color(definition.waterColor()),
                    color(definition.waterFogColor()),
                    color(definition.fogColor()),
                    optionalColor(definition.skyColor()),
                    optionalColor(definition.grassColor()),
                    optionalColor(definition.foliageColor()));
            resolved.put(javaIdentifier, visuals);
        }
        return resolved;
    }

    private static Object2IntMap<String> customMappings(Set<String> configuredIdentifiers, Set<String> bedrockIdentifiers) {
        Object2IntMap<String> customMappings = new Object2IntOpenHashMap<>();
        int customId = CUSTOM_BIOME_ID_START;
        for (String identifier : configuredIdentifiers) {
            if (!bedrockIdentifiers.contains(identifier)) {
                customMappings.put(identifier, customId++);
            }
        }
        return customMappings;
    }

    private static void validateIdentifier(String identifier) {
        try {
            if (!MinecraftKey.key(identifier).asString().equals(identifier)) {
                throw new IllegalArgumentException("Invalid biome identifier " + identifier);
            }
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid biome identifier " + identifier, e);
        }
    }

    private static void write(Path output, Map<String, BiomeVisuals> biomes) throws IOException {
        Map<String, BiomeVisuals> sortedBiomes = new TreeMap<>(biomes);
        UUID packUuid = UUID.nameUUIDFromBytes(
                (RESOURCE_PACK_VERSION + sortedBiomes.toString()).getBytes(StandardCharsets.UTF_8));
        UUID moduleUuid = UUID.nameUUIDFromBytes((packUuid + "/module").getBytes(StandardCharsets.UTF_8));

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            write(zip, "manifest.json", manifest(packUuid, moduleUuid));

            for (Map.Entry<String, BiomeVisuals> entry : sortedBiomes.entrySet()) {
                String biomeIdentifier = entry.getKey();
                String fileIdentifier = fileIdentifier(biomeIdentifier);
                String fogIdentifier = "geyser:fog_" + fileIdentifier;
                write(zip, "biomes/" + fileIdentifier + ".client_biome.json",
                        entry.getValue().clientBiome(biomeIdentifier, fogIdentifier));
                write(zip, "fogs/" + fileIdentifier + "_fog_setting.json", entry.getValue().fog(fogIdentifier));
            }
        }
    }

    private static int color(@Nullable String color) {
        if (color == null || !color.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Biome colors must use the #RRGGBB format");
        }
        return Integer.parseInt(color.substring(1), 16);
    }

    private static String fileIdentifier(String identifier) {
        String fileIdentifier = identifier.startsWith("minecraft:")
                ? identifier.substring("minecraft:".length())
                : identifier;
        return fileIdentifier.replace(':', '_').replace('/', '_');
    }

    private static @Nullable Integer optionalColor(@Nullable String color) {
        return color == null ? null : color(color);
    }

    private static JsonObject manifest(UUID packUuid, UUID moduleUuid) {
        JsonArray version = version(1, 0, 0);

        JsonObject header = new JsonObject();
        header.addProperty("name", "Geyser Java biome visuals");
        header.addProperty("description", "Java biome colors converted for Bedrock clients");
        header.addProperty("uuid", packUuid.toString());
        header.add("version", version);
        header.add("min_engine_version", version(1, 21, 40));

        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", moduleUuid.toString());
        module.add("version", version);

        JsonArray modules = new JsonArray();
        modules.add(module);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return manifest;
    }

    private static JsonArray version(int major, int minor, int patch) {
        JsonArray version = new JsonArray();
        version.add(major);
        version.add(minor);
        version.add(patch);
        return version;
    }

    private static void write(ZipOutputStream zip, String name, JsonObject json) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(GeyserImpl.GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private record BiomeConfig(
            @SerializedName("format_version") int formatVersion,
            Map<String, BiomeDefinition> biomes) {
    }

    private record BiomeDefinition(
            @SerializedName("water_color") String waterColor,
            @SerializedName("water_fog_color") String waterFogColor,
            @SerializedName("fog_color") String fogColor,
            @SerializedName("sky_color") @Nullable String skyColor,
            @SerializedName("grass_color") @Nullable String grassColor,
            @SerializedName("foliage_color") @Nullable String foliageColor) {
    }

    private record BiomeVisuals(
            int waterColor,
            int waterFogColor,
            int fogColor,
            @Nullable Integer skyColor,
            @Nullable Integer grassColor,
            @Nullable Integer foliageColor) {

        private JsonObject clientBiome(String biomeIdentifier, String fogIdentifier) {
            JsonObject components = new JsonObject();

            JsonObject water = new JsonObject();
            water.addProperty("surface_color", rgb(waterColor));
            components.add("minecraft:water_appearance", water);

            if (skyColor != null) {
                JsonObject sky = new JsonObject();
                sky.addProperty("sky_color", rgb(skyColor));
                components.add("minecraft:sky_color", sky);
            }

            if (grassColor != null) {
                JsonObject grass = new JsonObject();
                grass.addProperty("color", rgb(grassColor));
                components.add("minecraft:grass_appearance", grass);
            }

            if (foliageColor != null) {
                JsonObject foliage = new JsonObject();
                foliage.addProperty("color", rgb(foliageColor));
                components.add("minecraft:foliage_appearance", foliage);
            }

            JsonObject fogAppearance = new JsonObject();
            fogAppearance.addProperty("fog_identifier", fogIdentifier);
            components.add("minecraft:fog_appearance", fogAppearance);

            JsonObject description = new JsonObject();
            description.addProperty("identifier", biomeIdentifier);

            JsonObject clientBiome = new JsonObject();
            clientBiome.add("description", description);
            clientBiome.add("components", components);

            JsonObject root = new JsonObject();
            root.addProperty("format_version", "1.21.120");
            root.add("minecraft:client_biome", clientBiome);
            return root;
        }

        private JsonObject fog(String fogIdentifier) {
            JsonObject distance = new JsonObject();
            distance.add("air", fogDistance(fogColor));
            distance.add("water", fogDistance(waterFogColor));

            JsonObject description = new JsonObject();
            description.addProperty("identifier", fogIdentifier);

            JsonObject settings = new JsonObject();
            settings.add("description", description);
            settings.add("distance", distance);

            JsonObject root = new JsonObject();
            root.addProperty("format_version", "1.16.100");
            root.add("minecraft:fog_settings", settings);
            return root;
        }

        private static JsonObject fogDistance(int color) {
            JsonObject distance = new JsonObject();
            distance.addProperty("fog_start", 0);
            distance.addProperty("fog_end", 1);
            distance.addProperty("fog_color", rgb(color));
            distance.addProperty("render_distance_type", "render");
            return distance;
        }

        private static String rgb(int color) {
            return "#%06X".formatted(color & 0xFFFFFF);
        }
    }
}
