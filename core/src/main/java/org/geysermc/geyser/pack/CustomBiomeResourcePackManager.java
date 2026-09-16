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

package org.geysermc.geyser.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomePrecipitation;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.util.FileUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates the resource pack for registered custom biome appearances.
 */
public class CustomBiomeResourcePackManager {

    // The pack UUIDs hash this salt and the asset bytes, but not the manifest; bump it when
    // the manifest changes, so clients drop their cached copy
    private static final long RESOURCE_PACK_VERSION = 1;

    private static final String PACK_ROOT = "custom_biome_pack/";
    // The newest schema the pack can contain is the 1.26.0 water format; every supported
    // client has it
    private static final String MIN_ENGINE_VERSION = "[1, 26, 0]";
    private static final String CLIENT_BIOME_FORMAT_VERSION = "1.21.120";
    private static final String FOG_FORMAT_VERSION = "1.16.100";
    // The water schema version that introduced biome_water_color_contribution
    private static final String WATER_FORMAT_VERSION = "1.26.0";
    // One water setting serves every custom biome that colors its water surface, like
    // vanilla's minecraft:default_water
    private static final String WATER_IDENTIFIER = "geyser:biome_water";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static @Nullable Path createResourcePack() {
        Path cachePath = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache");
        try {
            Files.createDirectories(cachePath);
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().severe("Unable to create directories for the custom biome resource pack!", e);
            return null;
        }

        Path packPath = cachePath.resolve("custom_biomes.mcpack");
        File packFile = packPath.toFile();

        Map<String, byte[]> files = generateFiles();
        if (files.isEmpty()) {
            packFile.delete(); // No appearances to deliver
            return null;
        }

        // The pack is small enough to always rewrite; content-derived UUIDs keep the
        // client's own cache valid while the content is unchanged
        Pair<UUID, UUID> uuids = generatePackUUIDs(files);
        GeyserImpl.getInstance().getLogger().info("Creating custom biome resource pack.");
        try (ZipOutputStream zipOS = new ZipOutputStream(Files.newOutputStream(packPath))) {
            writeEntry(zipOS, PACK_ROOT + "manifest.json", manifestJson(uuids).getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                writeEntry(zipOS, PACK_ROOT + file.getKey(), file.getValue());
            }
            return packPath;
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().severe("Unable to create the custom biome resource pack!", e);
            GeyserImpl.getInstance().getLogger().severe("Geyser-generated custom biome appearances will be unavailable.");
            packFile.delete();
        }
        return null;
    }

    /**
     * Generates every appearance asset, keyed by pack-relative path. The map is sorted so
     * the content hash and the zip layout don't depend on catalogue iteration order.
     */
    private static Map<String, byte[]> generateFiles() {
        Map<String, byte[]> files = new TreeMap<>();
        String atmosphereTemplate = null;
        boolean water = false;
        for (CustomBiomeDefinition definition : Registries.CUSTOM_BIOMES.get().values()) {
            CustomBiomeAppearance appearance = definition.appearance();
            if (appearance == null) {
                continue;
            }
            String assetKey = assetKey(definition.bedrockIdentifier());

            files.put("biomes/" + assetKey + ".client_biome.json", clientBiomeJson(definition, assetKey).getBytes(StandardCharsets.UTF_8));
            if (hasFog(appearance)) {
                files.put("fogs/" + assetKey + ".fog.json", fogJson(appearance, assetKey).getBytes(StandardCharsets.UTF_8));
            }
            if (appearance.skyColor() != null) {
                if (atmosphereTemplate == null) {
                    atmosphereTemplate = FileUtils.readToString("bedrock/custom_biome_pack/atmosphere_settings.json");
                }
                files.put("atmospherics/" + assetKey + ".json", atmosphereJson(atmosphereTemplate, appearance.skyColor(), assetKey).getBytes(StandardCharsets.UTF_8));
            }
            water |= appearance.waterSurfaceColor() != null;
        }
        if (water) {
            files.put("water/biome_water.water.json", waterJson().getBytes(StandardCharsets.UTF_8));
        }
        return files;
    }

    private static String clientBiomeJson(CustomBiomeDefinition definition, String assetKey) {
        CustomBiomeAppearance appearance = definition.appearance();
        JsonObject components = new JsonObject();
        if (appearance.skyColor() != null) {
            components.add("minecraft:sky_color", color("sky_color", appearance.skyColor()));
            JsonObject atmosphere = new JsonObject();
            atmosphere.addProperty("atmosphere_identifier", "geyser:atmo_" + assetKey);
            components.add("minecraft:atmosphere_identifier", atmosphere);
        }
        if (hasFog(appearance)) {
            JsonObject fog = new JsonObject();
            fog.addProperty("fog_identifier", "geyser:fog_" + assetKey);
            components.add("minecraft:fog_appearance", fog);
        }
        if (appearance.waterSurfaceColor() != null || appearance.waterSurfaceOpacity() != null) {
            JsonObject water = new JsonObject();
            if (appearance.waterSurfaceColor() != null) {
                water.addProperty("surface_color", hex(appearance.waterSurfaceColor()));
            }
            if (appearance.waterSurfaceOpacity() != null) {
                water.addProperty("surface_opacity", appearance.waterSurfaceOpacity());
            }
            components.add("minecraft:water_appearance", water);
        }
        if (appearance.waterSurfaceColor() != null) {
            // Vibrant Visuals only mixes in the surface color through a bound water setting
            JsonObject water = new JsonObject();
            water.addProperty("water_identifier", WATER_IDENTIFIER);
            components.add("minecraft:water_identifier", water);
        }
        if (appearance.grassColor() != null) {
            components.add("minecraft:grass_appearance", color("color", appearance.grassColor()));
        }
        if (appearance.foliageColor() != null) {
            components.add("minecraft:foliage_appearance", color("color", appearance.foliageColor()));
        }
        if (appearance.dryFoliageColor() != null) {
            components.add("minecraft:dry_foliage_color", color("color", appearance.dryFoliageColor()));
        }
        CustomBiomePrecipitation precipitation = appearance.precipitation();
        if (precipitation != null) {
            JsonObject density = new JsonObject();
            density.addProperty(precipitation.type().name().toLowerCase(Locale.ROOT), precipitation.density());
            components.add("minecraft:precipitation", density);
        }

        JsonObject description = new JsonObject();
        description.addProperty("identifier", definition.bedrockIdentifier().toString());
        JsonObject clientBiome = new JsonObject();
        clientBiome.add("description", description);
        clientBiome.add("components", components);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", CLIENT_BIOME_FORMAT_VERSION);
        root.add("minecraft:client_biome", clientBiome);
        return GSON.toJson(root);
    }

    private static boolean hasFog(CustomBiomeAppearance appearance) {
        return appearance.fogColor() != null || appearance.waterFogColor() != null || appearance.waterFogEndDistance() != null;
    }

    /**
     * The client resolves fog per setting type, so only the blocks a value was supplied for
     * are emitted, and each emitted block is complete: members the caller didn't set are
     * filled in from the vanilla Bedrock default fog.
     */
    private static String fogJson(CustomBiomeAppearance appearance, String assetKey) {
        JsonObject distance = new JsonObject();
        if (appearance.fogColor() != null) {
            JsonObject air = new JsonObject();
            air.addProperty("fog_start", 0.92);
            air.addProperty("fog_end", 1.0);
            air.addProperty("fog_color", hex(appearance.fogColor()));
            air.addProperty("render_distance_type", "render");
            distance.add("air", air);

            // Weather fog replaces air fog in rain; without this block the default grey
            // would take over, so the custom color is kept, darkened with Java's full-rain factors
            JsonObject weather = new JsonObject();
            weather.addProperty("fog_start", 0.23);
            weather.addProperty("fog_end", 0.7);
            weather.addProperty("fog_color", hex(rainDarkened(appearance.fogColor())));
            weather.addProperty("render_distance_type", "render");
            distance.add("weather", weather);
        }
        if (appearance.waterFogColor() != null || appearance.waterFogEndDistance() != null) {
            String waterColor = appearance.waterFogColor() != null ? hex(appearance.waterFogColor()) : "#44AFF5";
            JsonObject water = new JsonObject();
            water.addProperty("fog_start", 0.0);
            water.addProperty("fog_end", appearance.waterFogEndDistance() != null ? appearance.waterFogEndDistance() : 60.0);
            water.addProperty("fog_color", waterColor);
            water.addProperty("render_distance_type", "fixed");
            water.add("transition_fog", transitionFog(waterColor));
            distance.add("water", water);
        }

        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geyser:fog_" + assetKey);
        JsonObject settings = new JsonObject();
        settings.add("description", description);
        settings.add("distance", distance);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", FOG_FORMAT_VERSION);
        root.add("minecraft:fog_settings", settings);
        return GSON.toJson(root);
    }

    // The gradual fade-in the vanilla default fog uses when the camera enters water
    private static JsonObject transitionFog(String waterColor) {
        JsonObject initFog = new JsonObject();
        initFog.addProperty("fog_start", 0.0);
        initFog.addProperty("fog_end", 0.01);
        initFog.addProperty("fog_color", waterColor);
        initFog.addProperty("render_distance_type", "fixed");
        JsonObject transition = new JsonObject();
        transition.add("init_fog", initFog);
        transition.addProperty("min_percent", 0.25);
        transition.addProperty("mid_seconds", 5);
        transition.addProperty("mid_percent", 0.6);
        transition.addProperty("max_seconds", 30);
        return transition;
    }

    // Java multiplies air fog with its full-rain color #7F7F99, dividing each channel by 255
    private static Color rainDarkened(Color color) {
        return new Color(color.getRed() * 127 / 255, color.getGreen() * 127 / 255, color.getBlue() * 153 / 255);
    }

    /**
     * Vibrant Visuals ignores the client biome sky color; it needs an atmosphere with the
     * sky color as its zenith. The template is the vanilla day cycle with the custom color
     * on the daylight keyframes only; the night keyframes stay vanilla, and sunsets
     * interpolate between the two.
     */
    private static String atmosphereJson(String template, Color skyColor, String assetKey) {
        return template
            .replace("${identifier}", "geyser:atmo_" + assetKey)
            .replace("${zenith_color}", "[" + skyColor.getRed() + ", " + skyColor.getGreen() + ", " + skyColor.getBlue() + "]");
    }

    /**
     * Gives the client biome's surface color maximum contribution in Vibrant Visuals
     * water; the renderer's other water properties still shape the final color.
     */
    private static String waterJson() {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", WATER_IDENTIFIER);
        JsonObject settings = new JsonObject();
        settings.add("description", description);
        settings.addProperty("biome_water_color_contribution", 1.0);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", WATER_FORMAT_VERSION);
        root.add("minecraft:water_settings", settings);
        return GSON.toJson(root);
    }

    private static String manifestJson(Pair<UUID, UUID> uuids) {
        // The pbr capability is required for the client to load this pack's Vibrant Visuals assets
        return """
            {
              "format_version": 2,
              "header": {
                "name": "Geyser Custom Biomes",
                "description": "Client-side visuals for custom biomes registered through the Geyser API",
                "uuid": "%s",
                "version": [1, 0, 0],
                "min_engine_version": %s
              },
              "modules": [
                {
                  "type": "resources",
                  "uuid": "%s",
                  "version": [1, 0, 0]
                }
              ],
              "capabilities": ["pbr"]
            }
            """.formatted(uuids.first(), MIN_ENGINE_VERSION, uuids.second());
    }

    /**
     * File names use a digest of the Bedrock identifier: identifiers can contain characters
     * that are unsafe in zip paths, and the digest gives each biome's assets a stable name
     * that operator packs can override.
     */
    private static String assetKey(Identifier bedrockIdentifier) {
        return HexFormat.of().formatHex(sha256(bedrockIdentifier.toString().getBytes(StandardCharsets.UTF_8)), 0, 16);
    }

    private static Pair<UUID, UUID> generatePackUUIDs(Map<String, byte[]> files) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        for (int i = 0; i < 8; i++) {
            digest.update((byte) ((RESOURCE_PACK_VERSION >> (i * 8)) & 0xFF));
        }
        files.forEach((path, bytes) -> {
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update(bytes);
        });

        ByteBuffer hash = ByteBuffer.wrap(digest.digest());
        return Pair.of(new UUID(hash.getLong(), hash.getLong()), new UUID(hash.getLong(), hash.getLong()));
    }

    private static void writeEntry(ZipOutputStream zipOS, String path, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setTime(0); // Fixed timestamps keep equal content byte-for-byte reproducible
        zipOS.putNextEntry(entry);
        zipOS.write(bytes);
        zipOS.closeEntry();
    }

    private static JsonObject color(String name, Color value) {
        JsonObject object = new JsonObject();
        object.addProperty(name, hex(value));
        return object;
    }

    private static String hex(Color color) {
        return "#%06X".formatted(color.getRGB() & 0xFFFFFF);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
