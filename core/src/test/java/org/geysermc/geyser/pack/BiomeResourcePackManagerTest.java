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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.registry.Registries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class BiomeResourcePackManagerTest {
    private static final String PLAINS = """
        "minecraft:plains": {
            "effects": {
                "water_color": "#3F76E4"
            }
        }
        """;
    private static final String CRYSTAL_CAVERNS = """
        "custom:crystal_caverns": {
            "attributes": {
                "minecraft:visual/sky_color": "#456789",
                "minecraft:visual/fog_color": "#345678",
                "minecraft:visual/water_fog_color": "#234567"
            },
            "effects": {
                "water_color": "#123456",
                "foliage_color": "#6789AB",
                "dry_foliage_color": "#6789AB",
                "grass_color": "#56789A"
            }
        }
        """;

    @TempDir
    Path tempDirectory;

    private static InputStream resource(String name) {
        return BiomeResourcePackManagerTest.class.getClassLoader().getResourceAsStream(name);
    }

    @AfterEach
    public void removeCustomMappings() {
        Registries.CUSTOM_BIOME_IDENTIFIERS.get().clear();
    }

    @Test
    public void generatesVisualsAndRegistersOnlyCustomBiomes() throws IOException {
        writeConfig(CRYSTAL_CAVERNS + "," + PLAINS);
        Path output = generatedPack();
        Object2IntMap<String> customMappings = Registries.CUSTOM_BIOME_IDENTIFIERS.get();

        assertEquals(30_000, customMappings.getInt("custom:crystal_caverns"));
        assertFalse(customMappings.containsKey("minecraft:plains"));
        try (ZipFile pack = new ZipFile(output.toFile())) {
            assertNotNull(pack.getEntry("biomes/custom_crystal_caverns.client_biome.json"));
            assertNotNull(pack.getEntry("fogs/custom_crystal_caverns_fog_setting.json"));
            assertNotNull(pack.getEntry("biomes/plains.client_biome.json"));
        }
    }

    @Test
    public void rejectsInvalidBiomeIdentifiers() throws IOException {
        assertInvalid("""
            {
                "format_version": 1,
                "biomes": { "Custom:invalid": { "effects": { "water_color": "#123456" } } }
            }
            """);
        assertInvalid("""
            {
                "format_version": 1,
                "biomes": {"plains": { "effects": { "water_color": "#123456" } } }
            }
            """);
    }

    @Test
    public void clearsMappingsWhenConfigIsRemoved() {
        Registries.CUSTOM_BIOME_IDENTIFIERS.get().put("custom:stale", 0);

        assertNull(createResourcePack());
        assertTrue(Registries.CUSTOM_BIOME_IDENTIFIERS.get().isEmpty());
    }

    @Test
    public void doesNotRegisterMappingsWhenPackWritingFails() throws IOException {
        writeConfig(CRYSTAL_CAVERNS);
        Files.createDirectories(tempDirectory.resolve("cache/GeyserBiomeVisuals.mcpack"));

        assertNull(createResourcePack());
        assertTrue(Registries.CUSTOM_BIOME_IDENTIFIERS.get().isEmpty());
    }

    private void writeConfig(String biomes) throws IOException {
        Path input = tempDirectory.resolve("biome-visuals.json");
        Files.writeString(input, """
            {"format_version": 1, "biomes": {%s}}
            """.formatted(biomes));
    }

    private void assertInvalid(String config) throws IOException {
        Path input = tempDirectory.resolve("biome-visuals.json");
        Files.writeString(input, config);
        assertNull(createResourcePack());
    }

    private @Nullable Path createResourcePack() {
        GeyserImpl geyser = mock(GeyserImpl.class);
        GeyserBootstrap bootstrap = mock(GeyserBootstrap.class);
        when(geyser.getBootstrap()).thenReturn(bootstrap);
        when(geyser.getLogger()).thenReturn(mock(GeyserLogger.class));
        when(bootstrap.getConfigFolder()).thenReturn(tempDirectory);
        when(bootstrap.getResourceOrThrow(anyString())).thenAnswer(invocation -> resource(invocation.getArgument(0)));

        try (MockedStatic<GeyserImpl> geyserInstance = mockStatic(GeyserImpl.class)) {
            geyserInstance.when(GeyserImpl::getInstance).thenReturn(geyser);
            if (!Registries.BIOMES.loaded()) {
                Registries.BIOMES.load();
            }
            if (!Registries.BIOME_IDENTIFIERS.loaded()) {
                Registries.BIOME_IDENTIFIERS.load();
            }
            return BiomeResourcePackManager.createResourcePack();
        }
    }

    private Path generatedPack() {
        return Objects.requireNonNull(createResourcePack());
    }
}
