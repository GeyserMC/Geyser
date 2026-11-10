/*
 * Copyright (c) 2024-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.java;

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
import org.geysermc.geyser.registry.java.reader.JavaRegistryReader;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.session.dialog.Dialog;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

import java.util.List;

/**
 * Defines {@link JavaRegistryKey}s for all registries holding Java content, used across Geyser. These registries may be built-in, or networked registries.
 * Geyser will load {@link Tag}s for all registries defined here.
 *
 * <p>If you need to do any of the following:</p>
 *
 * <ul>
 *     <li>Access the contents of a Java registry, which is networked or in some cases built-in,</li>
 *     <li>Access {@link HolderSet}s for a Java registry, be it built-in or networked, or,</li>
 *     <li>Access tags for a Java registry, be it built-in or networked.</li>
 * </ul>
 *
 * <p>You need to define that registry here. To define a new built-in registry, create a {@link JavaRegistryKey} here and add a new registry to {@link BuiltInJavaRegistries}.
 * To define a new networked registry, create a {@link JavaRegistryKey} here and add a new {@link JavaRegistryReader} to {@link JavaRegistryReaders}.</p>
 *
 * <p>Please try to maintain a sensible order of the registries defined here (see the comments), and mirror that order over at {@link BuiltInJavaRegistries} and
 * {@link JavaRegistryReaders}.</p>
 *
 * @see BuiltInJavaRegistries
 * @see JavaRegistryReaders
 * @see JavaRegistryKey
 * @see JavaRegistry
 */
public final class JavaRegistries {
    private static final List<JavaRegistryKey<?>> VALUES = new ObjectArrayList<>();

    static final JavaRegistryKey<MutableJavaRegistry<?>> BUILT_IN_ROOT = create("root");

    // Built-in registries
    public static final JavaRegistryKey<Block> BLOCK = create("block");
    public static final JavaRegistryKey<Item> ITEM = create("item");
    public static final JavaRegistryKey<EntityTypeDefinition<?>> ENTITY_TYPE = create("entity_type");

    // Most networked registries
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

    // Mob variants
    public static final JavaRegistryKey<CatEntity.BuiltInVariant> CAT_VARIANT = create("cat_variant");
    public static final JavaRegistryKey<FrogEntity.BuiltInVariant> FROG_VARIANT = create("frog_variant");
    public static final JavaRegistryKey<WolfEntity.BuiltInVariant> WOLF_VARIANT = create("wolf_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> PIG_VARIANT = create("pig_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> COW_VARIANT = create("cow_variant");
    public static final JavaRegistryKey<TemperatureVariantAnimal.BuiltInVariant> CHICKEN_VARIANT = create("chicken_variant");
    public static final JavaRegistryKey<ZombieNautilusEntity.BuiltInVariant> ZOMBIE_NAUTILUS_VARIANT = create("zombie_nautilus_variant");

    // Mob sound variants
    public static final JavaRegistryKey<RegistryUnit> CAT_SOUND_VARIANT = create("cat_sound_variant");
    public static final JavaRegistryKey<RegistryUnit> WOLF_SOUND_VARIANT = create("wolf_sound_variant");
    public static final JavaRegistryKey<RegistryUnit> PIG_SOUND_VARIANT = create("pig_sound_variant");
    public static final JavaRegistryKey<RegistryUnit> COW_SOUND_VARIANT = create("cow_sound_variant");
    public static final JavaRegistryKey<RegistryUnit> CHICKEN_SOUND_VARIANT = create("chicken_sound_variant");

    private JavaRegistries() {}

    private static <T> JavaRegistryKey<T> create(String key) {
        JavaRegistryKey<T> registry = new JavaRegistryKey<>(MinecraftKey.key(key));
        if (VALUES.contains(registry)) {
            throw new IllegalStateException("Tried to create a key for " + key + " twice!");
        }
        VALUES.add(registry);
        return registry;
    }

    /**
     * Returns the {@link JavaRegistryKey} correlating to the given {@link Key}, if it exists.
     *
     * <p>When this method returns {@code null} for a registry, Geyser should not handle data from that Java registry.</p>
     *
     * @param registryKey the {@link Key} of a Java registry
     * @return the {@link JavaRegistryKey} correlating to the given key, or null if it doesn't exist
     */
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
