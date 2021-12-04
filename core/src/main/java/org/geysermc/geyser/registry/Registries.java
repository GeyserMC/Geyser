/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry;

import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.mc.protocol.data.game.level.event.SoundEvent;
import com.github.steveice10.mc.protocol.data.game.level.particle.ParticleType;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.registry.populator.PacketRegistryPopulator;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.inventory.item.Enchantment.JavaEnchantment;
import org.geysermc.geyser.translator.sound.SoundTranslator;
import org.geysermc.geyser.translator.sound.SoundInteractionTranslator;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.event.LevelEventTranslator;
import org.geysermc.geyser.registry.loader.*;
import org.geysermc.geyser.registry.populator.ItemRegistryPopulator;
import org.geysermc.geyser.registry.populator.RecipeRegistryPopulator;
import org.geysermc.geyser.registry.type.EnchantmentData;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.registry.type.ParticleMapping;
import org.geysermc.geyser.registry.type.SoundMapping;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds all the common registries in Geyser.
 */
public final class Registries {
    /**
     * A registry holding a CompoundTag of the known entity identifiers.
     */
    public static final SimpleRegistry<NbtMap> BEDROCK_ENTITY_IDENTIFIERS = SimpleRegistry.create("bedrock/entity_identifiers.dat", RegistryLoaders.NBT);

    /**
     * A registry containing all the Bedrock packet translators.
     */
    public static final PacketTranslatorRegistry<BedrockPacket> BEDROCK_PACKET_TRANSLATORS = PacketTranslatorRegistry.create();

    /**
     * A registry holding a CompoundTag of all the known biomes.
     */
    public static final SimpleRegistry<NbtMap> BIOMES_NBT = SimpleRegistry.create("bedrock/biome_definitions.dat", RegistryLoaders.NBT);

    /**
     * A mapped registry which stores Java biome identifiers and their Bedrock biome identifier.
     */
    public static final SimpleRegistry<Object2IntMap<String>> BIOME_IDENTIFIERS = SimpleRegistry.create("mappings/biomes.json", BiomeIdentifierRegistryLoader::new);

    /**
     * A mapped registry which stores a block entity identifier to its {@link BlockEntityTranslator}.
     */
    public static final SimpleMappedRegistry<BlockEntityType, BlockEntityTranslator> BLOCK_ENTITIES = SimpleMappedRegistry.create("org.geysermc.geyser.translator.level.block.entity.BlockEntity", BlockEntityRegistryLoader::new);

    /**
     * A mapped registry containing which holds block IDs to its {@link BlockCollision}.
     */
    public static final SimpleMappedRegistry<Integer, BlockCollision> COLLISIONS = SimpleMappedRegistry.create(Pair.of("org.geysermc.geyser.translator.collision.CollisionRemapper", "mappings/collision.json"), CollisionRegistryLoader::new);

    /**
     * A versioned registry which holds a {@link RecipeType} to a corresponding list of {@link CraftingData}.
     */
    public static final VersionedRegistry<Map<RecipeType, List<CraftingData>>> CRAFTING_DATA = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    /**
     * A registry holding data of all the known enchantments.
     */
    public static final SimpleMappedRegistry<JavaEnchantment, EnchantmentData> ENCHANTMENTS;

    /**
     * A map containing all entity types and their respective Geyser definitions
     */
    public static final SimpleMappedRegistry<EntityType, EntityDefinition<?>> ENTITY_DEFINITIONS = SimpleMappedRegistry.create(RegistryLoaders.empty(() -> new EnumMap<>(EntityType.class)));

    /**
     * A map containing all Java entity identifiers and their respective Geyser definitions
     */
    public static final SimpleMappedRegistry<String, EntityDefinition<?>> JAVA_ENTITY_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    /**
     * A registry containing all the Java packet translators.
     */
    public static final PacketTranslatorRegistry<Packet> JAVA_PACKET_TRANSLATORS = PacketTranslatorRegistry.create();

    /**
     * A versioned registry which holds {@link ItemMappings} for each version. These item mappings contain
     * primarily Bedrock version-specific data.
     */
    public static final VersionedRegistry<ItemMappings> ITEMS = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    /**
     * A mapped registry holding the {@link ParticleType} to a corresponding {@link ParticleMapping}, containing various pieces of
     * data primarily for how Bedrock should handle the particle.
     */
    public static final SimpleMappedRegistry<ParticleType, ParticleMapping> PARTICLES = SimpleMappedRegistry.create("mappings/particles.json", ParticleTypesRegistryLoader::new);

    /**
     * A registry holding all the potion mixes.
     */
    public static final SimpleRegistry<Set<PotionMixData>> POTION_MIXES;

    /**
     * A versioned registry holding all the recipes, with the net ID being the key, and {@link Recipe} as the value.
     */
    public static final VersionedRegistry<Int2ObjectMap<Recipe>> RECIPES = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    /**
     * A mapped registry holding the available records, with the ID of the record being the key, and the {@link com.nukkitx.protocol.bedrock.data.SoundEvent}
     * as the value.
     */
    public static final SimpleMappedRegistry<Integer, com.nukkitx.protocol.bedrock.data.SoundEvent> RECORDS = SimpleMappedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    /**
     * A mapped registry holding sound identifiers to their corresponding {@link SoundMapping}.
     */
    public static final SimpleMappedRegistry<String, SoundMapping> SOUNDS = SimpleMappedRegistry.create("mappings/sounds.json", SoundRegistryLoader::new);

    /**
     * A mapped registry holding {@link SoundEvent}s to their corresponding {@link LevelEventTranslator}.
     */
    public static final SimpleMappedRegistry<SoundEvent, LevelEventTranslator> SOUND_EVENTS = SimpleMappedRegistry.create("mappings/effects.json", SoundEventsRegistryLoader::new);

    /**
     * A mapped registry holding {@link SoundTranslator}s to their corresponding {@link SoundInteractionTranslator}.
     */
    public static final SimpleMappedRegistry<SoundTranslator, SoundInteractionTranslator<?>> SOUND_TRANSLATORS = SimpleMappedRegistry.create("org.geysermc.geyser.translator.sound.SoundTranslator", SoundTranslatorRegistryLoader::new);

    public static void init() {
        // no-op
    }

    static {
        PacketRegistryPopulator.populate();
        ItemRegistryPopulator.populate();
        RecipeRegistryPopulator.populate();

        // Create registries that require other registries to load first
        POTION_MIXES = SimpleRegistry.create(PotionMixRegistryLoader::new);
        ENCHANTMENTS = SimpleMappedRegistry.create("mappings/enchantments.json", EnchantmentRegistryLoader::new);
    }
}