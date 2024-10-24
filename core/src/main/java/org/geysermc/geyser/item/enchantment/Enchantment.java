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

package org.geysermc.geyser.item.enchantment;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.inventory.item.BedrockEnchantment;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @param description only populated if {@link #bedrockEnchantment()} is null.
 * @param anvilCost also as a rarity multiplier
 */
public record Enchantment(String identifier,
                          Set<EnchantmentComponent> effects,
                          GeyserHolderSet<Item> supportedItems,
                          int maxLevel,
                          String description,
                          int anvilCost,
                          GeyserHolderSet<Enchantment> exclusiveSet,
                          @Nullable BedrockEnchantment bedrockEnchantment) {

    public static Enchantment read(RegistryEntryContext context) {
        NbtMap data = context.data();
        Set<EnchantmentComponent> effects = readEnchantmentComponents(data.getCompound("effects"));

        GeyserHolderSet<Item> supportedItems = GeyserHolderSet.readHolderSet(context.session(), JavaRegistries.ITEM, data.get("supported_items"), itemId -> Registries.JAVA_ITEM_IDENTIFIERS.getOrDefault(itemId.asString(), Items.AIR).javaId());

        int maxLevel = data.getInt("max_level");
        int anvilCost = data.getInt("anvil_cost");

        GeyserHolderSet<Enchantment> exclusiveSet = GeyserHolderSet.readHolderSet(context.session(), JavaRegistries.ENCHANTMENT, data.get("exclusive_set"), context::getNetworkId);

        BedrockEnchantment bedrockEnchantment = BedrockEnchantment.getByJavaIdentifier(context.id().asString());

        String description = bedrockEnchantment == null ? MessageTranslator.deserializeDescription(context.session(), data) : null;

        return new Enchantment(context.id().asString(), effects, supportedItems, maxLevel,
                description, anvilCost, exclusiveSet, bedrockEnchantment);
    }

    private static Set<EnchantmentComponent> readEnchantmentComponents(NbtMap effects) {
        Set<EnchantmentComponent> components = new HashSet<>();
        for (Map.Entry<String, Object> entry : effects.entrySet()) {
            switch (entry.getKey()) {
                case "minecraft:prevent_armor_change" -> components.add(EnchantmentComponent.PREVENT_ARMOR_CHANGE);
            }
        }
        return Set.copyOf(components); // Also ensures any empty sets are consolidated
    }
}
