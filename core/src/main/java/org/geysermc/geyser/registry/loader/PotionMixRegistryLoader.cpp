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

#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData"
#include "org.geysermc.geyser.inventory.item.Potion"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.registry.type.ItemMappings"

#include "java.util.ArrayList"
#include "java.util.HashSet"
#include "java.util.List"
#include "java.util.Set"


public class PotionMixRegistryLoader implements RegistryLoader<Object, Int2ObjectMap<Set<PotionMixData>>> {

    override public Int2ObjectMap<Set<PotionMixData>> load(Object input) {
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

            ingredients.add(getNonNull(mappings, Items.STONE));
            ingredients.add(getNonNull(mappings, Items.SLIME_BLOCK));
            ingredients.add(getNonNull(mappings, Items.COBWEB));
            ingredients.add(getNonNull(mappings, Items.BREEZE_ROD));

            List<ItemMapping> inputs = List.of(
                    getNonNull(mappings, Items.POTION),
                    getNonNull(mappings, Items.SPLASH_POTION),
                    getNonNull(mappings, Items.LINGERING_POTION)
            );

            ItemMapping glassBottle = getNonNull(mappings, Items.GLASS_BOTTLE);

            Set<PotionMixData> potionMixes = new HashSet<>();


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
