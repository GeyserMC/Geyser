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

package org.geysermc.geyser.registry;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.loader.BiomeIdentifierRegistryLoader;
import org.geysermc.geyser.registry.loader.BlockEntityRegistryLoader;
import org.geysermc.geyser.registry.loader.ParticleTypesRegistryLoader;
import org.geysermc.geyser.registry.loader.PotionMixRegistryLoader;
import org.geysermc.geyser.registry.loader.ProviderRegistryLoader;
import org.geysermc.geyser.registry.loader.RecipeRegistryLoader;
import org.geysermc.geyser.registry.loader.RegistryLoaders;
import org.geysermc.geyser.registry.loader.SoundEventsRegistryLoader;
import org.geysermc.geyser.registry.loader.SoundRegistryLoader;
import org.geysermc.geyser.registry.loader.SoundTranslatorRegistryLoader;
import org.geysermc.geyser.registry.populator.ItemRegistryPopulator;
import org.geysermc.geyser.registry.populator.PacketRegistryPopulator;
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

/**
 * Holds all the common registries in Geyser.
 */
public final class CommonRegistriesDefault implements CommonRegistries {
    private static final CommonRegistriesDefault INSTANCE = new CommonRegistriesDefault();

    private final SimpleMappedRegistry<Class<?>, ProviderSupplier> providers = SimpleMappedRegistry.create(new IdentityHashMap<>(), ProviderRegistryLoader::new);

    private final SimpleRegistry<NbtMap> bedrockEntityIdentifiers = SimpleRegistry.create("bedrock/entity_identifiers.dat", RegistryLoaders.NBT);
    private final SimpleMappedRegistry<String, EntityDefinition<?>> javaEntityIdentifiers = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    private final SimpleMappedRegistry<EntityType, EntityDefinition<?>> entityDefinitions = SimpleMappedRegistry.create(RegistryLoaders.empty(() -> new EnumMap<>(EntityType.class)));
    private final SimpleRegistry<Set<NbtMap>> bedrockEntityProperties = SimpleRegistry.create(RegistryLoaders.empty(HashSet::new));

    private final SimpleRegistry<NbtMap> biomesNbt = SimpleRegistry.create("bedrock/biome_definitions.dat", RegistryLoaders.NBT);
    private final SimpleRegistry<Object2IntMap<String>> biomeIdentifiers = SimpleRegistry.create("mappings/biomes.json", BiomeIdentifierRegistryLoader::new);

    private final SimpleMappedRegistry<BlockEntityType, BlockEntityTranslator> blockEntities = SimpleMappedRegistry.create("org.geysermc.geyser.translator.level.block.entity.BlockEntity", BlockEntityRegistryLoader::new);

    private final PacketTranslatorRegistry<BedrockPacket> bedrockPacketTranslators = PacketTranslatorRegistry.create();
    private final PacketTranslatorRegistry<Packet> javaPacketTranslators = PacketTranslatorRegistry.create();

    private final ListRegistry<Item> javaItems = ListRegistry.create(RegistryLoaders.empty(ArrayList::new));
    private final SimpleMappedRegistry<String, Item> javaItemIdentifiers = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));
    private final VersionedRegistry<ItemMappings> items = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    private final SimpleMappedRegistry<ParticleType, ParticleMapping> particles = SimpleMappedRegistry.create("mappings/particles.json", ParticleTypesRegistryLoader::new);

    private VersionedRegistry<Set<PotionMixData>> potionMixes;
    private final SimpleMappedRegistry<RecipeType, List<GeyserRecipe>> recipes = SimpleMappedRegistry.create("mappings/recipes.nbt", RecipeRegistryLoader::new);

    private final DeferredRegistry<Map<String, ResourcePack>> resourcePacks = DeferredRegistry.create(GeyserImpl.getInstance().packDirectory(), SimpleMappedRegistry::create, RegistryLoaders.RESOURCE_PACKS);

    private final SimpleMappedRegistry<String, SoundMapping> sounds = SimpleMappedRegistry.create("mappings/sounds.json", SoundRegistryLoader::new);
    private final SimpleMappedRegistry<LevelEvent, LevelEventTranslator> soundLevelEvents = SimpleMappedRegistry.create("mappings/effects.json", SoundEventsRegistryLoader::new);
    private final SimpleMappedRegistry<SoundTranslator, SoundInteractionTranslator<?>> soundTranslators = SimpleMappedRegistry.create("org.geysermc.geyser.translator.sound.SoundTranslator", SoundTranslatorRegistryLoader::new);

    CommonRegistriesDefault() {}

    public static CommonRegistriesDefault instance() {
        return INSTANCE;
    }

    @Override
    public void postInit() {
        PacketRegistryPopulator.populate(this);
        ItemRegistryPopulator.populate(this);

        // Create registries that require other registries to load first
        potionMixes = VersionedRegistry.create(this, PotionMixRegistryLoader::new);

        // Remove unneeded client generation data from NbtMapBuilder
        NbtMapBuilder biomesNbt = NbtMap.builder();
        for (Map.Entry<String, Object> entry : this.biomesNbt.get().entrySet()) {
            String key = entry.getKey();
            NbtMapBuilder value = ((NbtMap) entry.getValue()).toBuilder();
            value.remove("minecraft:consolidated_features");
            value.remove("minecraft:multinoise_generation_rules");
            value.remove("minecraft:surface_material_adjustments");
            value.remove( "minecraft:surface_parameters");
            biomesNbt.put(key, value.build());
        }
        this.biomesNbt.set(biomesNbt.build());
    }

    @Override
    public SimpleMappedRegistry<Class<?>, ProviderSupplier> providers() {
        return providers;
    }

    @Override
    public SimpleRegistry<NbtMap> bedrockEntityIdentifiers() {
        return bedrockEntityIdentifiers;
    }

    @Override
    public SimpleMappedRegistry<String, EntityDefinition<?>> javaEntityIdentifiers() {
        return javaEntityIdentifiers;
    }

    @Override
    public SimpleMappedRegistry<EntityType, EntityDefinition<?>> entityDefinitions() {
        return entityDefinitions;
    }

    @Override
    public SimpleRegistry<Set<NbtMap>> bedrockEntityProperties() {
        return bedrockEntityProperties;
    }

    @Override
    public SimpleRegistry<NbtMap> biomesNbt() {
        return biomesNbt;
    }

    @Override
    public SimpleRegistry<Object2IntMap<String>> biomeIdentifiers() {
        return biomeIdentifiers;
    }

    @Override
    public SimpleMappedRegistry<BlockEntityType, BlockEntityTranslator> blockEntities() {
        return blockEntities;
    }

    @Override
    public PacketTranslatorRegistry<BedrockPacket> bedrockPacketTranslators() {
        return bedrockPacketTranslators;
    }

    @Override
    public PacketTranslatorRegistry<Packet> javaPacketTranslators() {
        return javaPacketTranslators;
    }

    @Override
    public ListRegistry<Item> javaItems() {
        return javaItems;
    }

    @Override
    public SimpleMappedRegistry<String, Item> javaItemIdentifiers() {
        return javaItemIdentifiers;
    }

    @Override
    public VersionedRegistry<ItemMappings> items() {
        return items;
    }

    @Override
    public SimpleMappedRegistry<ParticleType, ParticleMapping> particles() {
        return particles;
    }

    @Override
    public VersionedRegistry<Set<PotionMixData>> potionMixes() {
        return potionMixes;
    }

    @Override
    public SimpleMappedRegistry<RecipeType, List<GeyserRecipe>> recipes() {
        return recipes;
    }

    @Override
    public DeferredRegistry<Map<String, ResourcePack>> resourcePacks() {
        return resourcePacks;
    }

    @Override
    public SimpleMappedRegistry<String, SoundMapping> sounds() {
        return sounds;
    }

    @Override
    public SimpleMappedRegistry<LevelEvent, LevelEventTranslator> soundLevelEvents() {
        return soundLevelEvents;
    }

    @Override
    public SimpleMappedRegistry<SoundTranslator, SoundInteractionTranslator<?>> soundTranslators() {
        return soundTranslators;
    }
}
