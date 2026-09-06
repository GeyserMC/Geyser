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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomePrecipitation;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CustomBiomeResourcePackManagerTest {

    @TempDir
    Path configFolder;

    @AfterEach
    public void clearCatalogue() {
        Registries.CUSTOM_BIOMES.set(new Object2ObjectOpenHashMap<>());
    }

    @Test
    void generatesAppearanceAssets() {
        Map<String, JsonObject> files = new HashMap<>();
        GeyserMockContext.mockContext(context -> {
            setup(context);
            catalogue(Map.of(
                "test:java_styled", definition("test:styled", true),
                "test:java_plain", definition("test:plain", false)));

            Path pack = CustomBiomeResourcePackManager.createResourcePack();
            assertNotNull(pack);
            files.putAll(readJsonEntries(pack));
        });

        // The appearance-less biome produces no assets; the styled one produces all four
        assertEquals(5, files.size()); // manifest + client_biome + fog + atmospherics + water
        JsonObject clientBiome = files.values().stream()
            .filter(json -> json.has("minecraft:client_biome"))
            .findFirst().orElseThrow()
            .getAsJsonObject("minecraft:client_biome");
        assertEquals("test:styled", clientBiome.getAsJsonObject("description").get("identifier").getAsString());

        JsonObject components = clientBiome.getAsJsonObject("components");
        assertEquals("#78A7FF", components.getAsJsonObject("minecraft:sky_color").get("sky_color").getAsString());
        assertEquals("#00FFFF", components.getAsJsonObject("minecraft:water_appearance").get("surface_color").getAsString());
        assertEquals(0.55F, components.getAsJsonObject("minecraft:water_appearance").get("surface_opacity").getAsFloat());
        assertEquals("#5F9F45", components.getAsJsonObject("minecraft:grass_appearance").get("color").getAsString());
        assertEquals("#4F8F3F", components.getAsJsonObject("minecraft:foliage_appearance").get("color").getAsString());
        assertEquals("#9E814D", components.getAsJsonObject("minecraft:dry_foliage_color").get("color").getAsString());
        assertEquals(2.0F, components.getAsJsonObject("minecraft:precipitation").get("white_ash").getAsFloat());
        String fogIdentifier = components.getAsJsonObject("minecraft:fog_appearance").get("fog_identifier").getAsString();

        // Vibrant Visuals only honors the surface color through a bound water setting,
        // which all surface-colored custom biomes share
        String waterIdentifier = components.getAsJsonObject("minecraft:water_identifier").get("water_identifier").getAsString();
        assertEquals("geyser:biome_water", waterIdentifier);
        JsonObject waterRoot = files.values().stream()
            .filter(json -> json.has("minecraft:water_settings"))
            .findFirst().orElseThrow();
        // biome_water_color_contribution only exists from water schema 1.26.0 on
        assertEquals("1.26.0", waterRoot.get("format_version").getAsString());
        JsonObject waterSettings = waterRoot.getAsJsonObject("minecraft:water_settings");
        assertEquals(waterIdentifier, waterSettings.getAsJsonObject("description").get("identifier").getAsString());
        assertEquals(1.0F, waterSettings.get("biome_water_color_contribution").getAsFloat());

        JsonObject fog = files.values().stream()
            .filter(json -> json.has("minecraft:fog_settings"))
            .findFirst().orElseThrow()
            .getAsJsonObject("minecraft:fog_settings");
        assertEquals(fogIdentifier, fog.getAsJsonObject("description").get("identifier").getAsString());
        JsonObject water = fog.getAsJsonObject("distance").getAsJsonObject("water");
        assertEquals("#050533", water.get("fog_color").getAsString());
        assertEquals(48.0F, water.get("fog_end").getAsFloat());
        // Entering water fades in like the vanilla default fog does
        assertEquals("#050533", water.getAsJsonObject("transition_fog")
            .getAsJsonObject("init_fog").get("fog_color").getAsString());
        // No air fog color was given, so neither an air nor a weather block may be emitted
        assertNull(fog.getAsJsonObject("distance").get("air"));
        assertNull(fog.getAsJsonObject("distance").get("weather"));

        JsonObject atmosphere = files.values().stream()
            .filter(json -> json.has("minecraft:atmosphere_settings"))
            .findFirst().orElseThrow()
            .getAsJsonObject("minecraft:atmosphere_settings");
        // The custom color applies to the daylight keyframes only; the night keyframes stay vanilla
        JsonObject zenith = atmosphere.getAsJsonObject("sky_zenith_color");
        assertEquals(5, zenith.size());
        for (String daylightKey : new String[] {"0.000000", "0.199685", "0.800315"}) {
            assertEquals("[120,167,255]", zenith.get(daylightKey).getAsJsonArray().toString());
        }
        for (String nightKey : new String[] {"0.352560", "0.644880"}) {
            assertEquals("[40,40,40]", zenith.get(nightKey).getAsJsonArray().toString());
        }
    }

    @Test
    void fogBlocksAreCompletedFromVanillaDefaults() {
        GeyserMockContext.mockContext(context -> {
            setup(context);
            catalogue(Map.of(
                "test:java_end_only", definitionWithAppearance("test:end_only",
                    CustomBiomeAppearance.builder().waterFogEndDistance(12.0F)),
                "test:java_color_only", definitionWithAppearance("test:color_only",
                    CustomBiomeAppearance.builder().waterFogColor(new Color(0x050533))),
                "test:java_air_only", definitionWithAppearance("test:air_only",
                    CustomBiomeAppearance.builder().fogColor(new Color(0x808080)))));

            Map<String, JsonObject> files = readJsonEntries(CustomBiomeResourcePackManager.createResourcePack());
            Map<String, JsonObject> fogs = new HashMap<>();
            files.forEach((path, json) -> {
                if (json.has("minecraft:fog_settings")) {
                    JsonObject settings = json.getAsJsonObject("minecraft:fog_settings");
                    fogs.put(settings.getAsJsonObject("description").get("identifier").getAsString(),
                        settings.getAsJsonObject("distance"));
                }
            });
            assertEquals(3, fogs.size());
            assertEquals(2, fogs.values().stream().filter(distance -> distance.has("water")).count());

            for (JsonObject distance : fogs.values()) {
                JsonObject water = distance.getAsJsonObject("water");
                if (water == null) {
                    continue;
                }
                boolean endOnly = water.get("fog_end").getAsFloat() == 12.0F;
                assertEquals(endOnly ? "#44AFF5" : "#050533", water.get("fog_color").getAsString());
                assertEquals(endOnly ? 12.0F : 60.0F, water.get("fog_end").getAsFloat());
                // The entry transition uses the block's own color and the vanilla timing
                JsonObject transition = water.getAsJsonObject("transition_fog");
                assertEquals(water.get("fog_color").getAsString(),
                    transition.getAsJsonObject("init_fog").get("fog_color").getAsString());
                assertEquals(30, transition.get("max_seconds").getAsInt());
            }

            // Air fog brings a weather companion, so rain doesn't fall back to the default
            // grey; the color is the air color multiplied with Java's full-rain #7F7F99
            JsonObject airOnly = fogs.values().stream()
                .filter(distance -> distance.has("air"))
                .findFirst().orElseThrow();
            JsonObject air = airOnly.getAsJsonObject("air");
            assertEquals("#808080", air.get("fog_color").getAsString());
            assertEquals(0.92F, air.get("fog_start").getAsFloat());
            assertEquals(1.0F, air.get("fog_end").getAsFloat());
            assertNull(airOnly.get("water"));
            JsonObject weather = airOnly.getAsJsonObject("weather");
            assertEquals("#3F3F4C", weather.get("fog_color").getAsString());
            assertEquals(0.23F, weather.get("fog_start").getAsFloat());
            assertEquals(0.7F, weather.get("fog_end").getAsFloat());
        });
    }

    @Test
    void manifestDeclaresPackMetadata() {
        GeyserMockContext.mockContext(context -> {
            setup(context);
            catalogue(Map.of("test:java_styled", definition("test:styled", true)));

            JsonObject manifest = readJsonEntries(CustomBiomeResourcePackManager.createResourcePack())
                .get("custom_biome_pack/manifest.json");
            assertNotNull(manifest);
            assertEquals("[\"pbr\"]", manifest.getAsJsonArray("capabilities").toString());
            assertEquals("[1,26,0]", manifest.getAsJsonObject("header").get("min_engine_version").getAsJsonArray().toString());
            assertEquals(1, manifest.getAsJsonArray("modules").size());
            assertNotEquals(manifest.getAsJsonObject("header").get("uuid").getAsString(),
                manifest.getAsJsonArray("modules").get(0).getAsJsonObject().get("uuid").getAsString());
        });
    }

    @Test
    void uuidsFollowThePackContent() {
        GeyserMockContext.mockContext(context -> {
            setup(context);
            catalogue(Map.of("test:java_styled", definition("test:styled", true)));

            UUID[] first = packUuids(CustomBiomeResourcePackManager.createResourcePack());
            // Every generation rewrites the pack; equal content must keep the identity
            UUID[] second = packUuids(CustomBiomeResourcePackManager.createResourcePack());
            assertEquals(first[0], second[0]);
            assertEquals(first[1], second[1]);

            // Changed content must change the identity, or clients would keep stale caches
            catalogue(Map.of("test:java_styled", definitionWithAppearance("test:styled",
                CustomBiomeAppearance.builder().skyColor(new Color(0x123456)))));
            UUID[] third = packUuids(CustomBiomeResourcePackManager.createResourcePack());
            assertNotEquals(first[0], third[0]);
            assertNotEquals(first[1], third[1]);
        });
    }

    @Test
    void withoutAppearancesNoPackIsGenerated() {
        GeyserMockContext.mockContext(context -> {
            setup(context);
            catalogue(Map.of("test:java_styled", definition("test:styled", true)));
            assertNotNull(CustomBiomeResourcePackManager.createResourcePack());

            // The pack from the earlier catalogue would go stale, so it is removed
            catalogue(Map.of("test:java_plain", definition("test:plain", false)));
            assertNull(CustomBiomeResourcePackManager.createResourcePack());
            assertFalse(Files.exists(configFolder.resolve("cache").resolve("custom_biomes.mcpack")));
        });
    }

    private void setup(GeyserMockContext context) {
        GeyserBootstrap bootstrap = context.mock(GeyserBootstrap.class);
        when(GeyserImpl.getInstance().getBootstrap()).thenReturn(bootstrap);
        when(bootstrap.getConfigFolder()).thenReturn(configFolder);
        when(bootstrap.getResourceOrThrow(any())).thenAnswer(invocation -> {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(invocation.<String>getArgument(0));
            assertNotNull(stream, "Missing resource " + invocation.getArgument(0));
            return stream;
        });
    }

    private static void catalogue(Map<String, CustomBiomeDefinition> javaToDefinition) {
        Map<Identifier, CustomBiomeDefinition> catalogue = new Object2ObjectOpenHashMap<>();
        javaToDefinition.forEach((javaIdentifier, definition) -> catalogue.put(Identifier.of(javaIdentifier), definition));
        Registries.CUSTOM_BIOMES.set(catalogue);
    }

    private static CustomBiomeDefinition definition(String bedrockIdentifier, boolean styled) {
        CustomBiomeDefinition.Builder builder = CustomBiomeDefinition.builder(Identifier.of(bedrockIdentifier));
        if (styled) {
            builder.appearance(CustomBiomeAppearance.builder()
                .skyColor(new Color(0x78A7FF))
                .waterSurfaceColor(new Color(0x00FFFF))
                .waterSurfaceOpacity(0.55F)
                .waterFogColor(new Color(0x050533))
                .waterFogEndDistance(48.0F)
                .grassColor(new Color(0x5F9F45))
                .foliageColor(new Color(0x4F8F3F))
                .dryFoliageColor(new Color(0x9E814D))
                .precipitation(CustomBiomePrecipitation.of(CustomBiomePrecipitation.Type.WHITE_ASH, 2.0F)));
        }
        return builder.build();
    }

    private static Map<String, JsonObject> readJsonEntries(Path pack) {
        Map<String, JsonObject> files = new HashMap<>();
        try (ZipFile zipFile = new ZipFile(pack.toFile())) {
            for (ZipEntry entry : zipFile.stream().toList()) {
                if (entry.getName().endsWith(".json")) {
                    try (InputStream stream = zipFile.getInputStream(entry)) {
                        files.put(entry.getName(), (JsonObject) JsonParser.parseReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8)));
                    }
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return files;
    }

    private static UUID[] packUuids(Path pack) {
        assertNotNull(pack);
        JsonObject manifest = readJsonEntries(pack).get("custom_biome_pack/manifest.json");
        assertNotNull(manifest);
        return new UUID[] {
            UUID.fromString(manifest.getAsJsonObject("header").get("uuid").getAsString()),
            UUID.fromString(manifest.getAsJsonArray("modules").get(0).getAsJsonObject().get("uuid").getAsString())
        };
    }

    private static CustomBiomeDefinition definitionWithAppearance(String bedrockIdentifier, CustomBiomeAppearance.Builder appearance) {
        return CustomBiomeDefinition.builder(Identifier.of(bedrockIdentifier))
            .appearance(appearance)
            .build();
    }
}
