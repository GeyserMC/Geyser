/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.living.animal.FrogEntity;
import org.geysermc.geyser.entity.type.living.animal.VariantHolder;
import org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal;
import org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity;
import org.geysermc.geyser.inventory.item.BannerPattern;
import org.geysermc.geyser.inventory.item.GeyserInstrument;
import org.geysermc.geyser.inventory.recipe.TrimRecipe;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.JukeboxSong;
import org.geysermc.geyser.level.PaintingType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.registry.JavaRegistryProvider;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;
import org.geysermc.geyser.session.cache.registry.RegistryUnit;
import org.geysermc.geyser.session.cache.registry.SimpleJavaRegistry;
import org.geysermc.geyser.session.dialog.Dialog;
import org.geysermc.geyser.text.ChatDecoration;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
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
public final class RegistryCache implements JavaRegistryProvider {
    private static final Map<JavaRegistryKey<?>, Map<Key, NbtMap>> DEFAULTS;
    @VisibleForTesting
    public static final Map<JavaRegistryKey<?>, RegistryReader<?>> READERS = new HashMap<>();

    static {
        register(JavaRegistries.CHAT_TYPE, ChatDecoration::readChatType);
        register(JavaRegistries.DIMENSION_TYPE, JavaDimension::read);
        register(JavaRegistries.BIOME, BiomeTranslator::loadServerBiome);
        register(JavaRegistries.ENCHANTMENT, Enchantment::read);
        register(JavaRegistries.BANNER_PATTERN, context -> BannerPattern.getByJavaIdentifier(context.id()));
        register(JavaRegistries.INSTRUMENT, GeyserInstrument::read);
        register(JavaRegistries.JUKEBOX_SONG, JukeboxSong::read);
        register(JavaRegistries.PAINTING_VARIANT, context -> PaintingType.getByName(context.id()));
        register(JavaRegistries.TRIM_MATERIAL, TrimRecipe::readTrimMaterial);
        register(JavaRegistries.TRIM_PATTERN, TrimRecipe::readTrimPattern);
        register(JavaRegistries.DAMAGE_TYPE, RegistryReader.UNIT);
        register(JavaRegistries.DIALOG, Dialog::readDialog);

        register(JavaRegistries.CAT_VARIANT, VariantHolder.reader(CatEntity.BuiltInVariant.class, CatEntity.BuiltInVariant.BLACK));
        register(JavaRegistries.FROG_VARIANT, VariantHolder.reader(FrogEntity.BuiltInVariant.class, FrogEntity.BuiltInVariant.TEMPERATE));
        register(JavaRegistries.WOLF_VARIANT, VariantHolder.reader(WolfEntity.BuiltInVariant.class, WolfEntity.BuiltInVariant.PALE));
        register(JavaRegistries.WOLF_SOUND_VARIANT, RegistryReader.UNIT);

        register(JavaRegistries.PIG_VARIANT, TemperatureVariantAnimal.VARIANT_READER);
        register(JavaRegistries.COW_VARIANT, TemperatureVariantAnimal.VARIANT_READER);
        register(JavaRegistries.CHICKEN_VARIANT, TemperatureVariantAnimal.VARIANT_READER);
        register(JavaRegistries.ZOMBIE_NAUTILUS_VARIANT, TemperatureVariantAnimal.VARIANT_READER);

        // Load from MCProtocolLib's classloader
        NbtMap tag = MinecraftProtocol.loadNetworkCodec();
        Map<JavaRegistryKey<?>, Map<Key, NbtMap>> defaults = new HashMap<>();
        // Don't create a keySet - no need to create the cached object in HashMap if we don't use it again
        READERS.forEach((key, $) -> {
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

    private final GeyserSession session;
    private final Reference2ObjectMap<JavaRegistryKey<?>, SimpleJavaRegistry<?>> registries;

    public RegistryCache(GeyserSession session) {
        this.session = session;
        this.registries = new Reference2ObjectOpenHashMap<>(READERS.size());
        for (JavaRegistryKey<?> registry : READERS.keySet()) {
            registries.put(registry, new SimpleJavaRegistry<>());
        }
    }

    /**
     * Loads a registry in, if we are tracking it.
     */
    public void load(ClientboundRegistryDataPacket packet) {
        // Java generic mess - we're sure we're putting the current readers for the correct registry types in the READERS map, so we use raw objects here to let it compile
        JavaRegistryKey registryKey = JavaRegistries.fromKey(packet.getRegistry());
        if (registryKey != null) {
            RegistryReader reader = READERS.get(registryKey);
            if (reader != null) {
                try {
                    readRegistry(session, registryKey, registries.get(registryKey), reader, packet.getEntries());
                } catch (Exception exception) {
                    GeyserImpl.getInstance().getLogger().error("Failed parsing registry entries for " + registryKey + "!", exception);
                }
            } else {
                throw new IllegalStateException("Expected reader for registry " + registryKey);
            }
        } else {
            GeyserImpl.getInstance().getLogger().debug("Ignoring registry of type " + packet.getRegistry());
        }
    }

    @Override
    public <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        if (!registries.containsKey(registryKey)) {
            throw new IllegalArgumentException("The given registry is not data-driven");
        }
        return (JavaRegistry<T>) registries.get(registryKey);
    }

    private static <T> void readRegistry(GeyserSession session, JavaRegistryKey<T> registryKey, SimpleJavaRegistry<T> registry,
                                         RegistryReader<T> reader, List<RegistryEntry> entries) {
        Map<Key, NbtMap> localRegistry = null;

        // Clear each local cache every time a new registry entry is given to us
        // (e.g. proxy server switches, reconfiguring)

        // Store each of the entries resource location IDs and their respective network ID, used for the key -> ID map in RegistryEntryContext
        Object2IntMap<Key> entryIdMap = new Object2IntOpenHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            entryIdMap.put(entries.get(i).getId(), i);
        }

        List<RegistryEntryData<T>> builder = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            RegistryEntry entry = entries.get(i);
            // If the data is null, that's the server telling us we need to use our default values.
            if (entry.getData() == null) {
                if (localRegistry == null) { // Lazy initialize
                    localRegistry = DEFAULTS.get(registryKey);
                }
                entry = new RegistryEntry(entry.getId(), localRegistry.get(entry.getId()));
            }

            RegistryEntryContext context = new RegistryEntryContext(entry, entryIdMap, Optional.of(session));
            // This is what Geyser wants to keep as a value for this registry.
            T cacheEntry = reader.read(context);
            if (cacheEntry == null) {
                // Registry readers should never return null, rather return a default value
                throw new IllegalStateException("Registry reader returned null for an entry!");
            }
            builder.add(i, new RegistryEntryData<>(i, entry.getId(), cacheEntry));
        }
        registry.reset(builder);
    }

    /**
     * @param registryKey the Java registry key, listed in {@link JavaRegistries}
     * @param reader converts the RegistryEntry NBT into an object. Should never return null, rather return a default value!
     * @param <T> the class that represents these entries.
     */
    private static <T> void register(JavaRegistryKey<T> registryKey, RegistryReader<T> reader) {
        if (READERS.containsKey(registryKey)) {
            throw new IllegalStateException("Tried to register registry reader for " + registryKey + " twice!");
        }
        READERS.put(registryKey, reader);
    }

    public static void init() {
        // no-op
    }

    @FunctionalInterface
    public interface RegistryReader<T> {

        RegistryReader<RegistryUnit> UNIT = context -> RegistryUnit.INSTANCE;

        T read(RegistryEntryContext context);
    }
}
