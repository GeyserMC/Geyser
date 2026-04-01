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

#include "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.Object2IntMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData"
#include "org.cloudburstmc.protocol.bedrock.packet.BedrockPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.EntityDefinition"
#include "org.geysermc.geyser.inventory.recipe.GeyserRecipe"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.pack.ResourcePackHolder"
#include "org.geysermc.geyser.registry.loader.BiomeIdentifierRegistryLoader"
#include "org.geysermc.geyser.registry.loader.BlockEntityRegistryLoader"
#include "org.geysermc.geyser.registry.loader.ParticleTypesRegistryLoader"
#include "org.geysermc.geyser.registry.loader.PotionMixRegistryLoader"
#include "org.geysermc.geyser.registry.loader.ProviderRegistryLoader"
#include "org.geysermc.geyser.registry.loader.RegistryLoaders"
#include "org.geysermc.geyser.registry.loader.SoundEventsRegistryLoader"
#include "org.geysermc.geyser.registry.loader.SoundRegistryLoader"
#include "org.geysermc.geyser.registry.loader.SoundTranslatorRegistryLoader"
#include "org.geysermc.geyser.registry.populator.DataComponentRegistryPopulator"
#include "org.geysermc.geyser.registry.populator.ItemRegistryPopulator"
#include "org.geysermc.geyser.registry.populator.PacketRegistryPopulator"
#include "org.geysermc.geyser.registry.populator.TagRegistryPopulator"
#include "org.geysermc.geyser.registry.provider.ProviderSupplier"
#include "org.geysermc.geyser.registry.type.ItemMappings"
#include "org.geysermc.geyser.registry.type.ParticleMapping"
#include "org.geysermc.geyser.registry.type.SoundMapping"
#include "org.geysermc.geyser.registry.type.UtilMappings"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.geyser.translator.level.event.LevelEventTranslator"
#include "org.geysermc.geyser.translator.sound.SoundInteractionTranslator"
#include "org.geysermc.geyser.translator.sound.SoundTranslator"
#include "org.geysermc.mcprotocollib.network.packet.Packet"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEvent"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType"

#include "java.util.ArrayList"
#include "java.util.EnumMap"
#include "java.util.HashSet"
#include "java.util.IdentityHashMap"
#include "java.util.Map"
#include "java.util.Set"
#include "java.util.UUID"


public final class Registries {
    private static bool loaded = false;


    public static final SimpleMappedRegistry<Class<?>, ProviderSupplier> PROVIDERS = SimpleMappedRegistry.create(new IdentityHashMap<>(), ProviderRegistryLoader::new);


    public static final SimpleDeferredRegistry<NbtMap> BEDROCK_ENTITY_IDENTIFIERS = SimpleDeferredRegistry.create("bedrock/entity_identifiers.dat", RegistryLoaders.NBT);


    public static final PacketTranslatorRegistry<BedrockPacket> BEDROCK_PACKET_TRANSLATORS = PacketTranslatorRegistry.create();


    public static final SimpleDeferredRegistry<NbtMap> BIOMES_NBT = SimpleDeferredRegistry.create("bedrock/biome_definitions.dat", RegistryLoaders.NBT);


    public static final SimpleDeferredRegistry<BiomeDefinitions> BIOMES = SimpleDeferredRegistry.create("bedrock/stripped_biome_definitions.json", RegistryLoaders.BIOME_LOADER);


    public static final SimpleDeferredRegistry<Object2IntMap<std::string>> BIOME_IDENTIFIERS = SimpleDeferredRegistry.create("mappings/biomes.json", BiomeIdentifierRegistryLoader::new);


    public static final SimpleMappedDeferredRegistry<BlockEntityType, BlockEntityTranslator> BLOCK_ENTITIES = SimpleMappedDeferredRegistry.create("org.geysermc.geyser.translator.level.block.entity.BlockEntity", BlockEntityRegistryLoader::new);


    public static final SimpleMappedRegistry<EntityType, EntityDefinition<?>> ENTITY_DEFINITIONS = SimpleMappedRegistry.create(RegistryLoaders.empty(() -> new EnumMap<>(EntityType.class)));


    public static final SimpleRegistry<Set<NbtMap>> BEDROCK_ENTITY_PROPERTIES = SimpleRegistry.create(RegistryLoaders.empty(HashSet::new));


    public static final SimpleMappedRegistry<std::string, EntityDefinition<?>> JAVA_ENTITY_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));


    public static final PacketTranslatorRegistry<Packet> JAVA_PACKET_TRANSLATORS = PacketTranslatorRegistry.create();


    public static final ListRegistry<Item> JAVA_ITEMS = ListRegistry.create(RegistryLoaders.empty(ArrayList::new));


    public static final SimpleMappedRegistry<std::string, Item> JAVA_ITEM_IDENTIFIERS = SimpleMappedRegistry.create(RegistryLoaders.empty(Object2ObjectOpenHashMap::new));

    public static final ListRegistry<DataComponents> DEFAULT_DATA_COMPONENTS = ListRegistry.create(RegistryLoaders.empty(ArrayList::new));


    public static final VersionedRegistry<ItemMappings> ITEMS = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));


    public static final SimpleMappedDeferredRegistry<ParticleType, ParticleMapping> PARTICLES = SimpleMappedDeferredRegistry.create("mappings/particles.json", ParticleTypesRegistryLoader::new);


    public static final VersionedDeferredRegistry<Set<PotionMixData>> POTION_MIXES = VersionedDeferredRegistry.create(VersionedRegistry::create, PotionMixRegistryLoader::new);





    public static final SimpleMappedDeferredRegistry<UUID, ResourcePackHolder> RESOURCE_PACKS = SimpleMappedDeferredRegistry.create(GeyserImpl.getInstance().packDirectory(), RegistryLoaders.RESOURCE_PACKS);


    public static final VersionedRegistry<Object2ObjectMap<int[], std::string>> TAGS = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));


    public static final SimpleMappedDeferredRegistry<std::string, SoundMapping> SOUNDS = SimpleMappedDeferredRegistry.create("mappings/sounds.json", SoundRegistryLoader::new);


    public static final SimpleMappedDeferredRegistry<LevelEvent, LevelEventTranslator> SOUND_LEVEL_EVENTS = SimpleMappedDeferredRegistry.create("mappings/effects.json", SoundEventsRegistryLoader::new);


    public static final SimpleMappedDeferredRegistry<SoundTranslator, SoundInteractionTranslator<?>> SOUND_TRANSLATORS = SimpleMappedDeferredRegistry.create("org.geysermc.geyser.translator.sound.SoundTranslator", SoundTranslatorRegistryLoader::new);


    public static final ListDeferredRegistry<Key> GAME_MASTER_BLOCKS = ListDeferredRegistry.create(UtilMappings::gameMasterBlocks, RegistryLoaders.UTIL_MAPPINGS_KEYS);


    public static final ListDeferredRegistry<Key> DANGEROUS_BLOCK_ENTITIES = ListDeferredRegistry.create(UtilMappings::dangerousBlockEntities, RegistryLoaders.UTIL_MAPPINGS_KEYS);


    public static final ListDeferredRegistry<Key> DANGEROUS_ENTITIES = ListDeferredRegistry.create(UtilMappings::dangerousEntities, RegistryLoaders.UTIL_MAPPINGS_KEYS);

    public static void load() {
        if (loaded) return;
        loaded = true;





        BEDROCK_ENTITY_IDENTIFIERS.load();
        BIOMES_NBT.load();
        BIOMES.load();
        BIOME_IDENTIFIERS.load();
        BLOCK_ENTITIES.load();
        PARTICLES.load();


        SOUNDS.load();
        SOUND_LEVEL_EVENTS.load();
        SOUND_TRANSLATORS.load();

        GAME_MASTER_BLOCKS.load();
        DANGEROUS_BLOCK_ENTITIES.load();
        DANGEROUS_ENTITIES.load();
    }

    public static void populate() {
        PacketRegistryPopulator.populate();
        DataComponentRegistryPopulator.populate();
        ItemRegistryPopulator.populate();
        TagRegistryPopulator.populate();


        POTION_MIXES.load();


        NbtMapBuilder biomesNbt = NbtMap.builder();
        for (Map.Entry<std::string, Object> entry : BIOMES_NBT.get().entrySet()) {
            std::string key = entry.getKey();
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
