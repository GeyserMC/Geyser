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

package org.geysermc.geyser.registry.loader;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates a collection of {@link PotionMixData} that enables the
 * Bedrock client to place brewing items into the brewing stand.
 * (Does not contain actual potion mixes.)
 * <p>
 * Designed to replicate Java Edition behavior.
 * (Ex: Bedrock cannot normally place glass bottles or fully upgraded
 * potions into the brewing stand, but Java can.)
 */
public class PotionMixRegistryLoader implements RegistryLoader<Object, Int2ObjectMap<Set<PotionMixData>>> {

    @Override
    public Int2ObjectMap<Set<PotionMixData>> load(Object input) {
        var allPotionMixes = new Int2ObjectOpenHashMap<Set<PotionMixData>>(Registries.ITEMS.get().size());
        for (var entry : Registries.ITEMS.get().int2ObjectEntrySet()) {
            ItemMappings mappings = entry.getValue();
            List<ItemMapping> ingredients = new ArrayList<>();
            ingredients.add(getNonNull(mappings, Items.NETHER_WART));
            ingredients.add(getNonNull(mappings, Items.REDSTONE));
            ingredients.add(getNonNull(mappings, Items.GLOWSTONE_DUST));
            ingredients.add(getNonNull(mappings, Items.FERMENTED_SPIDER_EYE));
            ingredients.add(getNonNull(mappings, Items.GUNPOWDER));
            ingredients.add(getNonNull(mappings, Items.DRAGON_BREATH));
            ingredients.add(getNonNull(mappings, Items.SUGAR));
            ingredients.add(getNonNull(mappings, Items.RABBIT_FOOT));
            ingredients.add(getNonNull(mappings, Items.GLISTERING_MELON_SLICE));
            ingredients.add(getNonNull(mappings, Items.SPIDER_EYE));
            ingredients.add(getNonNull(mappings, Items.PUFFERFISH));
            ingredients.add(getNonNull(mappings, Items.MAGMA_CREAM));
            ingredients.add(getNonNull(mappings, Items.GOLDEN_CARROT));
            ingredients.add(getNonNull(mappings, Items.BLAZE_POWDER));
            ingredients.add(getNonNull(mappings, Items.GHAST_TEAR));
            ingredients.add(getNonNull(mappings, Items.TURTLE_HELMET));
            ingredients.add(getNonNull(mappings, Items.PHANTOM_MEMBRANE));

            List<ItemMapping> inputs = List.of(
                    getNonNull(mappings, Items.POTION),
                    getNonNull(mappings, Items.SPLASH_POTION),
                    getNonNull(mappings, Items.LINGERING_POTION)
            );

            ItemMapping glassBottle = getNonNull(mappings, Items.GLASS_BOTTLE);

            Set<PotionMixData> potionMixes = new HashSet<>();

            // Add all types of potions as inputs
            ItemMapping fillerIngredient = ingredients.get(0);
            for (ItemMapping entryInput : inputs) {
                for (Potion potion : Potion.VALUES) {
                    potionMixes.add(new PotionMixData(
                            entryInput.getBedrockDefinition().getRuntimeId(), potion.getBedrockId(),
                            fillerIngredient.getBedrockDefinition().getRuntimeId(), fillerIngredient.getBedrockData(),
                            glassBottle.getBedrockDefinition().getRuntimeId(), glassBottle.getBedrockData())
                    );
                }
            }

            // Add all brewing ingredients
            // Also adds glass bottle as input
            for (ItemMapping ingredient : ingredients) {
                potionMixes.add(new PotionMixData(
                        glassBottle.getBedrockDefinition().getRuntimeId(), glassBottle.getBedrockData(),
                        ingredient.getBedrockDefinition().getRuntimeId(), ingredient.getBedrockData(),
                        glassBottle.getBedrockDefinition().getRuntimeId(), glassBottle.getBedrockData())
                );
            }

            allPotionMixes.put(entry.getIntKey(), potionMixes);
        }
        allPotionMixes.trim();
        return allPotionMixes;
    }

    private static ItemMapping getNonNull(ItemMappings mappings, Item javaItem) {
        ItemMapping itemMapping = mappings.getMapping(javaItem);
        if (itemMapping == null)
            throw new NullPointerException("No item entry exists for java identifier: " + javaItem.javaIdentifier());

        return itemMapping;
    }
}