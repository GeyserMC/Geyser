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

import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO this needs to be versioned, but the runtime item states between 1.17 and 1.17.10 are identical except for new blocks so this works for both
/**
 * Generates a collection of {@link PotionMixData} that enables the
 * Bedrock client to place brewing items into the brewing stand.
 * (Does not contain actual potion mixes.)
 *
 * Designed to replicate Java Edition behavior.
 * (Ex: Bedrock cannot normally place glass bottles or fully upgraded
 * potions into the brewing stand, but Java can.)
 */
public class PotionMixRegistryLoader implements RegistryLoader<Object, Set<PotionMixData>> {

    @Override
    public Set<PotionMixData> load(Object input) {
        List<ItemMapping> ingredients = new ArrayList<>();
        ingredients.add(getNonNull(Items.NETHER_WART));
        ingredients.add(getNonNull(Items.REDSTONE));
        ingredients.add(getNonNull(Items.GLOWSTONE_DUST));
        ingredients.add(getNonNull(Items.FERMENTED_SPIDER_EYE));
        ingredients.add(getNonNull(Items.GUNPOWDER));
        ingredients.add(getNonNull(Items.DRAGON_BREATH));
        ingredients.add(getNonNull(Items.SUGAR));
        ingredients.add(getNonNull(Items.RABBIT_FOOT));
        ingredients.add(getNonNull(Items.GLISTERING_MELON_SLICE));
        ingredients.add(getNonNull(Items.SPIDER_EYE));
        ingredients.add(getNonNull(Items.PUFFERFISH));
        ingredients.add(getNonNull(Items.MAGMA_CREAM));
        ingredients.add(getNonNull(Items.GOLDEN_CARROT));
        ingredients.add(getNonNull(Items.BLAZE_POWDER));
        ingredients.add(getNonNull(Items.GHAST_TEAR));
        ingredients.add(getNonNull(Items.TURTLE_HELMET));
        ingredients.add(getNonNull(Items.PHANTOM_MEMBRANE));

        List<ItemMapping> inputs = new ArrayList<>();
        inputs.add(getNonNull(Items.POTION));
        inputs.add(getNonNull(Items.SPLASH_POTION));
        inputs.add(getNonNull(Items.LINGERING_POTION));

        ItemMapping glassBottle = getNonNull(Items.GLASS_BOTTLE);

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
        return potionMixes;
    }

    private static ItemMapping getNonNull(Item javaItem) {
        ItemMapping itemMapping = Registries.ITEMS.forVersion(GameProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion()).getMapping(javaItem);
        if (itemMapping == null)
            throw new NullPointerException("No item entry exists for java identifier: " + javaItem.javaIdentifier());

        return itemMapping;
    }
}