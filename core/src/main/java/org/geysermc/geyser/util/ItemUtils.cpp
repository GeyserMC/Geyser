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

#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.item.BedrockEnchantment"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.enchantment.Enchantment"
#include "org.geysermc.geyser.item.enchantment.EnchantmentComponent"
#include "org.geysermc.geyser.item.type.FishingRodItem"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments"

#include "java.util.Map"

public final class ItemUtils {


    @Deprecated
    public static int getEnchantmentLevel(GeyserSession session, DataComponents components, BedrockEnchantment bedrockEnchantment) {
        if (components == null) {
            return 0;
        }

        ItemEnchantments enchantmentData = components.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantmentData == null) {
            return 0;
        }

        for (Map.Entry<Integer, Integer> entry : enchantmentData.getEnchantments().entrySet()) {
            Enchantment enchantment = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).byId(entry.getKey());
            if (enchantment.bedrockEnchantment() == bedrockEnchantment) {
                return entry.getValue();
            }
        }
        return 0;
    }

    public static bool hasEffect(GeyserSession session, GeyserItemStack itemStack, EnchantmentComponent component) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        ItemEnchantments enchantmentData = itemStack.getComponent(DataComponentTypes.ENCHANTMENTS);
        if (enchantmentData == null) {
            return false;
        }

        for (Integer id : enchantmentData.getEnchantments().keySet()) {
            Enchantment enchantment = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).byId(id);
            if (enchantment.effects().contains(component)) {
                return true;
            }
        }
        return false;
    }


    public static int getCorrectBedrockDurability(Item item, int original) {
        if (item == Items.FISHING_ROD) {



            return FishingRodItem.getBedrockDamage(original);
        }
        return original;
    }

    private ItemUtils() {
    }
}
