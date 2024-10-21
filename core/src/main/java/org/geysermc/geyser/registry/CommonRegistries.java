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

import it.unimi.dsi.fastutil.objects.Object2IntMap;
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

public interface CommonRegistries {
    /**
     * A registry holding all the providers.
     * This has to be initialized first to allow extensions to access providers during other registry events.
     */
    SimpleMappedRegistry<Class<?>, ProviderSupplier> providers();

    /**
     * A registry holding a CompoundTag of the known entity identifiers.
     */
    SimpleRegistry<NbtMap> bedrockEntityIdentifiers();
    /**
     * A map containing all Java entity identifiers and their respective Geyser definitions
     */
    SimpleMappedRegistry<String, EntityDefinition<?>> javaEntityIdentifiers();

    /**
     * A map containing all entity types and their respective Geyser definitions
     */
    SimpleMappedRegistry<EntityType, EntityDefinition<?>> entityDefinitions();
    /**
     * A registry holding a list of all the known entity properties to be sent to the client after start game.
     */
    SimpleRegistry<Set<NbtMap>> bedrockEntityProperties();

    /**
     * A registry holding a CompoundTag of all the known biomes.
     */
    SimpleRegistry<NbtMap> biomesNbt();
    /**
     * A mapped registry which stores Java biome identifiers and their Bedrock biome identifier.
     */
    SimpleRegistry<Object2IntMap<String>> biomeIdentifiers();

    /**
     * A mapped registry which stores a block entity identifier to its {@link BlockEntityTranslator}.
     */
    SimpleMappedRegistry<BlockEntityType, BlockEntityTranslator> blockEntities();

    /**
     * A registry containing all the Bedrock packet translators.
     */
    PacketTranslatorRegistry<BedrockPacket> bedrockPacketTranslators();
    /**
     * A registry containing all the Java packet translators.
     */
    PacketTranslatorRegistry<Packet> javaPacketTranslators();

    /**
     * A registry containing all Java items ordered by their network ID.
     */
    ListRegistry<Item> javaItems();
    SimpleMappedRegistry<String, Item> javaItemIdentifiers();
    /**
     * A versioned registry which holds {@link ItemMappings} for each version. These item mappings contain
     * primarily Bedrock version-specific data.
     */
    VersionedRegistry<ItemMappings> items();

    /**
     * A mapped registry holding the {@link ParticleType} to a corresponding {@link ParticleMapping}, containing various pieces of
     * data primarily for how Bedrock should handle the particle.
     */
    SimpleMappedRegistry<ParticleType, ParticleMapping> particles();

    /**
     * A registry holding all the potion mixes.
     */
    VersionedRegistry<Set<PotionMixData>> potionMixes();
    /**
     * A versioned registry holding all the recipes, with the net ID being the key, and {@link GeyserRecipe} as the value.
     */
    SimpleMappedRegistry<RecipeType, List<GeyserRecipe>> recipes();

    /**
     * A mapped registry holding {@link ResourcePack}'s with the pack uuid as keys.
     */
    DeferredRegistry<Map<String, ResourcePack>> resourcePacks();

    /**
     * A mapped registry holding sound identifiers to their corresponding {@link SoundMapping}.
     */
    SimpleMappedRegistry<String, SoundMapping> sounds();
    /**
     * A mapped registry holding {@link LevelEvent}s to their corresponding {@link LevelEventTranslator}.
     */
    SimpleMappedRegistry<LevelEvent, LevelEventTranslator> soundLevelEvents();
    /**
     * A mapped registry holding {@link SoundTranslator}s to their corresponding {@link SoundInteractionTranslator}.
     */
    SimpleMappedRegistry<SoundTranslator, SoundInteractionTranslator<?>> soundTranslators();

    /**
     * Called after the instance has been set for {@link Registries#instance()}.
     * This allows registries that depend on other registries to continue working.
     * Done specifically for the {@link org.geysermc.geyser.item.Items} class.
     */
    void postInit();
}
