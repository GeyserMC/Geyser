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

#include "it.unimi.dsi.fastutil.objects.Object2IntMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Reference2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap"
#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.type.living.animal.FrogEntity"
#include "org.geysermc.geyser.entity.type.living.animal.VariantHolder"
#include "org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal"
#include "org.geysermc.geyser.entity.type.living.animal.nautilus.ZombieNautilusEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity"
#include "org.geysermc.geyser.inventory.item.BannerPattern"
#include "org.geysermc.geyser.inventory.item.GeyserInstrument"
#include "org.geysermc.geyser.inventory.recipe.TrimRecipe"
#include "org.geysermc.geyser.item.enchantment.Enchantment"
#include "org.geysermc.geyser.level.JavaDimension"
#include "org.geysermc.geyser.level.JukeboxSong"
#include "org.geysermc.geyser.level.PaintingType"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistry"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryKey"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryProvider"
#include "org.geysermc.geyser.session.cache.registry.RegistryEntryContext"
#include "org.geysermc.geyser.session.cache.registry.RegistryEntryData"
#include "org.geysermc.geyser.session.cache.registry.RegistryUnit"
#include "org.geysermc.geyser.session.cache.registry.SimpleJavaRegistry"
#include "org.geysermc.geyser.session.dialog.Dialog"
#include "org.geysermc.geyser.text.ChatDecoration"
#include "org.geysermc.geyser.translator.level.BiomeTranslator"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.MinecraftProtocol"
#include "org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry"
#include "org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket"
#include "org.jetbrains.annotations.VisibleForTesting"

#include "java.util.ArrayList"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Optional"


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
        register(JavaRegistries.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusEntity.VARIANT_READER);


        NbtMap tag = MinecraftProtocol.loadNetworkCodec();
        Map<JavaRegistryKey<?>, Map<Key, NbtMap>> defaults = new HashMap<>();

        READERS.forEach((key, $) -> {
            List<NbtMap> rawValues = tag.getCompound(key.registryKey().asString()).getList("value", NbtType.COMPOUND);
            Map<Key, NbtMap> values = new HashMap<>();
            for (NbtMap value : rawValues) {
                Key name = MinecraftKey.key(value.getString("name"));
                values.put(name, value.getCompound("element"));
            }

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


    public void load(ClientboundRegistryDataPacket packet) {

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

    override public <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        if (!registries.containsKey(registryKey)) {
            throw new IllegalArgumentException("The given registry is not data-driven");
        }
        return (JavaRegistry<T>) registries.get(registryKey);
    }

    private static <T> void readRegistry(GeyserSession session, JavaRegistryKey<T> registryKey, SimpleJavaRegistry<T> registry,
                                         RegistryReader<T> reader, List<RegistryEntry> entries) {
        Map<Key, NbtMap> localRegistry = null;





        Object2IntMap<Key> entryIdMap = new Object2IntOpenHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            entryIdMap.put(entries.get(i).getId(), i);
        }

        List<RegistryEntryData<T>> builder = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            RegistryEntry entry = entries.get(i);

            if (entry.getData() == null) {
                if (localRegistry == null) {
                    localRegistry = DEFAULTS.get(registryKey);
                }
                entry = new RegistryEntry(entry.getId(), localRegistry.get(entry.getId()));
            }

            RegistryEntryContext context = new RegistryEntryContext(entry, entryIdMap, Optional.of(session));

            T cacheEntry = reader.read(context);
            if (cacheEntry == null) {

                throw new IllegalStateException("Registry reader returned null for an entry!");
            }
            builder.add(i, new RegistryEntryData<>(i, entry.getId(), cacheEntry));
        }
        registry.reset(builder);
    }


    private static <T> void register(JavaRegistryKey<T> registryKey, RegistryReader<T> reader) {
        if (READERS.containsKey(registryKey)) {
            throw new IllegalStateException("Tried to register registry reader for " + registryKey + " twice!");
        }
        READERS.put(registryKey, reader);
    }

    public static void init() {

    }

    @FunctionalInterface
    public interface RegistryReader<T> {

        RegistryReader<RegistryUnit> UNIT = context -> RegistryUnit.INSTANCE;

        T read(RegistryEntryContext context);
    }
}
