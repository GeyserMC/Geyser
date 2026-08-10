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

package org.geysermc.geyser.translator.level;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;
import org.geysermc.geyser.session.cache.registry.SimpleJavaRegistry;
import org.geysermc.geyser.translator.level.BiomeTranslator.BiomeMapping;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class BiomeTranslatorTest {
    private static final int CUSTOM_BIOME_ID = 30_000;

    @BeforeAll
    public static void loadRegistries() {
        GeyserImpl geyser = mock(GeyserImpl.class);
        GeyserBootstrap bootstrap = mock(GeyserBootstrap.class);
        when(geyser.getBootstrap()).thenReturn(bootstrap);
        when(bootstrap.getResourceOrThrow(anyString())).thenAnswer(invocation -> resource(invocation.getArgument(0)));

        try (var geyserInstance = mockStatic(GeyserImpl.class)) {
            geyserInstance.when(GeyserImpl::getInstance).thenReturn(geyser);
            if (!Registries.BIOMES.loaded()) {
                Registries.BIOMES.load();
            }
            if (!Registries.BIOME_IDENTIFIERS.loaded()) {
                Registries.BIOME_IDENTIFIERS.load();
            }
        }
    }

    @AfterEach
    public void removeCustomMapping() {
        Registries.CUSTOM_BIOME_IDENTIFIERS.get().clear();
    }

    @Test
    public void builtInAndUnknownBiomesDoNotReadRegistryData() {
        BiomeMapping builtIn = BiomeTranslator.loadServerBiome(context("minecraft:plains", null));
        BiomeMapping unknown = BiomeTranslator.loadServerBiome(context("custom:unknown", null));

        assertEquals(Registries.BIOME_IDENTIFIERS.get().getInt("minecraft:plains"), builtIn.bedrockId());
        assertNull(builtIn.customDefinition());
        assertEquals(-1, unknown.bedrockId());
        assertNull(unknown.customDefinition());
    }

    @Test
    public void configuredBiomeUsesCustomMappingAndJavaClimateData() {
        String identifier = "minecraft:end_highlands";
        NbtMap data = NbtMap.builder()
                .putFloat("temperature", 0.25F)
                .putFloat("downfall", 0.75F)
                .putBoolean("has_precipitation", true)
                .putCompound("effects", NbtMap.builder().putInt("water_color", 0x123456).build())
                .build();

        Registries.CUSTOM_BIOME_IDENTIFIERS.get().put(identifier, CUSTOM_BIOME_ID);
        BiomeMapping mapping = BiomeTranslator.loadServerBiome(context(identifier, data));
        BiomeDefinitionData definition = mapping.customDefinition();

        assertEquals(CUSTOM_BIOME_ID, mapping.bedrockId());
        assertNotEquals(Registries.BIOME_IDENTIFIERS.get().getInt(identifier), mapping.bedrockId());
        assertEquals(CUSTOM_BIOME_ID, definition.getId());
        assertEquals(0.25F, definition.getTemperature());
        assertEquals(0.75F, definition.getDownfall());
        assertTrue(definition.isRain());
        assertEquals(0x123456, definition.getMapWaterColor().getRGB() & 0xFFFFFF);
    }

    @Test
    public void customDefinitionsAreAppendedToVanillaDefinitions() {
        BiomeDefinitionData custom = definition(CUSTOM_BIOME_ID, true);
        SimpleJavaRegistry<BiomeMapping> registry = new SimpleJavaRegistry<>();
        registry.reset(List.of(
                new RegistryEntryData<>(0, Key.key("minecraft:plains"), new BiomeMapping(1, null)),
                new RegistryEntryData<>(1, Key.key("custom:crystal_caverns"),
                        new BiomeMapping(CUSTOM_BIOME_ID, custom))));

        BiomeDefinitions definitions = BiomeTranslator.bedrockBiomeDefinitions(registry);
        BiomeDefinitionData vanilla = Registries.BIOMES.get().getDefinitions().get("minecraft:plains");

        assertEquals(Registries.BIOMES.get().getDefinitions().size() + 1, definitions.getDefinitions().size());
        assertSame(vanilla, definitions.getDefinitions().get("minecraft:plains"));
        assertSame(custom, definitions.getDefinitions().get("custom:crystal_caverns"));
    }

    private static RegistryEntryContext context(String identifier, @Nullable NbtMap data) {
        return new RegistryEntryContext(new RegistryEntry(Key.key(identifier), data), key -> -1, Optional.empty());
    }

    private static BiomeDefinitionData definition(@Nullable Integer id, boolean rain) {
        return new BiomeDefinitionData(id, 0, 0, 0, 0, 0, new Color(0), rain, List.of(), null);
    }

    private static InputStream resource(String name) {
        return BiomeTranslatorTest.class.getClassLoader().getResourceAsStream(name);
    }
}
