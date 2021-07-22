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

package org.geysermc.connector.registry;

import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import com.github.steveice10.mc.protocol.data.game.world.effect.SoundEffect;
import com.github.steveice10.mc.protocol.data.game.world.particle.ParticleType;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.effect.Effect;
import org.geysermc.connector.network.translators.sound.SoundHandler;
import org.geysermc.connector.network.translators.sound.SoundInteractionHandler;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.registry.loader.*;
import org.geysermc.connector.registry.populator.ItemRegistryPopulator;
import org.geysermc.connector.registry.populator.RecipeRegistryPopulator;
import org.geysermc.connector.registry.type.ItemMappings;
import org.geysermc.connector.registry.type.ParticleMapping;
import org.geysermc.connector.registry.type.SoundMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds all the common registries in Geyser.
 */
public class Registries {
    public static final SimpleRegistry<NbtMap> BIOMES = SimpleRegistry.create("bedrock/biome_definitions.dat", RegistryLoaders.NBT);

    public static final SimpleMappedRegistry<String, BlockEntityTranslator> BLOCK_ENTITIES = SimpleMappedRegistry.create("org.geysermc.connector.network.translators.world.block.entity.BlockEntity", BlockEntityRegistryLoader::new);

    public static final SimpleMappedRegistry<Integer, BlockCollision> COLLISIONS = SimpleMappedRegistry.create(Pair.of("org.geysermc.connector.network.translators.collision.translators.Translator", "mappings/collision.json"), CollisionRegistryLoader::new);

    public static final VersionedRegistry<Map<RecipeType, List<CraftingData>>> CRAFTING_DATA = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    public static final SimpleRegistry<NbtMap> ENTITY_IDENTIFIERS = SimpleRegistry.create("bedrock/entity_identifiers.dat", RegistryLoaders.NBT);

    public static final VersionedRegistry<ItemMappings> ITEMS = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    public static final SimpleMappedRegistry<ParticleType, ParticleMapping> PARTICLES = SimpleMappedRegistry.create("mappings/particles.json", ParticleTypesRegistryLoader::new);

    public static final SimpleRegistry<Set<PotionMixData>> POTION_MIXES;

    public static final VersionedRegistry<Int2ObjectMap<Recipe>> RECIPES = VersionedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    public static final SimpleMappedRegistry<Integer, SoundEvent> RECORDS = SimpleMappedRegistry.create(RegistryLoaders.empty(Int2ObjectOpenHashMap::new));

    public static final SimpleMappedRegistry<String, SoundMapping> SOUNDS = SimpleMappedRegistry.create("mappings/sounds.json", SoundRegistryLoader::new);

    public static final SimpleMappedRegistry<SoundEffect, Effect> SOUND_EFFECTS = SimpleMappedRegistry.create("mappings/effects.json", SoundEffectsRegistryLoader::new);

    public static final SimpleMappedRegistry<SoundHandler, SoundInteractionHandler<?>> SOUND_HANDLERS = SimpleMappedRegistry.create("org.geysermc.connector.network.translators.sound.SoundHandler", SoundHandlerRegistryLoader::new);

    public static void init() {
        // no-op
    }

    static {
        ItemRegistryPopulator.populate();
        RecipeRegistryPopulator.populate();

        // Create registries that require other registries to load first
        POTION_MIXES = SimpleRegistry.create(PotionMixRegistryLoader::new);
    }
}