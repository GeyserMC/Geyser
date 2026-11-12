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

package org.geysermc.geyser.registry.populator;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinitionRegisterException;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBiomesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.event.GeyserEventBus;
import org.geysermc.geyser.level.JavaBiome;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.SimpleJavaRegistry;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomBiomeRegistryPopulatorTest {

    @TempDir
    Path configFolder;

    @AfterEach
    public void clearCatalogue() {
        Registries.CUSTOM_BIOMES.set(new Object2ObjectOpenHashMap<>());
    }

    @Test
    void registrationClosesAfterStartup() {
        GeyserMockContext.mockContext(context -> {
            GeyserImpl geyser = GeyserImpl.getInstance();
            GeyserBootstrap bootstrap = context.mock(GeyserBootstrap.class);
            when(geyser.getBootstrap()).thenReturn(bootstrap);
            when(bootstrap.getConfigFolder()).thenReturn(configFolder);
            GeyserConfig.GameplayConfig gameplay = context.mock(GeyserConfig.GameplayConfig.class);
            when(context.mockOrSpy(GeyserConfig.class).gameplay()).thenReturn(gameplay);
            when(gameplay.enableCustomContent()).thenReturn(true);
            GeyserEventBus eventBus = geyser.eventBus();
            when(geyser.getEventBus()).thenReturn(eventBus);

            GeyserDefineCustomBiomesEvent[] captured = new GeyserDefineCustomBiomesEvent[1];

            // With custom content disabled, the event never fires and the catalogue is empty
            when(gameplay.enableCustomContent()).thenReturn(false);
            geyser.eventBus().subscribe(geyser, GeyserDefineCustomBiomesEvent.class, event -> captured[0] = event);
            CustomBiomeRegistryPopulator.populate();
            assertNull(captured[0]);
            assertEquals(0, Registries.CUSTOM_BIOMES.get().size());
            when(gameplay.enableCustomContent()).thenReturn(true);

            geyser.eventBus().subscribe(geyser, GeyserDefineCustomBiomesEvent.class, event -> {
                captured[0] = event;
                event.register(Identifier.of("test:java_biome"),
                    CustomBiomeDefinition.builder(Identifier.of("test:bedrock_biome")).build());
                // Definitions must come from the API builder
                assertThrows(CustomBiomeDefinitionRegisterException.class, () -> event.register(
                    Identifier.of("test:foreign"), mock(CustomBiomeDefinition.class)));
                // Java and Bedrock identifiers may each only be registered once
                assertThrows(CustomBiomeDefinitionRegisterException.class, () -> event.register(
                    Identifier.of("test:java_biome"), CustomBiomeDefinition.builder(Identifier.of("test:other_biome")).build()));
                assertThrows(CustomBiomeDefinitionRegisterException.class, () -> event.register(
                    Identifier.of("test:other_java"), CustomBiomeDefinition.builder(Identifier.of("test:bedrock_biome")).build()));
            });

            CustomBiomeRegistryPopulator.populate();

            assertEquals(1, Registries.CUSTOM_BIOMES.get().size());
            // Both the published catalogue and the retained event are frozen after startup
            assertThrows(UnsupportedOperationException.class, () -> Registries.CUSTOM_BIOMES.get().put(
                Identifier.of("test:late"), CustomBiomeDefinition.builder(Identifier.of("test:late_biome")).build()));
            assertThrows(CustomBiomeDefinitionRegisterException.class, () -> captured[0].register(
                Identifier.of("test:late"), CustomBiomeDefinition.builder(Identifier.of("test:late_biome")).build()));
        });
    }

    @Test
    void registersServerBiomes() {
        GeyserMockContext.mockContext(context -> {
            GeyserImpl geyser = GeyserImpl.getInstance();
            GeyserConfig.GameplayConfig gameplay = context.mock(GeyserConfig.GameplayConfig.class);
            when(context.mockOrSpy(GeyserConfig.class).gameplay()).thenReturn(gameplay);
            when(gameplay.enableCustomContent()).thenReturn(true);

            CustomBiomeAppearance appearance = CustomBiomeAppearance.builder().skyColor(new Color(0x78a7ff)).build();
            SimpleJavaRegistry<JavaBiome> registry = new SimpleJavaRegistry<>(JavaRegistries.BIOME);
            registry.register(0, MinecraftKey.key("minecraft:plains"), new JavaBiome(1, 0.8F, 0.4F, true, null));
            registry.register(1, MinecraftKey.key("test:mapped"), new JavaBiome(BiomeTranslator.UNKNOWN_BIOME, 0.8F, 0.4F, true, appearance));
            registry.register(2, MinecraftKey.key("test:styled"), new JavaBiome(BiomeTranslator.UNKNOWN_BIOME, 0.8F, 0.4F, true, appearance));
            registry.register(3, MinecraftKey.key("test:cave/deep"), new JavaBiome(BiomeTranslator.UNKNOWN_BIOME, 0.8F, 0.4F, true, null));
            registry.freeze();
            JavaRegistryProvider registries = context.mock(JavaRegistryProvider.class);
            when(registries.registry(JavaRegistries.BIOME)).thenAnswer(invocation -> registry);
            WorldManager worldManager = context.mock(WorldManager.class);
            when(worldManager.getGlobalJavaRegistries()).thenReturn(registries);
            when(geyser.getWorldManager()).thenReturn(worldManager);

            CustomBiomeDefinition mapped = CustomBiomeDefinition.builder(Identifier.of("test:mapped_bedrock")).build();
            Registries.CUSTOM_BIOMES.set(Map.of(Identifier.of("test:mapped"), mapped));

            when(gameplay.enableCustomContent()).thenReturn(false);
            CustomBiomeRegistryPopulator.populateFromServer();
            assertEquals(1, Registries.CUSTOM_BIOMES.get().size());
            when(gameplay.enableCustomContent()).thenReturn(true);

            // Running it again, as a reload does, must not change the result
            for (int i = 0; i < 2; i++) {
                CustomBiomeRegistryPopulator.populateFromServer();

                Map<Identifier, CustomBiomeDefinition> catalogue = Registries.CUSTOM_BIOMES.get();
                assertEquals(3, catalogue.size());
                // Vanilla biomes are left alone, and existing definitions win
                assertNull(catalogue.get(Identifier.of("minecraft:plains")));
                assertSame(mapped, catalogue.get(Identifier.of("test:mapped")));

                CustomBiomeDefinition styled = catalogue.get(Identifier.of("test:styled"));
                assertEquals(Identifier.of("test:styled"), styled.bedrockIdentifier());
                assertEquals(appearance, styled.appearance());

                CustomBiomeDefinition cave = catalogue.get(Identifier.of("test:cave/deep"));
                assertEquals("geyser", cave.bedrockIdentifier().namespace());
                assertNull(cave.appearance());
            }
        });
    }
}
