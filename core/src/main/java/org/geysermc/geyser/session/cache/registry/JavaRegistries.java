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
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.JukeboxSong;
import org.geysermc.geyser.level.PaintingType;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.geyser.session.cache.tags.EnchantmentTag;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;

import java.util.ArrayList;
import java.util.List;

public class JavaRegistries {
    private static final List<JavaRegistryKey<?>> VALUES = new ArrayList<>();

    public static final JavaRegistryKey<Block> BLOCK = create("block");
    public static final JavaRegistryKey<Item> ITEM = create("item");
    public static final JavaRegistryKey<ChatType> CHAT_TYPE = create("chat_type");
    public static final JavaRegistryKey<JavaDimension> DIMENSION_TYPE = create("dimension_type");
    public static final JavaRegistryKey<Enchantment> ENCHANTMENT = create("enchantment");
    public static final JavaRegistryKey<JukeboxSong> JUKEBOX_SONG = create("jukebox_song");
    public static final JavaRegistryKey<PaintingType> PAINTING_VARIANT = create("painting_variant");
    public static final JavaRegistryKey<TrimMaterial> TRIM_MATERIAL = create("trim_material");
    public static final JavaRegistryKey<TrimPattern> TRIM_PATTERN = create("trim_pattern");
    public static final JavaRegistryKey<Integer> BIOME = create("worldgen/biome"); // FIXME
    public static final JavaRegistryKey<BannerPattern> BANNER_PATTERN = create("banner_pattern");
    public static final JavaRegistryKey<WolfEntity.BuiltInWolfVariant> WOLF_VARIANT = create("wolf_variant");

    private static <T> JavaRegistryKey<T> create(String key) {
        JavaRegistryKey<T> registry = JavaRegistryKey.create(key);
        VALUES.add(registry);
        return registry;
    }

    @Nullable
    public static JavaRegistryKey<?> fromKey(Key registryKey) {
        for (JavaRegistryKey<?> registry : VALUES) {
            if (registry.getRegistryKey().equals(registryKey)) {
                return registry;
            }
        }
        return null;
    }

    public static void init() {
        BlockTag.init();
        ItemTag.init();
        EnchantmentTag.init();
    }
}
