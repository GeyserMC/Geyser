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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinitionRegisterException;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBiomesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.biome.custom.GeyserCustomBiomeDefinition;
import org.geysermc.geyser.level.JavaBiome;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.mappings.MappingsConfigReader;
import org.geysermc.geyser.registry.mappings.MappingsType;
import org.geysermc.geyser.translator.level.BiomeTranslator;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CustomBiomeRegistryPopulator {

    public static void populate() {
        if (!GeyserImpl.getInstance().config().gameplay().enableCustomContent()) {
            Registries.CUSTOM_BIOMES.set(Map.of());
            return;
        }

        DefineCustomBiomesEvent event = new DefineCustomBiomesEvent();
        try {
            MappingsConfigReader.loadCustomMappingsFromJson(MappingsType.BIOMES, event::register);
            GeyserImpl.getInstance().getEventBus().fire(event);
        } finally {
            // The catalogue must not change after sessions and pack generation start reading it
            event.closed = true;
        }
        Registries.CUSTOM_BIOMES.set(Map.copyOf(event.definitions));

        if (!event.definitions.isEmpty()) {
            GeyserImpl.getInstance().getLogger().info("Registered " + event.definitions.size() + " custom biome mappings");
        }
    }

    /**
     * Registers the server's biomes that have no vanilla Bedrock biome and no definition
     * yet, with the appearance read from their registry data. Runs when Geyser starts,
     * before the resource pack is generated. Platforms that can't read the server's
     * registries, such as proxies, have none to register.
     */
    public static void populateFromServer() {
        if (!GeyserImpl.getInstance().config().gameplay().enableCustomContent()) {
            return;
        }

        Map<Identifier, CustomBiomeDefinition> definitions = new Object2ObjectOpenHashMap<>(Registries.CUSTOM_BIOMES.get());
        Set<Identifier> bedrockIdentifiers = new ObjectOpenHashSet<>();
        definitions.values().forEach(definition -> bedrockIdentifiers.add(definition.bedrockIdentifier()));
        int registered = definitions.size();

        GeyserImpl.getInstance().getWorldManager().getGlobalJavaRegistries().registry(JavaRegistries.BIOME).forEachEntry(entry -> {
            Identifier javaIdentifier = Identifier.of(entry.key().asString());
            JavaBiome biome = entry.data();
            if (biome.bedrockId() != BiomeTranslator.UNKNOWN_BIOME || definitions.containsKey(javaIdentifier)) {
                return;
            }

            GeyserCustomBiomeDefinition.Builder builder = GeyserCustomBiomeDefinition.derivedBuilder(javaIdentifier);
            if (biome.appearance() != null) {
                builder.appearance(biome.appearance());
            }
            CustomBiomeDefinition definition = builder.build();
            if (bedrockIdentifiers.add(definition.bedrockIdentifier())) {
                definitions.put(javaIdentifier, definition);
            }
        });

        if (definitions.size() > registered) {
            Registries.CUSTOM_BIOMES.set(Map.copyOf(definitions));
            GeyserImpl.getInstance().getLogger().info("Registered " + (definitions.size() - registered) + " custom biomes from the server's registries");
        }
    }

    private static class DefineCustomBiomesEvent implements GeyserDefineCustomBiomesEvent {
        private final Map<Identifier, CustomBiomeDefinition> definitions = new Object2ObjectOpenHashMap<>();
        private final Set<Identifier> bedrockIdentifiers = new ObjectOpenHashSet<>();
        private boolean closed;

        @Override
        public Map<Identifier, CustomBiomeDefinition> customBiomeDefinitions() {
            return Collections.unmodifiableMap(definitions);
        }

        @Override
        public void register(Identifier javaIdentifier, CustomBiomeDefinition definition) {
            Objects.requireNonNull(javaIdentifier, "javaIdentifier may not be null");
            Objects.requireNonNull(definition, "definition may not be null");
            if (closed) {
                throw new CustomBiomeDefinitionRegisterException(
                    "Custom biomes can only be registered while the event is being fired");
            }
            if (!(definition instanceof GeyserCustomBiomeDefinition)) {
                throw new CustomBiomeDefinitionRegisterException(
                    "The definition for " + javaIdentifier + " was not created with CustomBiomeDefinition.builder()");
            }
            if (definitions.containsKey(javaIdentifier)) {
                throw new CustomBiomeDefinitionRegisterException(
                    "A custom biome is already registered for " + javaIdentifier);
            }
            if (!bedrockIdentifiers.add(definition.bedrockIdentifier())) {
                throw new CustomBiomeDefinitionRegisterException(
                    "A custom biome definition is already registered as " + definition.bedrockIdentifier());
            }
            definitions.put(javaIdentifier, definition);
        }
    }
}
