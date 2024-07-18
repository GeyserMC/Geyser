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

import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.inventory.item.BedrockEnchantment;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

/**
 * @param description only populated if {@link #bedrockEnchantment()} is not null.
 * @param anvilCost also as a rarity multiplier
 */
public record Enchantment(String identifier,
                          Set<EnchantmentComponent> effects,
                          HolderSet supportedItems,
                          int maxLevel,
                          String description,
                          int anvilCost,
                          HolderSet exclusiveSet,
                          @Nullable BedrockEnchantment bedrockEnchantment) {

    public static Enchantment read(RegistryEntryContext context) {
        NbtMap data = context.data();
        Set<EnchantmentComponent> effects = readEnchantmentComponents(data.getCompound("effects"));

        HolderSet supportedItems = readHolderSet(data.get("supported_items"), itemId -> Registries.JAVA_ITEM_IDENTIFIERS.getOrDefault(itemId.asString(), Items.AIR).javaId());

        int maxLevel = data.getInt("max_level");
        int anvilCost = data.getInt("anvil_cost");

        HolderSet exclusiveSet = readHolderSet(data.getOrDefault("exclusive_set", null), context::getNetworkId);

        BedrockEnchantment bedrockEnchantment = BedrockEnchantment.getByJavaIdentifier(context.id().asString());

        // TODO - description is a component. So if a hardcoded literal string is given, this will display normally on Java,
        //  but Geyser will attempt to lookup the literal string as translation - and will fail, displaying an empty string as enchantment name.
        String description = bedrockEnchantment == null ? MessageTranslator.deserializeDescription(data) : null;

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

    // TODO holder set util?
    private static HolderSet readHolderSet(@Nullable Object holderSet, Function<Key, Integer> keyIdMapping) {
        if (holderSet == null) {
            return new HolderSet(new int[]{});
        }

        if (holderSet instanceof String stringTag) {
            // Tag
            if (stringTag.startsWith("#")) {
                return new HolderSet(Key.key(stringTag.substring(1))); // Remove '#' at beginning that indicates tag
            } else {
                return new HolderSet(new int[]{keyIdMapping.apply(Key.key(stringTag))});
            }
        } else if (holderSet instanceof List<?> list) {
            // Assume the list is a list of strings
            return new HolderSet(list.stream().map(o -> (String) o).map(Key::key).map(keyIdMapping).mapToInt(Integer::intValue).toArray());
        }
        throw new IllegalArgumentException("Holder set must either be a tag, a string ID or a list of string IDs");
    }
}
