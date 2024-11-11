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
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.ListRegistry;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.util.MinecraftKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores {@link JavaRegistryKey} for Java registries that are used for loading of data-driven objects, tags, or both. Read {@link JavaRegistryKey} for more information on how to use one.
 */
public class JavaRegistries {
    private static final List<JavaRegistryKey<?>> VALUES = new ArrayList<>();

    public static final JavaRegistryKey<Block> BLOCK = create("block", BlockRegistries.JAVA_BLOCKS, Block::javaId);
    public static final JavaRegistryKey<Item> ITEM = create("item", Registries.JAVA_ITEMS, Item::javaId);
    public static final JavaRegistryKey<Enchantment> ENCHANTMENT = create("enchantment", RegistryCache::enchantments);

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
