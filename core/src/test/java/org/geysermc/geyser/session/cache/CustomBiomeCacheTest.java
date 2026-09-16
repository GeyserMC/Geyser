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

package org.geysermc.geyser.session.cache;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BiomeDefinitionListPacket;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.level.JavaBiome;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;
import org.geysermc.geyser.session.cache.registry.SimpleJavaRegistry;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomBiomeCacheTest {
    private static final int UNKNOWN_BIOME = -1;

    @BeforeAll
    public static void loadBiomeDefinitions() {
        GeyserMockContext.mockContext(context -> {
            GeyserBootstrap bootstrap = context.mock(GeyserBootstrap.class);
            when(GeyserImpl.getInstance().getBootstrap()).thenReturn(bootstrap);
            when(bootstrap.getResourceOrThrow(any())).thenAnswer(invocation -> CustomBiomeCacheTest.class
                .getClassLoader().getResourceAsStream(invocation.getArgument(0)));
            Registries.BIOMES.load();
            Registries.BIOME_IDENTIFIERS.load();
        });
    }

    @AfterEach
    public void clearCatalogue() {
        Registries.CUSTOM_BIOMES.set(new Object2ObjectOpenHashMap<>());
    }

    @Test
    void readsClimateFromTheServerRegistry() {
        // A modded biome has no vanilla Bedrock id; its climate comes from the registry NBT
        RegistryEntry modded = new RegistryEntry(MinecraftKey.key("test:java_biome"), NbtMap.builder()
            .putFloat("temperature", 1.9F)
            .putFloat("downfall", 0.15F)
            .putBoolean("has_precipitation", false)
            .build());
        JavaBiome biome = BiomeTranslator.loadServerBiome(new RegistryEntryContext(modded, key -> -1, Optional.empty()));
        assertEquals(UNKNOWN_BIOME, biome.bedrockId());
        assertEquals(1.9F, biome.temperature());
        assertEquals(0.15F, biome.downfall());
        assertFalse(biome.hasPrecipitation());

        RegistryEntry vanilla = new RegistryEntry(MinecraftKey.key("minecraft:plains"), NbtMap.EMPTY);
        assertNotEquals(UNKNOWN_BIOME, BiomeTranslator.loadServerBiome(
            new RegistryEntryContext(vanilla, key -> -1, Optional.empty())).bedrockId());
    }

    @Test
    void commitsPendingOnLogin() {
        GeyserMockContext.mockContext(() -> {
            catalogue(Map.of("test:java_biome", "test:bedrock_biome"));
            TestSession session = new TestSession("minecraft:plains", "test:java_biome");

            session.cache.reconcile();
            // Nothing is active until the login definitions are handed over for sending
            assertEquals(UNKNOWN_BIOME, session.biomeValue("test:java_biome"));

            // The spawn packet precedes the commit in production
            session.spawned.set(true);
            BiomeDefinitions definitions = session.cache.loginDefinitions();
            BiomeDefinitionData data = definitions.getDefinitions().get("test:bedrock_biome");
            assertNotNull(data);
            int id = session.biomeValue("test:java_biome");
            assertTrue(id >= 30000 && id <= 32767);
            assertEquals(id, data.getId());
            // Climate comes from the Java biome the server sent; the test values match no
            // vanilla definition, so a copied vanilla reference would fail here
            assertEquals(1.9F, data.getTemperature());
            assertEquals(0.15F, data.getDownfall());
            assertFalse(data.isRain());
            assertTrue(session.packets.isEmpty());
        });
    }

    @Test
    void committedLoginStateCarriesIntoTransfers() {
        GeyserMockContext.mockContext(() -> {
            catalogue(Map.of("test:java_biome", "test:bedrock_biome", "test:java_extra", "test:bedrock_extra"));
            TestSession session = new TestSession("minecraft:plains", "test:java_biome");
            session.cache.reconcile();
            session.spawned.set(true);
            int id = session.cache.loginDefinitions().getDefinitions().get("test:bedrock_biome").getId();
            assertTrue(session.packets.isEmpty());

            // A transfer that adds a second custom biome resends: the committed definition
            // keeps its id and the new one continues the sequence
            session.reset(new JavaBiome(UNKNOWN_BIOME, 1.9F, 0.15F, false), "minecraft:plains", "test:java_biome", "test:java_extra");
            session.cache.reconcile();
            assertEquals(1, session.packets.size());
            assertEquals(id, session.biomeValue("test:java_biome"));
            BiomeDefinitions resent = ((BiomeDefinitionListPacket) session.packets.getFirst()).getBiomes();
            assertEquals(id, resent.getDefinitions().get("test:bedrock_biome").getId());
            assertEquals(id + 1, resent.getDefinitions().get("test:bedrock_extra").getId());
        });
    }

    @Test
    void newConfigurationPhaseDiscardsPending() {
        GeyserMockContext.mockContext(() -> {
            catalogue(Map.of("test:java_biome", "test:bedrock_biome"));
            TestSession session = new TestSession("minecraft:plains", "test:java_biome");
            session.cache.reconcile();

            // Starting a new configuration phase discards the candidate; a form can spawn
            // the client before the phase ends, and the commit must not send the stale one
            session.cache.discardPending();
            session.spawned.set(true);
            assertNull(session.cache.loginDefinitions().getDefinitions().get("test:bedrock_biome"));

            // The new phase's registry applies with its own climate
            session.reset(new JavaBiome(UNKNOWN_BIOME, 0.2F, 0.9F, true), "minecraft:plains", "test:java_biome");
            session.cache.reconcile();
            assertEquals(1, session.packets.size());
            assertTrue(session.biomeValue("test:java_biome") >= 30000);
            BiomeDefinitions sent = ((BiomeDefinitionListPacket) session.packets.getFirst()).getBiomes();
            assertEquals(0.2F, sent.getDefinitions().get("test:bedrock_biome").getTemperature());
        });
    }

    @Test
    void spawnedSessionResendsOnlyWhenDefinitionsAreAdded() {
        GeyserMockContext.mockContext(() -> {
            catalogue(Map.of("test:java_biome", "test:bedrock_biome", "test:java_extra", "test:bedrock_extra"));
            TestSession session = new TestSession("minecraft:plains", "test:java_biome");
            session.spawned.set(true);

            session.cache.reconcile();
            assertEquals(1, session.packets.size());
            int id = session.biomeValue("test:java_biome");
            assertTrue(id >= 30000);

            // The same registry again: mapping is reinstalled, but no redundant packet is sent
            session.reset("minecraft:plains", "test:java_biome");
            session.cache.reconcile();
            assertEquals(1, session.packets.size());
            assertEquals(id, session.biomeValue("test:java_biome"));

            // A backend with one more custom biome: the complete union is resent, and the
            // definition sent earlier keeps its id
            session.reset("minecraft:plains", "test:java_biome", "test:java_extra");
            session.cache.reconcile();
            assertEquals(2, session.packets.size());
            assertEquals(id, session.biomeValue("test:java_biome"));
            assertTrue(session.biomeValue("test:java_extra") >= 30000);
            BiomeDefinitions resent = ((BiomeDefinitionListPacket) session.packets.getLast()).getBiomes();
            assertEquals(id, resent.getDefinitions().get("test:bedrock_biome").getId());
            assertNotNull(resent.getDefinitions().get("test:bedrock_extra"));
        });
    }

    @Test
    void unusableClimateFallsBack() {
        GeyserMockContext.mockContext(() -> {
            catalogue(Map.of("test:java_biome", "test:bedrock_biome"));
            TestSession session = new TestSession("minecraft:plains", "test:java_biome");
            session.spawned.set(true);
            session.cache.reconcile();
            assertTrue(session.biomeValue("test:java_biome") >= 30000);

            // A later backend has the same biome with different climate; the sent definition
            // must keep its meaning, so this backend's biome uses the vanilla fallback
            session.reset(new JavaBiome(UNKNOWN_BIOME, 0.2F, 0.9F, true), "minecraft:plains", "test:java_biome");
            session.cache.reconcile();
            assertEquals(1, session.packets.size());
            assertEquals(UNKNOWN_BIOME, session.biomeValue("test:java_biome"));

            // Non-finite climate from a malformed backend is never sent
            TestSession invalid = new TestSession();
            invalid.reset(new JavaBiome(UNKNOWN_BIOME, Float.NaN, 0.0F, false), "test:java_biome");
            invalid.spawned.set(true);
            invalid.cache.reconcile();
            assertTrue(invalid.packets.isEmpty());
            assertEquals(UNKNOWN_BIOME, invalid.biomeValue("test:java_biome"));
        });
    }

    @Test
    void definitionsOverTheStringPoolCapFallBack() {
        GeyserMockContext.mockContext(() -> {
            Map<String, BiomeDefinitionData> vanilla = Registries.BIOMES.get().getDefinitions();
            Set<String> vanillaPool = new HashSet<>();
            vanilla.forEach((name, data) -> {
                vanillaPool.add(name);
                if (data.getTags() != null) {
                    vanillaPool.addAll(data.getTags());
                }
            });
            // Admission stops when the 1024-entry string pool is full; every definition
            // pools its name, so this bounds the definition count too
            int fits = 1024 - vanillaPool.size();

            Map<Identifier, CustomBiomeDefinition> catalogue = new Object2ObjectOpenHashMap<>();
            String[] javaIdentifiers = new String[fits + 1];
            for (int i = 0; i < fits + 1; i++) {
                javaIdentifiers[i] = "test:java_%04d".formatted(i);
                catalogue.put(Identifier.of(javaIdentifiers[i]), definition("test:bedrock_%04d".formatted(i)));
            }
            Registries.CUSTOM_BIOMES.set(catalogue);

            TestSession session = new TestSession(javaIdentifiers);
            session.spawned.set(true);
            session.cache.reconcile();

            int applied = 0;
            for (String javaIdentifier : javaIdentifiers) {
                if (session.biomeValue(javaIdentifier) != UNKNOWN_BIOME) {
                    applied++;
                }
            }
            assertEquals(fits, applied);

            BiomeDefinitions sent = ((BiomeDefinitionListPacket) session.packets.getFirst()).getBiomes();
            assertEquals(vanilla.size() + fits, sent.getDefinitions().size());
        });
    }

    private static void catalogue(Map<String, String> javaToBedrock) {
        Map<Identifier, CustomBiomeDefinition> catalogue = new Object2ObjectOpenHashMap<>();
        javaToBedrock.forEach((javaIdentifier, bedrockIdentifier) ->
            catalogue.put(Identifier.of(javaIdentifier), definition(bedrockIdentifier)));
        Registries.CUSTOM_BIOMES.set(catalogue);
    }

    private static CustomBiomeDefinition definition(String bedrockIdentifier) {
        return CustomBiomeDefinition.builder(Identifier.of(bedrockIdentifier)).build();
    }

    /**
     * A mocked session with a real biome registry, tracking sent packets and the spawn state
     * the same way the login flow does.
     */
    private static class TestSession {
        private final GeyserSession session = mock(GeyserSession.class);
        private final SimpleJavaRegistry<JavaBiome> registry = new SimpleJavaRegistry<>();
        private final List<BedrockPacket> packets = new ArrayList<>();
        private final AtomicBoolean spawned = new AtomicBoolean();
        private final CustomBiomeCache cache;

        TestSession(String... javaIdentifiers) {
            reset(javaIdentifiers);
            RegistryCache registryCache = mock(RegistryCache.class);
            when(session.getRegistryCache()).thenReturn(registryCache);
            when(registryCache.registry(JavaRegistries.BIOME)).thenReturn(registry);
            when(session.isSentSpawnPacket()).thenAnswer(invocation -> spawned.get());
            GeyserImpl geyser = GeyserImpl.getInstance();
            when(session.getGeyser()).thenReturn(geyser);
            doAnswer(invocation -> {
                packets.add(invocation.getArgument(0));
                return null;
            }).when(session).sendUpstreamPacket(any());
            this.cache = new CustomBiomeCache(session);
        }

        /**
         * Loads a fresh registry epoch, like a configuration phase does. Every entry starts
         * unknown, with climate no vanilla definition has, so tests can tell values derived
         * from the Java registry apart from copied vanilla ones.
         */
        void reset(String... javaIdentifiers) {
            reset(new JavaBiome(UNKNOWN_BIOME, 1.9F, 0.15F, false), javaIdentifiers);
        }

        void reset(JavaBiome data, String... javaIdentifiers) {
            List<RegistryEntryData<JavaBiome>> entries = new ArrayList<>();
            for (int i = 0; i < javaIdentifiers.length; i++) {
                entries.add(new RegistryEntryData<>(i, MinecraftKey.key(javaIdentifiers[i]), data));
            }
            registry.reset(entries);
        }

        int biomeValue(String javaIdentifier) {
            for (RegistryEntryData<JavaBiome> entry : registry.entries()) {
                if (entry.key().asString().equals(javaIdentifier)) {
                    return entry.data().bedrockId();
                }
            }
            throw new IllegalArgumentException(javaIdentifier + " is not in the registry");
        }
    }
}
