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
import org.geysermc.geyser.session.GeyserSession;
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

    RegistryCache() {
        this.registries = new Reference2ObjectOpenHashMap<>(JavaRegistryReaders.networkRegistries().size());
        for (JavaRegistryKey<?> registry : JavaRegistryReaders.networkRegistries()) {
            registries.put(registry, new SimpleJavaRegistry<>(registry));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        return (JavaRegistry<T>) registries.get(registryKey);
    }

    /**
     * Loads a registry in, if we are tracking it.
     */
    // Java generic mess
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void load(JavaRegistryProvider registryProvider, Optional<GeyserSession> session, ClientboundRegistryDataPacket packet) {
        JavaRegistryKey registryKey = JavaRegistries.fromKey(packet.getRegistry());
        if (registryKey != null) {
            loadRegistry(registryKey, registryProvider, session, packet.getEntries());
        } else {
            GeyserImpl.getInstance().getLogger().debug("Ignoring registry of type " + packet.getRegistry());
        }
    }

    private <T> void loadRegistry(JavaRegistryKey<T> registryKey, JavaRegistryProvider registryProvider,
                                  Optional<GeyserSession> session, List<RegistryEntry> entries) {
        Optional<KeyDependentJavaRegistryReader<T>> reader = JavaRegistryReaders.getReader(registryKey);
        if (reader.isPresent()) {
            try {
                readRegistry(registryKey, registryProvider, (MutableJavaRegistry<T>) registries.get(registryKey), reader.get(), session, entries);
            } catch (Exception exception) {
                GeyserImpl.getInstance().getLogger().error("Failed parsing registry entries for " + registryKey + "!", exception);
            }
        } else {
            throw new IllegalStateException("Expected reader for networked registry " + registryKey);
        }
    }

    private static <T> void readRegistry(JavaRegistryKey<T> registryKey, JavaRegistryProvider registryProvider,
                                         MutableJavaRegistry<T> registry, KeyDependentJavaRegistryReader<T> reader,
                                         Optional<GeyserSession> session, List<RegistryEntry> entries) {
        Map<Key, NbtMap> localRegistry = DEFAULTS.get(registryKey);

        // Clear each local cache every time a new registry entry is given to us
        // (e.g. proxy server switches, reconfiguring)
        // TODO technically we need to clear all registries at start of configuration,
        // TODO and only freeze/bind at the end of it
        registry.clear();

        // First, register all entries in the registry
        entries.forEach(entry -> registry.register(entry.getId()));

        // Then, bind the registry, parsing all the registries
        registry.freeze(id -> {
            RegistryEntry entry = entries.get(id);
            // If the data is null, that's the server telling us we need to use our default values.
            if (entry.getData() == null) {
                entry = new RegistryEntry(entry.getId(), localRegistry.get(entry.getId()));
            }

            return reader.read(new RegistryEntryContext(registryProvider, entry, session));
        });
    }
}
