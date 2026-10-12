/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.java.reader;

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.entity.type.living.animal.FrogEntity;
import org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal;
import org.geysermc.geyser.entity.type.living.animal.VariantHolder;
import org.geysermc.geyser.entity.type.living.animal.nautilus.ZombieNautilusEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity;
import org.geysermc.geyser.inventory.item.BannerPattern;
import org.geysermc.geyser.inventory.item.GeyserInstrument;
import org.geysermc.geyser.inventory.recipe.TrimRecipes;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.JukeboxSong;
import org.geysermc.geyser.level.PaintingType;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.session.dialog.Dialog;
import org.geysermc.geyser.text.ChatDecoration;
import org.geysermc.geyser.translator.level.BiomeTranslator;
import org.geysermc.geyser.translator.level.block.entity.DecoratedPotBlockEntityTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.JavaDamageEventTranslator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Defines {@link JavaRegistryReader}s for all networked {@link JavaRegistryKey}s. A "networked registry" is a Java registry that is synced with the client over network.
 *
 * <p>The server can add, modify, or remove entries from such a registry during the configuration phase. This will happen especially frequently on proxy servers.</p>
 *
 * <p>For each networked registry defined in {@link JavaRegistries}, a {@link JavaRegistryReader} has to be defined here. This may be a {@link KeyDependentJavaRegistryReader} as well,
 * however prefer to use {@link JavaRegistryReader} when possible (see the notes at {@link KeyDependentJavaRegistryReader} as to why).</p>
 *
 * <p>Register readers in the static initialisation block below, using {@link JavaRegistryReaders#register(JavaRegistryKey, JavaRegistryReader)}
 * or {@link JavaRegistryReaders#registerKeyDependent(JavaRegistryKey, KeyDependentJavaRegistryReader)}. Please maintain the same order as in {@link JavaRegistries}.</p>
 *
 * @see JavaRegistryReader
 * @see KeyDependentJavaRegistryReader
 * @see JavaRegistries
 */
public final class JavaRegistryReaders {
    private static final Map<JavaRegistryKey<?>, KeyDependentJavaRegistryReader<?>> READERS = new HashMap<>();

    static {
        register(JavaRegistries.CHAT_TYPE, ChatDecoration::readChatType);
        registerKeyDependent(JavaRegistries.DIMENSION_TYPE, JavaDimension::read);
        registerKeyDependent(JavaRegistries.BIOME, BiomeTranslator::loadServerBiome);
        registerKeyDependent(JavaRegistries.ENCHANTMENT, Enchantment::read);
        registerKeyDependent(JavaRegistries.BANNER_PATTERN, context -> BannerPattern.getByJavaIdentifier(context.id()));
        registerKeyDependent(JavaRegistries.INSTRUMENT, GeyserInstrument::read);
        register(JavaRegistries.JUKEBOX_SONG, JukeboxSong::read);
        registerKeyDependent(JavaRegistries.PAINTING_VARIANT, context -> PaintingType.getByName(context.id()));
        register(JavaRegistries.TRIM_MATERIAL, TrimRecipes::readTrimMaterial);
        register(JavaRegistries.TRIM_PATTERN, TrimRecipes::readTrimPattern);
        register(JavaRegistries.DAMAGE_TYPE, JavaDamageEventTranslator::readDamageCause);
        register(JavaRegistries.DIALOG, Dialog::readDialog);
        register(JavaRegistries.WORLD_CLOCK, JavaRegistryReader.UNIT);
        register(JavaRegistries.DECORATED_POT_PATTERN, DecoratedPotBlockEntityTranslator::readDecoratedPotPattern);
        register(JavaRegistries.BLOCK_TRANSFORMER, JavaRegistryReader.UNIT);

        registerKeyDependent(JavaRegistries.CAT_VARIANT, VariantHolder.reader(CatEntity.BuiltInVariant.class, CatEntity.BuiltInVariant.BLACK));
        registerKeyDependent(JavaRegistries.FROG_VARIANT, VariantHolder.reader(FrogEntity.BuiltInVariant.class, FrogEntity.BuiltInVariant.TEMPERATE));
        registerKeyDependent(JavaRegistries.WOLF_VARIANT, VariantHolder.reader(WolfEntity.BuiltInVariant.class, WolfEntity.BuiltInVariant.PALE));
        registerKeyDependent(JavaRegistries.PIG_VARIANT, TemperatureVariantAnimal.VARIANT_READER);
        registerKeyDependent(JavaRegistries.COW_VARIANT, TemperatureVariantAnimal.VARIANT_READER);
        registerKeyDependent(JavaRegistries.CHICKEN_VARIANT, TemperatureVariantAnimal.VARIANT_READER);
        registerKeyDependent(JavaRegistries.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusEntity.VARIANT_READER);

        register(JavaRegistries.CAT_SOUND_VARIANT, JavaRegistryReader.UNIT);
        register(JavaRegistries.WOLF_SOUND_VARIANT, JavaRegistryReader.UNIT);
        register(JavaRegistries.PIG_SOUND_VARIANT, JavaRegistryReader.UNIT);
        register(JavaRegistries.COW_SOUND_VARIANT, JavaRegistryReader.UNIT);
        register(JavaRegistries.CHICKEN_SOUND_VARIANT, JavaRegistryReader.UNIT);
    }

    /**
     * @return a collection of {@link JavaRegistryKey}s for all networked registries
     */
    public static Collection<JavaRegistryKey<?>> networkRegistries() {
        return READERS.keySet();
    }

    /**
     * Tries to get the reader for the given {@code key}. This will be an empty optional when the registry is built-in.
     *
     * @param key the {@link JavaRegistryKey} to get the reader for
     * @param <T> the type of the registry
     * @return the reader, or an empty optional when the registry is built-in (no reader was defined)
     */
    public static <T> Optional<KeyDependentJavaRegistryReader<T>> getReader(JavaRegistryKey<T> key) {
        return Optional.ofNullable((KeyDependentJavaRegistryReader<T>) READERS.get(key));
    }

    /**
     * Tries to get the reader for the given {@code key}. This will be an empty optional when the registry is built-in.
     *
     * <p>This method ensures the reader is a {@link JavaRegistryReader}, which supports reading inline entries (without a {@link Key}).</p>
     *
     * @param key the {@link JavaRegistryKey} to get the reader for
     * @param <T> the type of the registry
     * @return the reader, or an empty optional when the registry is built-in (no reader was defined), or the registry reader does not support reading inline entries
     */
    public static <T> Optional<JavaRegistryReader<T>> getInlineSuitableReader(JavaRegistryKey<T> key) {
        return getReader(key)
            .flatMap(reader -> reader instanceof JavaRegistryReader<T> inlineSuitable ? Optional.of(inlineSuitable) : Optional.empty());
    }

    /**
     * Registers a {@link JavaRegistryReader} for the given {@link JavaRegistryKey}.
     *
     * @param registryKey the registry to register a reader for
     * @param reader the {@link JavaRegistryReader}
     * @param <T> the type of the registry
     * @throws IllegalStateException when a reader for the registry was already registered
     */
    private static <T> void register(JavaRegistryKey<T> registryKey, JavaRegistryReader<T> reader) {
        registerKeyDependent(registryKey, reader);
    }

    /**
     * Registers a {@link KeyDependentJavaRegistryReader} for the given {@link JavaRegistryKey}. When possible, prefer {@link JavaRegistryReaders#register(JavaRegistryKey, JavaRegistryReader)}.
     *
     * @param registryKey the registry to register a reader for
     * @param reader the {@link KeyDependentJavaRegistryReader}
     * @param <T> the type of the registry
     * @throws IllegalStateException when a reader for the registry was already registered
     */
    private static <T> void registerKeyDependent(JavaRegistryKey<T> registryKey, KeyDependentJavaRegistryReader<T> reader) {
        if (READERS.containsKey(registryKey)) {
            throw new IllegalStateException("Tried to register registry reader for " + registryKey + " twice!");
        }
        READERS.put(registryKey, reader);
    }

    private JavaRegistryReaders() {}

    public static void bootstrap() {
        // no-op
    }
}
