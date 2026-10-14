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

package org.geysermc.geyser.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.item.enchantment.EnchantmentComponent;
import org.geysermc.geyser.item.type.FishingRodItem;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;

public final class ItemUtils {

    public static boolean hasEffect(JavaRegistryProvider registries, @Nullable GeyserItemStack itemStack, EnchantmentComponent component) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        ItemEnchantments enchantmentData = itemStack.getComponent(DataComponentTypes.ENCHANTMENTS);
        if (enchantmentData == null) {
            return false;
        }

        for (Integer id : enchantmentData.getEnchantments().keySet()) {
            Enchantment enchantment = JavaRegistries.ENCHANTMENT.getOrThrow(registries, id);
            if (enchantment.effects().contains(component)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the correct Bedrock durability for this item.
     */
    public static int getCorrectBedrockDurability(Item item, int original) {
        if (item == Items.FISHING_ROD) {
            // Java durability: 64
            // Bedrock durability : 384
            // 384 / 64 = 6
            return FishingRodItem.getBedrockDamage(original);
        }
        return original;
    }

    private ItemUtils() {
    }
}
