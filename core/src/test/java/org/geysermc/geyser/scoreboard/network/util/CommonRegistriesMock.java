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

package org.geysermc.geyser.scoreboard.network.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.CommonRegistries;
import org.geysermc.geyser.registry.DeferredRegistry;
import org.geysermc.geyser.registry.ListRegistry;
import org.geysermc.geyser.registry.PacketTranslatorRegistry;
import org.geysermc.geyser.registry.SimpleMappedRegistry;
import org.geysermc.geyser.registry.SimpleRegistry;
import org.geysermc.geyser.registry.VersionedRegistry;
import org.geysermc.geyser.registry.loader.RegistryLoaders;
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

public class CommonRegistriesMock implements CommonRegistries {
    private final SimpleMappedRegistry<EntityType, EntityDefinition<?>> entityDefinitions = SimpleMappedRegistry.create(RegistryLoaders.empty(() -> new EnumMap<>(EntityType.class)));
    private final SimpleRegistry<Set<NbtMap>> bedrockEntityProperties = SimpleRegistry.create(RegistryLoaders.empty(HashSet::new));

    @Override
    public SimpleMappedRegistry<Class<?>, ProviderSupplier> providers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleRegistry<NbtMap> bedrockEntityIdentifiers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<String, EntityDefinition<?>> javaEntityIdentifiers() {
        // not used in current unit tests, can be safely ignored
        return SimpleMappedRegistry.create(RegistryLoaders.empty(HashMap::new));
    }

    @Override
    public SimpleMappedRegistry<EntityType, EntityDefinition<?>> entityDefinitions() {
        // it is safely initialized by EntityDefinitions
        return entityDefinitions;
    }

    @Override
    public SimpleRegistry<Set<NbtMap>> bedrockEntityProperties() {
        // it is safely initialized by EntityDefinitions
        return bedrockEntityProperties;
    }

    @Override
    public SimpleRegistry<NbtMap> biomesNbt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleRegistry<Object2IntMap<String>> biomeIdentifiers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<BlockEntityType, BlockEntityTranslator> blockEntities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PacketTranslatorRegistry<BedrockPacket> bedrockPacketTranslators() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PacketTranslatorRegistry<Packet> javaPacketTranslators() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListRegistry<Item> javaItems() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<String, Item> javaItemIdentifiers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionedRegistry<ItemMappings> items() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<ParticleType, ParticleMapping> particles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionedRegistry<Set<PotionMixData>> potionMixes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<RecipeType, List<GeyserRecipe>> recipes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeferredRegistry<Map<String, ResourcePack>> resourcePacks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<String, SoundMapping> sounds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<LevelEvent, LevelEventTranslator> soundLevelEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleMappedRegistry<SoundTranslator, SoundInteractionTranslator<?>> soundTranslators() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void postInit() {}
}
