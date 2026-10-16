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

package org.geysermc.geyser.session.cache.registry;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistry;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.MutableJavaRegistry;
import org.geysermc.geyser.registry.java.SimpleJavaRegistry;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;
import org.geysermc.geyser.registry.java.reader.KeyDependentJavaRegistryReader;
import org.geysermc.geyser.registry.java.reader.RegistryEntryContext;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores any information sent via Java registries. May not contain all data in a given registry - we'll strip what's
 * unneeded.
 *
 * <p>Crafted as of 1.20.5 for easy "add new registry" functionality in the future.</p>
 */
final class RegistryCache {
    private static final Map<JavaRegistryKey<?>, Map<Key, NbtMap>> DEFAULTS;

    static {
        // Load from MCProtocolLib's classloader
        NbtMap tag = MinecraftProtocol.loadNetworkCodec();
        Map<JavaRegistryKey<?>, Map<Key, NbtMap>> defaults = new HashMap<>();
        // Don't create a keySet - no need to create the cached object in HashMap if we don't use it again
        JavaRegistryReaders.networkRegistries().forEach(key -> {
            List<NbtMap> rawValues = tag.getCompound(key.registryKey().asString()).getList("value", NbtType.COMPOUND);
            Map<Key, NbtMap> values = new HashMap<>();
            for (NbtMap value : rawValues) {
                Key name = MinecraftKey.key(value.getString("name"));
                values.put(name, value.getCompound("element"));
            }
            // Can make these maps immutable and as efficient as possible after initialization
            defaults.put(key, Map.copyOf(values));
        });

        DEFAULTS = Map.copyOf(defaults);
    }

    private final Reference2ObjectMap<JavaRegistryKey<?>, MutableJavaRegistry<?>> registries;
    private final Reference2ObjectMap<JavaRegistryKey<?>, List<RegistryEntry>> configurationRegistryData;

    RegistryCache() {
        this.registries = new Reference2ObjectOpenHashMap<>(JavaRegistryReaders.networkRegistries().size());
        this.configurationRegistryData = new Reference2ObjectOpenHashMap<>(JavaRegistryReaders.networkRegistries().size());
        for (JavaRegistryKey<?> registry : JavaRegistryReaders.networkRegistries()) {
            registries.put(registry, new SimpleJavaRegistry<>(registry));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        return (JavaRegistry<T>) registries.get(registryKey);
    }

    /**
     * Clears all held registry data. Should be called at the start of the configuration phase.
     */
    public void clear() {
        registries.values().forEach(MutableJavaRegistry::clear);
    }

    /**
     * Loads a {@link ClientboundRegistryDataPacket}. Adds the entries of this packet to the local cache waiting to be parsed by {@link RegistryCache#parseAndFreeze(JavaRegistryProvider, String)}.
     *
     * @param packet the packet with registry data to load
     */
    public void load(ClientboundRegistryDataPacket packet) {
        JavaRegistryKey<?> registryKey = JavaRegistries.fromKey(packet.getRegistry());
        if (registryKey != null && JavaRegistryReaders.shouldParseRegistry(registryKey)) {
            this.configurationRegistryData.put(registryKey, packet.getEntries());
        } else {
            GeyserImpl.getInstance().getLogger().debug("Ignoring registry of type " + packet.getRegistry());
        }
    }

    /**
     * Parses all loaded registry entries, and then freezes all registries.
     *
     * @param registryProvider the {@link JavaRegistryProvider}
     * @param locale the locale used to translate text components in registry data
     */
    public void parseAndFreeze(JavaRegistryProvider registryProvider, String locale) {
        // First, register all the entries (leaving them unbound) with our mutable registries
        configurationRegistryData.forEach((registryKey, registryEntries) -> {
            MutableJavaRegistry<?> registry = registries.get(registryKey);
            if (registryEntries == null) {
                // Server sent us no packet for this registry, presumably because it is running an older version in which it didn't exist yet.
                // Vanilla (26.3) leaves the registry empty in that case (instead of loading default data), as far as I can tell.
                registry.freeze();
            } else {
                registerEntries(registry, registryEntries);
            }
        });
        // Then, go over all entries again, this time binding them. We do this at this stage to ensure references to other dynamic registries work.
        configurationRegistryData.forEach((registryKey, registryEntries) -> {
            loadRegistry(registryProvider, registryKey, locale, registryEntries);
        });
        // Finally, we can clear the raw registry data
        configurationRegistryData.clear();
    }

    private void registerEntries(MutableJavaRegistry<?> registry, List<RegistryEntry> registryEntries) {
        registryEntries.forEach(entry -> registry.register(entry.getId()));
    }

    public static void bootstrap() {
        // noop to load default network codec
    }

    private <T> void loadRegistry(JavaRegistryProvider registryProvider, JavaRegistryKey<T> registryKey,
                                  String locale, List<RegistryEntry> entries) {
        Optional<KeyDependentJavaRegistryReader<T>> reader = JavaRegistryReaders.getReader(registryKey);
        if (reader.isPresent()) {
            try {
                // Generics ftw
                readRegistry(registryProvider, registryKey, (MutableJavaRegistry<T>) registry(registryKey), reader.get(), locale, entries);
            } catch (Exception exception) {
                GeyserImpl.getInstance().getLogger().error("Failed parsing registry entries for " + registryKey + "!", exception);
            }
        } else {
            throw new IllegalStateException("Expected reader for networked registry " + registryKey);
        }
    }

    private static <T> void readRegistry(JavaRegistryProvider registryProvider, JavaRegistryKey<T> registryKey,
                                         MutableJavaRegistry<T> registry, KeyDependentJavaRegistryReader<T> reader,
                                         String locale, List<RegistryEntry> entries) {

        Map<Key, NbtMap> localRegistryData = DEFAULTS.get(registryKey);

        // Bind the registry, parsing all the registries
        registry.freeze(id -> {
            RegistryEntry entry = entries.get(id);
            // If the data is null, that's the server telling us we need to use our default values.
            if (entry.getData() == null) {
                entry = new RegistryEntry(entry.getId(), localRegistryData.get(entry.getId()));
            }

            return reader.read(new RegistryEntryContext(registryProvider, entry, locale));
        });
    }
}
