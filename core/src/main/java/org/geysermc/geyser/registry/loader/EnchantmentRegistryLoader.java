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

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.Enchantment.JavaEnchantment;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.EnchantmentData;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

public class EnchantmentRegistryLoader implements RegistryLoader<String, Map<JavaEnchantment, EnchantmentData>> {
    @Override
    public Map<JavaEnchantment, EnchantmentData> load(String input) {
        JsonNode enchantmentsNode;
        try (InputStream enchantmentsStream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input)) {
            enchantmentsNode = GeyserImpl.JSON_MAPPER.readTree(enchantmentsStream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load enchantment data", e);
        }

        Map<JavaEnchantment, EnchantmentData> enchantments = new EnumMap<>(JavaEnchantment.class);
        Iterator<Map.Entry<String, JsonNode>> it = enchantmentsNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JavaEnchantment key = JavaEnchantment.getByJavaIdentifier(entry.getKey());
            JsonNode node = entry.getValue();
            int rarityMultiplier = switch (node.get("rarity").textValue()) {
                case "common" -> 1;
                case "uncommon" -> 2;
                case "rare" -> 4;
                case "very_rare" -> 8;
                default -> throw new IllegalStateException("Unexpected value: " + node.get("rarity").textValue());
            };
            int maxLevel = node.get("max_level").asInt();

            EnumSet<JavaEnchantment> incompatibleEnchantments = EnumSet.noneOf(JavaEnchantment.class);
            JsonNode incompatibleEnchantmentsNode = node.get("incompatible_enchantments");
            if (incompatibleEnchantmentsNode != null) {
                for (JsonNode incompatibleNode : incompatibleEnchantmentsNode) {
                    incompatibleEnchantments.add(JavaEnchantment.getByJavaIdentifier(incompatibleNode.textValue()));
                }
            }

            IntSet validItems = new IntOpenHashSet();
            for (JsonNode itemNode : node.get("valid_items")) {
                String javaIdentifier = itemNode.textValue();
                Item item = Registries.JAVA_ITEM_IDENTIFIERS.get(javaIdentifier);
                if (item != null) {
                    validItems.add(item.javaId());
                } else {
                    throw new NullPointerException("No item entry exists for java identifier: " + javaIdentifier);
                }
            }

            EnchantmentData enchantmentData = new EnchantmentData(rarityMultiplier, maxLevel, incompatibleEnchantments, validItems);
            enchantments.put(key, enchantmentData);
        }
        return enchantments;
    }
}
