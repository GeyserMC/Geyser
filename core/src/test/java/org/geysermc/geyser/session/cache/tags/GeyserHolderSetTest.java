package org.geysermc.geyser.session.cache.tags;

import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.scoreboard.network.util.GeyserMockContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.registry.JavaRegistryProvider;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GeyserHolderSetTest {

    private void runWithContext(TestRunner runner) {
        GeyserMockContext.mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            RegistryCache registryCache = context.mock(RegistryCache.class);
            TagCache tagCache = context.mock(TagCache.class);
            when(session.getRegistryCache()).thenReturn(registryCache);
            when(session.getTagCache()).thenReturn(tagCache);

            JavaRegistryKey.RegistryLookup<String> lookup = new JavaRegistryKey.RegistryLookup<>() {
                @Override
                public Optional<RegistryEntryData<String>> entry(JavaRegistryProvider registries, JavaRegistryKey<String> registry, int networkId) {
                    if (networkId == 100) {
                        return Optional.of(new RegistryEntryData<>(100, Key.key("minecraft:dialog_ref"), "ResolvedDialog"));
                    }
                    return Optional.empty();
                }

                @Override
                public Optional<RegistryEntryData<String>> entry(JavaRegistryProvider registries, JavaRegistryKey<String> registry, Key key) {
                    if (key.equals(Key.key("minecraft:dialog_ref"))) {
                        return Optional.of(new RegistryEntryData<>(100, Key.key("minecraft:dialog_ref"), "ResolvedDialog"));
                    }
                    return Optional.empty();
                }

                @Override
                public Optional<RegistryEntryData<String>> entry(JavaRegistryProvider registries, JavaRegistryKey<String> registry, String object) {
                    if ("ResolvedDialog".equals(object)) {
                        return Optional.of(new RegistryEntryData<>(100, Key.key("minecraft:dialog_ref"), "ResolvedDialog"));
                    }
                    return Optional.empty();
                }
            };

            JavaRegistryKey<String> registryKey = new JavaRegistryKey<>(Key.key("minecraft:dialog"), lookup);
            runner.run(session, registryKey, tagCache);
        });
    }

    @FunctionalInterface
    interface TestRunner {
        void run(GeyserSession session, JavaRegistryKey<String> registryKey, TagCache tagCache);
    }

    @Test
    void testEmptyHolderSet() {
        runWithContext((session, registryKey, tagCache) -> {
            GeyserHolderSet<String> set = GeyserHolderSet.readHolderSet(registryKey, null, key -> -1, null);
            assertNotNull(set);
            assertNull(set.getTag());
            assertEquals(0, set.getHolders().size());
            assertNull(set.getInline());
            assertNull(set.getEntries());
            assertTrue(set.resolve(session).isEmpty());
        });
    }

    @Test
    void testTagHolderSet() {
        runWithContext((session, registryKey, tagCache) -> {
            GeyserHolderSet<String> set = GeyserHolderSet.readHolderSet(registryKey, "#minecraft:all_dialogs", key -> -1, null);
            assertNotNull(set.getTag());
            assertEquals("minecraft", set.getTag().tag().namespace());
            assertEquals("all_dialogs", set.getTag().tag().value());
            assertNull(set.getHolders());
            assertNull(set.getInline());
            assertNull(set.getEntries());
        });
    }

    @Test
    void testSingleStringHolderSet() {
        runWithContext((session, registryKey, tagCache) -> {
            ToIntFunction<Key> idMapper = key -> key.asString().equals("minecraft:dialog_ref") ? 100 : -1;
            GeyserHolderSet<String> set = GeyserHolderSet.readHolderSet(registryKey, "minecraft:dialog_ref", idMapper, null);
            assertNull(set.getTag());
            assertNotNull(set.getHolders());
            assertEquals(IntList.of(100), set.getHolders());
            assertNull(set.getInline());
            assertNull(set.getEntries());
            assertEquals(List.of("ResolvedDialog"), set.resolve(session));
        });
    }

    @Test
    void testSingleInlineHolderSet() {
        runWithContext((session, registryKey, tagCache) -> {
            NbtMap inlineNbt = NbtMap.builder().putString("title", "Inline 1").build();
            Function<NbtMap, String> reader = map -> map.getString("title");

            GeyserHolderSet<String> set = GeyserHolderSet.readHolderSet(registryKey, inlineNbt, key -> -1, reader);
            assertNull(set.getTag());
            assertNull(set.getHolders());
            assertNotNull(set.getInline());
            assertEquals(List.of("Inline 1"), set.getInline());
            assertNull(set.getEntries());
            assertEquals(List.of("Inline 1"), set.resolve(session));
            assertThrows(IllegalStateException.class, () -> set.resolveRaw(tagCache));
        });
    }

    @Test
    void testHomogeneousStringListHolderSet() {
        runWithContext((session, registryKey, tagCache) -> {
            ToIntFunction<Key> idMapper = key -> 100;
            GeyserHolderSet<String> set = GeyserHolderSet.readHolderSet(registryKey, List.of("minecraft:dialog_ref"), idMapper, null);
            assertNotNull(set.getHolders());
            assertEquals(IntList.of(100), set.getHolders());
            assertNull(set.getInline());
            assertNull(set.getEntries());
            assertEquals(List.of("ResolvedDialog"), set.resolve(session));
        });
    }

    @Test
    void testHomogeneousInlineListHolderSet() {
        runWithContext((session, registryKey, tagCache) -> {
            NbtMap nbt1 = NbtMap.builder().putString("title", "Inline 1").build();
            NbtMap nbt2 = NbtMap.builder().putString("title", "Inline 2").build();
            Function<NbtMap, String> reader = map -> map.getString("title");

            GeyserHolderSet<String> set = GeyserHolderSet.readHolderSet(registryKey, List.of(nbt1, nbt2), key -> -1, reader);
            assertNull(set.getHolders());
            assertNotNull(set.getInline());
            assertEquals(List.of("Inline 1", "Inline 2"), set.getInline());
            assertNull(set.getEntries());
            assertEquals(List.of("Inline 1", "Inline 2"), set.resolve(session));
        });
    }

    @Test
    void testHeterogeneousListHolderSet() {
        runWithContext((session, registryKey, tagCache) -> {
            NbtMap inline1 = NbtMap.builder().putString("title", "Inline First").build();
            String ref = "minecraft:dialog_ref";
            NbtMap inline2 = NbtMap.builder().putString("title", "Inline Last").build();

            ToIntFunction<Key> idMapper = key -> key.asString().equals("minecraft:dialog_ref") ? 100 : -1;
            Function<NbtMap, String> reader = map -> map.getString("title");

            List<Object> mixedList = List.of(inline1, ref, inline2);
            GeyserHolderSet<String> set = GeyserHolderSet.readHolderSet(registryKey, mixedList, idMapper, reader);

            assertNull(set.getHolders());
            assertNull(set.getInline());
            assertNotNull(set.getEntries());
            assertEquals(3, set.getEntries().size());

            assertInstanceOf(GeyserHolderSet.HolderEntry.Direct.class, set.getEntries().get(0));
            assertInstanceOf(GeyserHolderSet.HolderEntry.Id.class, set.getEntries().get(1));
            assertInstanceOf(GeyserHolderSet.HolderEntry.Direct.class, set.getEntries().get(2));

            // Test resolution preserves exact sequence
            List<String> resolved = set.resolve(session);
            assertEquals(List.of("Inline First", "ResolvedDialog", "Inline Last"), resolved);

            // Test resolveRaw throws
            assertThrows(IllegalStateException.class, () -> set.resolveRaw(tagCache));

            // Test contains
            assertTrue(set.contains(session, "Inline First"));
            assertTrue(set.contains(session, "ResolvedDialog"));
            assertTrue(set.contains(session, "Inline Last"));
            assertFalse(set.contains(session, "NonExistentDialog"));
        });
    }
}
