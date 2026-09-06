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

package org.geysermc.geyser.registry.mappings;

import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomePrecipitation;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.biome.custom.GeyserCustomBiomeDefinition;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomBiomesLoaderTest {

    @Test
    void packBoundFileAttachesThePackAndRejectsAppearances() throws URISyntaxException {
        Path biomeConfigPath = getConfigResource("configuration/custom-biomes-packed.json");
        Map<Identifier, CustomBiomeDefinition> biomes = new HashMap<>();
        GeyserMockContext.mockContext(() -> {
            MappingsConfigReader.readCustomMappings(MappingsType.BIOMES, biomeConfigPath, biomes::put);

            // The styled biome fails: the named pack provides the visuals
            assertNull(biomes.get(Identifier.of("example:packed_styled")));
            assertEquals(2, biomes.size());
            UUID packUuid = UUID.fromString("8caa1b2a-0b23-4d55-9b46-8a3c2d9f0e11");
            for (CustomBiomeDefinition definition : biomes.values()) {
                assertNull(definition.appearance());
                assertEquals(packUuid, ((GeyserCustomBiomeDefinition) definition).packUuid());
            }
        });
    }

    @Test
    void malformedPackUuidFailsTheWholeFile() throws URISyntaxException {
        Path biomeConfigPath = getConfigResource("configuration/custom-biomes-bad-pack.json");
        Map<Identifier, CustomBiomeDefinition> biomes = new HashMap<>();
        GeyserMockContext.mockContext(() -> {
            MappingsConfigReader.readCustomMappings(MappingsType.BIOMES, biomeConfigPath, biomes::put);
            assertTrue(biomes.isEmpty());
        });
    }

    @Test
    void readMappings() throws URISyntaxException {
        Path biomeConfigPath = getConfigResource("configuration/custom-biomes.json");
        Map<Identifier, CustomBiomeDefinition> biomes = new HashMap<>();
        GeyserMockContext.mockContext(() -> {
            MappingsConfigReader.readCustomMappings(MappingsType.BIOMES, biomeConfigPath, biomes::put);
            assertMappings(biomes);
        });
    }

    private void assertMappings(Map<Identifier, CustomBiomeDefinition> biomes) {
        // The vanilla Bedrock namespace and malformed color entries are invalid and skipped
        assertEquals(6, biomes.size());
        // Java also allows three-float color arrays; those fail the biome instead of loading
        // without the color
        assertNull(biomes.get(Identifier.of("example:bad_color")));
        assertNull(biomes.get(Identifier.of("example:not_object")));

        // A bare bedrock_identifier lands in the geyser_custom namespace, like item mappings
        CustomBiomeDefinition bare = biomes.get(Identifier.of("example:bare_identifier"));
        assertNotNull(bare);
        assertEquals(Identifier.of("geyser_custom:bare_biome"), bare.bedrockIdentifier());

        // A vanilla Java biome may be overridden, as long as the Bedrock identifier is custom
        CustomBiomeDefinition swamp = biomes.get(Identifier.of("minecraft:swamp"));
        assertNotNull(swamp);
        assertEquals(Identifier.of("example:swamp_recolor"), swamp.bedrockIdentifier());

        // Without a geyser identifier, a Java identifier that is valid on Bedrock is reused
        CustomBiomeDefinition derived = biomes.get(Identifier.of("example:derived"));
        assertNotNull(derived);
        assertEquals(Identifier.of("example:derived"), derived.bedrockIdentifier());
        assertEquals(new Color(0x5f9f45), Objects.requireNonNull(derived.appearance()).grassColor());

        // A Java identifier Bedrock can't express becomes a digest in the reserved namespace
        CustomBiomeDefinition digest = biomes.get(Identifier.of("my_datapack:cave/derived"));
        assertNotNull(digest);
        assertEquals("geyser", digest.bedrockIdentifier().namespace());
        assertTrue(digest.bedrockIdentifier().path().startsWith("auto_"));
        // The geyser block still applies when it names no identifier
        assertEquals(Set.of("overworld"), digest.tags());

        CustomBiomeDefinition caves = biomes.get(Identifier.of("my_datapack:cave/crystal_caves"));
        assertNotNull(caves);
        assertEquals(Identifier.of("my_datapack:crystal_caves"), caves.bedrockIdentifier());
        assertEquals(Set.of("overworld", "monster"), caves.tags());

        CustomBiomeAppearance appearance = caves.appearance();
        assertNotNull(appearance);
        assertEquals(new Color(0x78a7ff), appearance.skyColor());
        assertEquals(new Color(12632256), appearance.fogColor());
        assertEquals(new Color(0x050533), appearance.waterFogColor());
        // The attribute is a modifier object, which is skipped; the geyser value applies instead
        assertEquals(48.0F, appearance.waterFogEndDistance());
        // The fixture value is #803f76e4; the alpha of #aarrggbb colors is ignored
        assertEquals(new Color(0x3f76e4), appearance.waterSurfaceColor());
        assertEquals(0.55F, appearance.waterSurfaceOpacity());
        assertEquals(new Color(0x5f9f45), appearance.grassColor());
        assertEquals(new Color(0x4f8f3f), appearance.foliageColor());
        assertEquals(new Color(0x9e814d), appearance.dryFoliageColor());

        CustomBiomePrecipitation precipitation = appearance.precipitation();
        assertNotNull(precipitation);
        assertEquals(CustomBiomePrecipitation.Type.BLUE_SPORES, precipitation.type());
        assertEquals(2.0F, precipitation.density());

        CustomBiomeDefinition minimal = biomes.get(Identifier.of("example:minimal"));
        assertNotNull(minimal);
        assertNull(minimal.appearance());
        assertTrue(minimal.tags().isEmpty());
    }

    private Path getConfigResource(String name) throws URISyntaxException {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(name), "No resource for name: " + name);
        return Path.of(url.toURI());
    }
}
