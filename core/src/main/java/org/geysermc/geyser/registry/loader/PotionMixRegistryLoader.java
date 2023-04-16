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

import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.inventory.item.Potion;
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
 *
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
            ingredients.add(getNonNull(mappings, "minecraft:nether_wart"));
            ingredients.add(getNonNull(mappings, "minecraft:redstone"));
            ingredients.add(getNonNull(mappings, "minecraft:glowstone_dust"));
            ingredients.add(getNonNull(mappings, "minecraft:fermented_spider_eye"));
            ingredients.add(getNonNull(mappings, "minecraft:gunpowder"));
            ingredients.add(getNonNull(mappings, "minecraft:dragon_breath"));
            ingredients.add(getNonNull(mappings, "minecraft:sugar"));
            ingredients.add(getNonNull(mappings, "minecraft:rabbit_foot"));
            ingredients.add(getNonNull(mappings, "minecraft:glistering_melon_slice"));
            ingredients.add(getNonNull(mappings, "minecraft:spider_eye"));
            ingredients.add(getNonNull(mappings, "minecraft:pufferfish"));
            ingredients.add(getNonNull(mappings, "minecraft:magma_cream"));
            ingredients.add(getNonNull(mappings, "minecraft:golden_carrot"));
            ingredients.add(getNonNull(mappings, "minecraft:blaze_powder"));
            ingredients.add(getNonNull(mappings, "minecraft:ghast_tear"));
            ingredients.add(getNonNull(mappings, "minecraft:turtle_helmet"));
            ingredients.add(getNonNull(mappings, "minecraft:phantom_membrane"));

            List<ItemMapping> inputs = List.of(
                    getNonNull(mappings, "minecraft:potion"),
                    getNonNull(mappings, "minecraft:splash_potion"),
                    getNonNull(mappings, "minecraft:lingering_potion")
            );

            ItemMapping glassBottle = getNonNull(mappings, "minecraft:glass_bottle");

            Set<PotionMixData> potionMixes = new HashSet<>();

            // Add all types of potions as inputs
            ItemMapping fillerIngredient = ingredients.get(0);
            for (ItemMapping entryInput : inputs) {
                for (Potion potion : Potion.VALUES) {
                    potionMixes.add(new PotionMixData(
                            entryInput.getBedrockId(), potion.getBedrockId(),
                            fillerIngredient.getBedrockId(), fillerIngredient.getBedrockData(),
                            glassBottle.getBedrockId(), glassBottle.getBedrockData())
                    );
                }
            }

            // Add all brewing ingredients
            // Also adds glass bottle as input
            for (ItemMapping ingredient : ingredients) {
                potionMixes.add(new PotionMixData(
                        glassBottle.getBedrockId(), glassBottle.getBedrockData(),
                        ingredient.getBedrockId(), ingredient.getBedrockData(),
                        glassBottle.getBedrockId(), glassBottle.getBedrockData())
                );
            }

            allPotionMixes.put(entry.getIntKey(), potionMixes);
        }
        allPotionMixes.trim();
        return allPotionMixes;
    }

    private static ItemMapping getNonNull(ItemMappings mappings, String javaIdentifier) {
        ItemMapping itemMapping = mappings.getMapping(javaIdentifier);
        if (itemMapping == null)
            throw new NullPointerException("No item entry exists for java identifier: " + javaIdentifier);

        return itemMapping;
    }
}