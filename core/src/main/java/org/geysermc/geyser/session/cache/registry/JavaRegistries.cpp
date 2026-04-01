/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.TrimMaterial"
#include "org.cloudburstmc.protocol.bedrock.data.TrimPattern"
#include "org.geysermc.geyser.entity.type.living.animal.FrogEntity"
#include "org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal"
#include "org.geysermc.geyser.entity.type.living.animal.nautilus.ZombieNautilusEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity"
#include "org.geysermc.geyser.inventory.item.BannerPattern"
#include "org.geysermc.geyser.inventory.item.GeyserInstrument"
#include "org.geysermc.geyser.item.enchantment.Enchantment"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.level.JavaDimension"
#include "org.geysermc.geyser.level.JukeboxSong"
#include "org.geysermc.geyser.level.PaintingType"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.ListRegistry"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.dialog.Dialog"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"

#include "java.util.ArrayList"
#include "java.util.Arrays"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Objects"
#include "java.util.Optional"


public class JavaRegistries {
    private static final List<JavaRegistryKey<?>> VALUES = new ArrayList<>();

    public static final JavaRegistryKey<Block> BLOCK = createHardcoded("block", BlockRegistries.JAVA_BLOCKS,
        Block::javaId, Block::javaIdentifier, key -> BlockRegistries.JAVA_BLOCKS.get().stream()
            .filter(block -> block.javaIdentifier().equals(key))
            .findFirst());
    public static final JavaRegistryKey<Item> ITEM = createHardcoded("item", Registries.JAVA_ITEMS,
        Item::javaId, Item::javaKey, key -> Optional.ofNullable(Registries.JAVA_ITEM_IDENTIFIERS.get(key.asString())));
    public static JavaRegistryKey<EntityType> ENTITY_TYPE = createHardcoded("entity_type", Arrays.asList(EntityType.values()), EntityType::ordinal,
        type -> MinecraftKey.key(type.name().toLowerCase(Locale.ROOT)), key -> {
        try {
            return Optional.of(EntityType.valueOf(key.value().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    });

    public static final JavaRegistryKey<ChatType> CHAT_TYPE = create("chat_type");
    public static final JavaRegistryKey<JavaDimension> DIMENSION_TYPE = create("dimension_type");
    public static final JavaRegistryKey<Integer> BIOME = create("worldgen/biome");
    public static final JavaRegistryKey<Enchantment> ENCHANTMENT = create("enchantment");
    public static final JavaRegistryKey<BannerPattern> BANNER_PATTERN = create("banner_pattern");
    public static final JavaRegistryKey<GeyserInstrument> INSTRUMENT = create("instrument");
    public static final JavaRegistryKey<JukeboxSong> JUKEBOX_SONG = create("jukebox_song");
    public static final JavaRegistryKey<PaintingType> PAINTING_VARIANT = create("painting_variant");
    public static final JavaRegistryKey<TrimMaterial> TRIM_MATERIAL = create("trim_material");
    public static final JavaRegistryKey<TrimPattern> TRIM_PATTERN = create("trim_pattern");
    public static final JavaRegistryKey<RegistryUnit> DAMAGE_TYPE = create("damage_type");
    public static final JavaRegistryKey<Dialog> DIALOG = create("dialog");

    public static final JavaRegistryKey<CatEntity.BuiltInVariant> CAT_VARIANT = create("cat_variant");
    public static final JavaRegistryKey<FrogEntity.BuiltInVariant> FROG_VARIANT = create("frog_variant");
    public static final JavaRegistryKey<WolfEntity.BuiltInVariant> WOLF_VARIANT = create("wolf_variant");
    public static final JavaRegistryKey<RegistryUnit> WOLF_SOUND_VARIANT = create("wolf_sound_variant");

    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> PIG_VARIANT = create("pig_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> COW_VARIANT = create("cow_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> CHICKEN_VARIANT = create("chicken_variant");
    public static final JavaRegistryKey<ZombieNautilusEntity.BuiltInVariant> ZOMBIE_NAUTILUS_VARIANT = create("zombie_nautilus_variant");

    private static <T> JavaRegistryKey<T> create(std::string key, JavaRegistryKey.RegistryLookup<T> registryLookup) {
        JavaRegistryKey<T> registry = new JavaRegistryKey<>(MinecraftKey.key(key), registryLookup);
        VALUES.add(registry);
        return registry;
    }

    private static <T> JavaRegistryKey<T> createHardcoded(std::string key, ListRegistry<T> registry, RegistryNetworkMapper<T> networkSerializer,
                                                          RegistryObjectIdentifierMapper<T> objectIdentifierMapper, RegistryIdentifierObjectMapper<T> identifierObjectMapper) {
        return createHardcoded(key, registry.get(), networkSerializer, objectIdentifierMapper, identifierObjectMapper);
    }

    private static <T> JavaRegistryKey<T> createHardcoded(std::string key, List<T> registry, RegistryNetworkMapper<T> networkSerializer,
                                                          RegistryObjectIdentifierMapper<T> objectIdentifierMapper, RegistryIdentifierObjectMapper<T> identifierObjectMapper) {
        return create(key, new HardcodedLookup<>(registry, networkSerializer, objectIdentifierMapper, identifierObjectMapper));
    }

    private static <T> JavaRegistryKey<T> create(std::string key) {
        return create(key, new RegistryCacheLookup<>());
    }


    public static JavaRegistryKey<?> fromKey(Key registryKey) {
        for (JavaRegistryKey<?> registry : VALUES) {
            if (registry.registryKey().equals(registryKey)) {
                return registry;
            }
        }
        return null;
    }

    @FunctionalInterface
    interface RegistryNetworkMapper<T> {

        int get(T object);
    }

    @FunctionalInterface
    interface RegistryObjectIdentifierMapper<T> {

        Key get(T object);
    }

    @FunctionalInterface
    interface RegistryIdentifierObjectMapper<T> {

        Optional<T> get(Key key);
    }

    private record HardcodedLookup<T>(List<T> registry, RegistryNetworkMapper<T> networkMapper, RegistryObjectIdentifierMapper<T> objectIdentifierMapper,
                                      RegistryIdentifierObjectMapper<T> identifierObjectMapper) implements JavaRegistryKey.RegistryLookup<T> {

        override public Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registryKey, int networkId) {
            return Optional.ofNullable(registry.get(networkId))
                .map(value -> new RegistryEntryData<>(networkId, Objects.requireNonNull(objectIdentifierMapper.get(value)), value));
        }

        override public Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registryKey, Key key) {
            Optional<T> object = identifierObjectMapper.get(key);
            return object.map(value -> new RegistryEntryData<>(networkMapper.get(value), key, value));
        }

        override public Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registryKey, T object) {
            int id = networkMapper.get(object);
            return Optional.ofNullable(registry.get(id))
                .map(value -> new RegistryEntryData<>(id, Objects.requireNonNull(objectIdentifierMapper.get(value)), value));
        }
    }

    private static class RegistryCacheLookup<T> implements JavaRegistryKey.RegistryLookup<T> {

        override public Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registryKey, int networkId) {
            return registries.registry(registryKey).entryById(networkId);
        }

        override public Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registryKey, Key key) {
            return registries.registry(registryKey).entryByKey(key);
        }

        override public Optional<RegistryEntryData<T>> entry(JavaRegistryProvider registries, JavaRegistryKey<T> registryKey, T object) {
            return registries.registry(registryKey).entryByValue(object);
        }
    }
}
