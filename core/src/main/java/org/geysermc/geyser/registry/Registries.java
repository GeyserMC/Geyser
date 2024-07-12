/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.loader.*;
import org.geysermc.geyser.registry.populator.ItemRegistryPopulator;
import org.geysermc.geyser.registry.populator.PacketRegistryPopulator;
import org.geysermc.geyser.registry.loader.RecipeRegistryLoader;
import org.geysermc.geyser.registry.provider.ProviderSupplier;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.registry.type.ParticleMapping;
import org.geysermc.geyser.registry.type.SoundMapping;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.event.LevelEventTranslator;
import org.geysermc.geyser.translator.sound.SoundInteractionTranslator;
import org.geysermc.geyser.translator.sound.SoundTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.RecipeType;

import java.util.*;

/**
 * Holds all the common registries in Geyser.
 */
public final class Registries {
    /**
     * A registry holding all the providers.
     * This has to be initialized first to allow extensions to access providers during other registry events.
     */
    public static final SimpleMappedRegistry<Class<?>, ProviderSupplier> PROVIDERS = SimpleMappedRegistry.create(new IdentityHashMap<>(), ProviderRegistryLoader::new);

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
     * A map containing all entity types and their respective Geyser definitions
     */
    public static final SimpleMappedRegistry<EntityType, EntityDefinition<?>> ENTITY_DEFINITIONS = SimpleMappedRegistry.create(RegistryLoaders.empty(() -> new EnumMap<>(EntityType.class)));

    /**
     * A registry holding a list of all the known entity properties to be sent to the client after start game.
     */
    public static final SimpleRegistry<Set<NbtMap>> BEDROCK_ENTITY_PROPERTIES = SimpleRegistry.create(RegistryLoaders.empty(HashSet::new));

    /**
     * A map containing all Java entity identifiers and their respective Geyser definitions
     */
    public static final SimpleMappedRegistry<String, EntityDefinition<?>> JAVA_ENTITY_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    /**
     * A registry containing all the Java packet translators.
     */
    public static final PacketTranslatorRegistry<Packet> JAVA_PACKET_TRANSLATORS = PacketTranslatorRegistry.create();

    /**
     * A registry containing all Java items ordered by their network ID.
     */
    public static final ListRegistry<Item> JAVA_ITEMS = ListRegistry.create(RegistryLoaders.empty(ArrayList::new));

    public static final SimpleMappedRegistry<String, Item> JAVA_ITEM_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

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
    public static final VersionedRegistry<Set<PotionMixData>> POTION_MIXES;

    /**
     * A versioned registry holding all the recipes, with the net ID being the key, and {@link GeyserRecipe} as the value.
     */
    public static final SimpleMappedRegistry<RecipeType, List<GeyserRecipe>> RECIPES = SimpleMappedRegistry.create("mappings/recipes.nbt", RecipeRegistryLoader::new);

    /**
     * A mapped registry holding {@link ResourcePack}'s with the pack uuid as keys.
     */
    public static final DeferredRegistry<Map<String, ResourcePack>> RESOURCE_PACKS = DeferredRegistry.create(GeyserImpl.getInstance().packDirectory(), SimpleMappedRegistry::create, RegistryLoaders.RESOURCE_PACKS);

    /**
     * A mapped registry holding sound identifiers to their corresponding {@link SoundMapping}.
     */
    public static final SimpleMappedRegistry<String, SoundMapping> SOUNDS = SimpleMappedRegistry.create("mappings/sounds.json", SoundRegistryLoader::new);

    /**
     * A mapped registry holding {@link LevelEvent}s to their corresponding {@link LevelEventTranslator}.
     */
    public static final SimpleMappedRegistry<LevelEvent, LevelEventTranslator> SOUND_LEVEL_EVENTS = SimpleMappedRegistry.create("mappings/effects.json", SoundEventsRegistryLoader::new);

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

        // Create registries that require other registries to load first
        POTION_MIXES = VersionedRegistry.create(PotionMixRegistryLoader::new);

        // Remove unneeded client generation data from NbtMapBuilder
        NbtMapBuilder biomesNbt = NbtMap.builder();
        for (Map.Entry<String, Object> entry : BIOMES_NBT.get().entrySet()) {
            String key = entry.getKey();
            NbtMapBuilder value = ((NbtMap) entry.getValue()).toBuilder();
            value.remove("minecraft:consolidated_features");
            value.remove("minecraft:multinoise_generation_rules");
            value.remove("minecraft:surface_material_adjustments");
            value.remove( "minecraft:surface_parameters");
            biomesNbt.put(key, value.build());
        }
        BIOMES_NBT.set(biomesNbt.build());
    }
}
