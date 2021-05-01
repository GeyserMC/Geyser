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

package org.geysermc.connector.network.translators.item;

import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;

import java.util.*;

/**
 * Generates a {@link Collection} of {@link PotionMixData} that enables the
 * Bedrock client to place brewing items into the brewing stand.
 * (Does not contain actual potion mixes.)
 *
 * Designed to replicate Java Edition behavior.
 * (Ex: Bedrock cannot normally place glass bottles or fully upgraded
 * potions into the brewing stand, but Java can.)
 */
public class PotionMixRegistry {
    public static final Collection<PotionMixData> POTION_MIXES;

    private PotionMixRegistry() {
    }

    public static void init() {
        // no-op
    }

    static {
        List<ItemEntry> ingredients = new ArrayList<>();
        ingredients.add(getNonNull("minecraft:nether_wart"));
        ingredients.add(getNonNull("minecraft:redstone"));
        ingredients.add(getNonNull("minecraft:glowstone_dust"));
        ingredients.add(getNonNull("minecraft:fermented_spider_eye"));
        ingredients.add(getNonNull("minecraft:gunpowder"));
        ingredients.add(getNonNull("minecraft:dragon_breath"));
        ingredients.add(getNonNull("minecraft:sugar"));
        ingredients.add(getNonNull("minecraft:rabbit_foot"));
        ingredients.add(getNonNull("minecraft:glistering_melon_slice"));
        ingredients.add(getNonNull("minecraft:spider_eye"));
        ingredients.add(getNonNull("minecraft:pufferfish"));
        ingredients.add(getNonNull("minecraft:magma_cream"));
        ingredients.add(getNonNull("minecraft:golden_carrot"));
        ingredients.add(getNonNull("minecraft:blaze_powder"));
        ingredients.add(getNonNull("minecraft:ghast_tear"));
        ingredients.add(getNonNull("minecraft:turtle_helmet"));
        ingredients.add(getNonNull("minecraft:phantom_membrane"));

        List<ItemEntry> inputs = new ArrayList<>();
        inputs.add(getNonNull("minecraft:potion"));
        inputs.add(getNonNull("minecraft:splash_potion"));
        inputs.add(getNonNull("minecraft:lingering_potion"));

        ItemEntry glassBottle = getNonNull("minecraft:glass_bottle");

        Set<PotionMixData> potionMixes = new HashSet<>();

        // Add all types of potions as inputs
        ItemEntry fillerIngredient = ingredients.get(0);
        for (ItemEntry input : inputs) {
            for (Potion potion : Potion.VALUES) {
                potionMixes.add(new PotionMixData(
                        input.getBedrockId(), potion.getBedrockId(),
                        fillerIngredient.getBedrockId(), fillerIngredient.getBedrockData(),
                        glassBottle.getBedrockId(), glassBottle.getBedrockData())
                );
            }
        }

        // Add all brewing ingredients
        // Also adds glass bottle as input
        for (ItemEntry ingredient : ingredients) {
            potionMixes.add(new PotionMixData(
                    glassBottle.getBedrockId(), glassBottle.getBedrockData(),
                    ingredient.getBedrockId(), ingredient.getBedrockData(),
                    glassBottle.getBedrockId(), glassBottle.getBedrockData())
            );
        }

        POTION_MIXES = potionMixes;
    }

    private static ItemEntry getNonNull(String javaIdentifier) {
        ItemEntry itemEntry = ItemRegistry.getItemEntry(javaIdentifier);
        if (itemEntry == null)
            throw new NullPointerException("No item entry exists for java identifier: " + javaIdentifier);

        return itemEntry;
    }
}
