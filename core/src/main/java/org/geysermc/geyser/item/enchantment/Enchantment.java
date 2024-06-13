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
import org.geysermc.geyser.session.cache.tags.EnchantmentTag;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @param description only populated if {@link #bedrockEnchantment()} is not null.
 * @param anvilCost also as a rarity multiplier
 */
public record Enchantment(String identifier,
                          Set<EnchantmentComponent> effects,
                          ItemTag supportedItems,
                          int maxLevel,
                          String description,
                          int anvilCost,
                          @Nullable EnchantmentTag exclusiveSet,
                          @Nullable BedrockEnchantment bedrockEnchantment) {

    // Implementation note: I have a feeling the tags can be a list of items, because in vanilla they're HolderSet classes.
    // I'm not sure how that's wired over the network, so we'll put it off.
    public static Enchantment read(RegistryEntry entry) {
        NbtMap data = entry.getData();
        Set<EnchantmentComponent> effects = readEnchantmentComponents(data.getCompound("effects"));
        String supportedItems = data.getString("supported_items").substring(1); // Remove '#' at beginning that indicates tag
        int maxLevel = data.getInt("max_level");
        int anvilCost = data.getInt("anvil_cost");
        String exclusiveSet = data.getString("exclusive_set", null);
        EnchantmentTag exclusiveSetTag = exclusiveSet == null ? null : EnchantmentTag.ALL_ENCHANTMENT_TAGS.get(MinecraftKey.key(exclusiveSet.substring(1)));
        BedrockEnchantment bedrockEnchantment = BedrockEnchantment.getByJavaIdentifier(entry.getId().asString());
        String description = bedrockEnchantment == null ? MessageTranslator.deserializeDescription(data) : null;

        return new Enchantment(entry.getId().asString(), effects, ItemTag.ALL_ITEM_TAGS.get(MinecraftKey.key(supportedItems)), maxLevel,
                description, anvilCost, exclusiveSetTag, bedrockEnchantment);
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
