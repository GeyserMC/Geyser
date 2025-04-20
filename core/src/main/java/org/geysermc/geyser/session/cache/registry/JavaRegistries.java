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

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.cloudburstmc.protocol.bedrock.data.TrimPattern;
import org.geysermc.geyser.entity.type.living.animal.FrogEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.TemperatureVariantAnimal;
import org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity;
import org.geysermc.geyser.inventory.item.BannerPattern;
import org.geysermc.geyser.inventory.item.GeyserInstrument;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.JukeboxSong;
import org.geysermc.geyser.level.PaintingType;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.ListRegistry;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores {@link JavaRegistryKey} for Java registries that are used for loading of data-driven objects, tags, or both. Read {@link JavaRegistryKey} for more information on how to use one.
 */
public class JavaRegistries {
    private static final List<JavaRegistryKey<?>> VALUES = new ArrayList<>();

    public static final JavaRegistryKey<Block> BLOCK = createHardcoded("block", BlockRegistries.JAVA_BLOCKS, Block::javaId, Block::javaIdentifier);
    public static final JavaRegistryKey<Item> ITEM = createHardcoded("item", Registries.JAVA_ITEMS, Item::javaId, Item::javaKey);

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

    public static final JavaRegistryKey<CatEntity.BuiltInVariant> CAT_VARIANT = create("cat_variant");
    public static final JavaRegistryKey<FrogEntity.BuiltInVariant> FROG_VARIANT = create("frog_variant");
    public static final JavaRegistryKey<WolfEntity.BuiltInVariant> WOLF_VARIANT = create("wolf_variant");
    public static final JavaRegistryKey<RegistryUnit> WOLF_SOUND_VARIANT = create("wolf_sound_variant");

    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> PIG_VARIANT = create("pig_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> COW_VARIANT = create("cow_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> CHICKEN_VARIANT = create("chicken_variant");

    private static <T> JavaRegistryKey<T> create(String key, JavaRegistryKey.NetworkSerializer<T> networkSerializer, JavaRegistryKey.NetworkDeserializer<T> networkDeserializer,
                                                 JavaRegistryKey.NetworkIdentifier<T> networkIdentifier) {
        JavaRegistryKey<T> registry = new JavaRegistryKey<>(MinecraftKey.key(key), networkSerializer, networkDeserializer, networkIdentifier);
        VALUES.add(registry);
        return registry;
    }

    private static <T> JavaRegistryKey<T> createHardcoded(String key, ListRegistry<T> registry, RegistryNetworkMapper<T> networkSerializer, RegistryIdentifierMapper<T> identifierMapper) {
        return create(key, (session, $, object) -> networkSerializer.get(object),
            (session, $, id) -> registry.get(id),
            (session, $, id) -> identifierMapper.get(registry.get(id)));
    }

    private static <T> JavaRegistryKey<T> create(String key) {
        return create(key, (session, registry, object) -> session.getRegistryCache().registry(registry).byValue(object),
            (session, registry, id) -> session.getRegistryCache().registry(registry).byId(id),
            (session, registry, id) -> session.getRegistryCache().registry(registry).entryById(id).key());
    }

    @Nullable
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
    interface RegistryIdentifierMapper<T> {

        Key get(T object);
    }
}
