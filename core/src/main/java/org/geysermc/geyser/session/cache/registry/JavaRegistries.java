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
import org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity;
import org.geysermc.geyser.inventory.item.BannerPattern;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.inventory.item.GeyserInstrument;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.JukeboxSong;
import org.geysermc.geyser.level.PaintingType;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.ListRegistry;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores {@link JavaRegistryKey} for Java registries that are used for loading of data-driven objects, tags, or both. Read {@link JavaRegistryKey} for more information on how to use one.
 */
public class JavaRegistries {
    private static final List<JavaRegistryKey<?>> VALUES = new ArrayList<>();

    public static final JavaRegistryKey<Block> BLOCK = create("block", BlockRegistries.JAVA_BLOCKS, Block::javaId);
    public static final JavaRegistryKey<Item> ITEM = create("item", Registries.JAVA_ITEMS, Item::javaId);
    public static final JavaRegistryKey<ChatType> CHAT_TYPE = create("chat_type", RegistryCache::chatTypes);
    public static final JavaRegistryKey<JavaDimension> DIMENSION_TYPE = create("dimension_type", RegistryCache::dimensions);
    public static final JavaRegistryKey<Enchantment> ENCHANTMENT = create("enchantment", RegistryCache::enchantments);
    public static final JavaRegistryKey<JukeboxSong> JUKEBOX_SONG = create("jukebox_song", RegistryCache::jukeboxSongs);
    public static final JavaRegistryKey<PaintingType> PAINTING_VARIANT = create("painting_variant", RegistryCache::paintings);
    public static final JavaRegistryKey<TrimMaterial> TRIM_MATERIAL = create("trim_material", RegistryCache::trimMaterials);
    public static final JavaRegistryKey<TrimPattern> TRIM_PATTERN = create("trim_pattern", RegistryCache::trimPatterns);
    public static final JavaRegistryKey<GeyserInstrument> INSTRUMENT = create("instrument", RegistryCache::instruments);
    /**
     * This registry should not be used in holder sets, tags, etc. It's simply used as a mapping from Java biomes to Bedrock ones.
     */
    public static final JavaRegistryKey<Integer> BIOME = create("worldgen/biome");
    public static final JavaRegistryKey<BannerPattern> BANNER_PATTERN = create("banner_pattern", RegistryCache::bannerPatterns);
    public static final JavaRegistryKey<WolfEntity.BuiltInWolfVariant> WOLF_VARIANT = create("wolf_variant", RegistryCache::wolfVariants);

    private static <T> JavaRegistryKey<T> create(String key, JavaRegistryKey.NetworkSerializer<T> networkSerializer, JavaRegistryKey.NetworkDeserializer<T> networkDeserializer) {
        JavaRegistryKey<T> registry = new JavaRegistryKey<>(MinecraftKey.key(key), networkSerializer, networkDeserializer);
        VALUES.add(registry);
        return registry;
    }

    private static <T> JavaRegistryKey<T> create(String key, ListRegistry<T> registry, RegistryNetworkMapper<T> networkSerializer) {
        return create(key, (session, object) -> networkSerializer.get(object), (session, id) -> registry.get(id));
    }

    private static <T> JavaRegistryKey<T> create(String key, RegistryGetter<T> getter) {
        return create(key, (session, object) -> getter.get(session.getRegistryCache()).byValue(object), (session, id) -> getter.get(session.getRegistryCache()).byId(id));
    }

    private static <T> JavaRegistryKey<T> create(String key) {
        // Cast for ambiguous call
        return create(key, (JavaRegistryKey.NetworkSerializer<T>) null, null);
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
    interface RegistryGetter<T> {

        JavaRegistry<T> get(RegistryCache cache);
    }

    @FunctionalInterface
    interface RegistryNetworkMapper<T> {

        int get(T object);
    }
}
