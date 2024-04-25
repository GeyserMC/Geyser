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

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.inventory.item.Enchantment;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.FishingRodItem;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;

public class ItemUtils {

    public static int getEnchantmentLevel(@Nullable DataComponents components, Enchantment.JavaEnchantment enchantment) {
        if (components == null) {
            return 0;
        }

        ItemEnchantments enchantmentData = components.get(DataComponentType.ENCHANTMENTS);
        if (enchantmentData == null) {
            return 0;
        }

        return enchantmentData.getEnchantments().getOrDefault(enchantment.ordinal(), 0);
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

    /**
     * @param components the data components of the item
     * @return the custom name of the item
     */
    public static @Nullable Component getCustomName(DataComponents components) {
        if (components == null) {
            return null;
        }
        return components.get(DataComponentType.CUSTOM_NAME);
    }
}
