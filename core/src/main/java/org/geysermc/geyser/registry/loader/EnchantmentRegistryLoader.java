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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.Enchantment.JavaEnchantment;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.EnchantmentData;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class EnchantmentRegistryLoader implements RegistryLoader<String, Map<JavaEnchantment, EnchantmentData>> {
    @Override
    public Map<JavaEnchantment, EnchantmentData> load(String input) {
        JsonObject enchantmentsJson;
        try (InputStream enchantmentsStream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input)) {
            enchantmentsJson = new JsonParser().parse(new InputStreamReader(enchantmentsStream)).getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("Unable to load enchantment data", e);
        }

        Map<JavaEnchantment, EnchantmentData> enchantments = new EnumMap<>(JavaEnchantment.class);
        for (Map.Entry<String, JsonElement> entry : enchantmentsJson.entrySet()) {
            JavaEnchantment key = JavaEnchantment.getByJavaIdentifier(entry.getKey());
            JsonObject node = entry.getValue().getAsJsonObject();
            int rarityMultiplier = node.get("anvil_cost").getAsInt();
            int maxLevel = node.get("max_level").getAsInt();

            EnumSet<JavaEnchantment> incompatibleEnchantments = EnumSet.noneOf(JavaEnchantment.class);
            JsonArray incompatibleEnchantmentsJson = node.getAsJsonArray("incompatible_enchantments");
            if (incompatibleEnchantmentsJson != null) {
                for (JsonElement incompatibleNode : incompatibleEnchantmentsJson) {
                    incompatibleEnchantments.add(JavaEnchantment.getByJavaIdentifier(incompatibleNode.getAsString()));
                }
            }

            IntSet validItems = new IntOpenHashSet();
            for (JsonElement itemNode : node.getAsJsonArray("valid_items")) {
                String javaIdentifier = itemNode.getAsString();
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
