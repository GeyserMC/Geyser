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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDamageCause;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.entity.type.living.animal.FrogEntity;
import org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal;
import org.geysermc.geyser.entity.type.living.animal.nautilus.ZombieNautilusEntity;
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
import org.geysermc.geyser.session.dialog.Dialog;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;

import java.util.List;

/**
 * Stores {@link JavaRegistryKey} for Java registries that are used for loading of data-driven objects, tags, or both. Read {@link JavaRegistryKey} for more information on how to use one.
 */
public class JavaRegistries {
    private static final List<JavaRegistryKey<?>> VALUES = new ObjectArrayList<>();

    public static final JavaRegistryKey<Block> BLOCK = create("block");
    public static final JavaRegistryKey<Item> ITEM = create("item");
    public static final JavaRegistryKey<EntityTypeDefinition<?>> ENTITY_TYPE = create("entity_type");

    public static final JavaRegistryKey<ChatType> CHAT_TYPE = create("chat_type");
    public static final JavaRegistryKey<JavaDimension> DIMENSION_TYPE = create("dimension_type");
    public static final JavaRegistryKey<Integer> BIOME = create("worldgen/biome");
    public static final JavaRegistryKey<Enchantment> ENCHANTMENT = create("enchantment");
    public static final JavaRegistryKey<BannerPattern> BANNER_PATTERN = create("banner_pattern");
    public static final JavaRegistryKey<GeyserInstrument> INSTRUMENT = create("instrument");
    public static final JavaRegistryKey<JukeboxSong> JUKEBOX_SONG = create("jukebox_song");
    public static final JavaRegistryKey<PaintingType> PAINTING_VARIANT = create("painting_variant");
    public static final JavaRegistryKey<ArmorTrim.TrimMaterial> TRIM_MATERIAL = create("trim_material");
    public static final JavaRegistryKey<ArmorTrim.TrimPattern> TRIM_PATTERN = create("trim_pattern");
    public static final JavaRegistryKey<EntityDamageCause> DAMAGE_TYPE = create("damage_type");
    public static final JavaRegistryKey<Dialog> DIALOG = create("dialog");
    public static final JavaRegistryKey<RegistryUnit> WORLD_CLOCK = create("world_clock");
    public static final JavaRegistryKey<Key> DECORATED_POT_PATTERN = create("decorated_pot_pattern");
    public static final JavaRegistryKey<RegistryUnit> BLOCK_TRANSFORMER = create("block_transformer");

    public static final JavaRegistryKey<CatEntity.BuiltInVariant> CAT_VARIANT = create("cat_variant");
    public static final JavaRegistryKey<RegistryUnit> CAT_SOUND_VARIANT = create("cat_sound_variant");
    public static final JavaRegistryKey<FrogEntity.BuiltInVariant> FROG_VARIANT = create("frog_variant");
    public static final JavaRegistryKey<WolfEntity.BuiltInVariant> WOLF_VARIANT = create("wolf_variant");
    public static final JavaRegistryKey<RegistryUnit> WOLF_SOUND_VARIANT = create("wolf_sound_variant");

    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> PIG_VARIANT = create("pig_variant");
    public static final JavaRegistryKey<RegistryUnit> PIG_SOUND_VARIANT = create("pig_sound_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> COW_VARIANT = create("cow_variant");
    public static final JavaRegistryKey<RegistryUnit> COW_SOUND_VARIANT = create("cow_sound_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> CHICKEN_VARIANT = create("chicken_variant");
    public static final JavaRegistryKey<RegistryUnit> CHICKEN_SOUND_VARIANT = create("chicken_sound_variant");
    public static final JavaRegistryKey<ZombieNautilusEntity.BuiltInVariant> ZOMBIE_NAUTILUS_VARIANT = create("zombie_nautilus_variant");

    private static <T> JavaRegistryKey<T> create(String key) {
        JavaRegistryKey<T> registry = new JavaRegistryKey<>(MinecraftKey.key(key));
        VALUES.add(registry);
        return registry;
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
}
