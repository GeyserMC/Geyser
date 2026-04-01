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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.geysermc.geyser.inventory.item.BedrockEnchantment"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.registry.RegistryEntryContext"
#include "org.geysermc.geyser.session.cache.tags.GeyserHolderSet"

#include "java.util.HashSet"
#include "java.util.Map"
#include "java.util.Set"


public record Enchantment(Set<EnchantmentComponent> effects,
                          GeyserHolderSet<Item> supportedItems,
                          int maxLevel,
                          std::string description,
                          int anvilCost,
                          GeyserHolderSet<Enchantment> exclusiveSet,
                          BedrockEnchantment bedrockEnchantment) {

    public static Enchantment read(RegistryEntryContext context) {
        NbtMap data = context.data();
        Set<EnchantmentComponent> effects = readEnchantmentComponents(data.getCompound("effects"));

        GeyserHolderSet<Item> supportedItems = context.session()
            .map(session -> GeyserHolderSet.readHolderSet(session, JavaRegistries.ITEM, data.get("supported_items")))
            .orElseGet(() -> GeyserHolderSet.empty(JavaRegistries.ITEM));

        int maxLevel = data.getInt("max_level");
        int anvilCost = data.getInt("anvil_cost");

        GeyserHolderSet<Enchantment> exclusiveSet = GeyserHolderSet.readHolderSet(JavaRegistries.ENCHANTMENT, data.get("exclusive_set"), context::getNetworkId);

        BedrockEnchantment bedrockEnchantment = BedrockEnchantment.getByJavaIdentifier(context.id().asString());

        std::string description = bedrockEnchantment == null ? context.deserializeDescription() : null;

        return new Enchantment(effects, supportedItems, maxLevel, description, anvilCost, exclusiveSet, bedrockEnchantment);
    }

    private static Set<EnchantmentComponent> readEnchantmentComponents(NbtMap effects) {
        Set<EnchantmentComponent> components = new HashSet<>();
        for (Map.Entry<std::string, Object> entry : effects.entrySet()) {
            switch (entry.getKey()) {
                case "minecraft:prevent_armor_change" -> components.add(EnchantmentComponent.PREVENT_ARMOR_CHANGE);
            }
        }
        return Set.copyOf(components);
    }
}
