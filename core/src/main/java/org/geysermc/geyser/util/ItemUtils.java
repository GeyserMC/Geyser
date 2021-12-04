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

package org.geysermc.geyser.util;

import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.geyser.session.GeyserSession;

public class ItemUtils {

    public static int getEnchantmentLevel(CompoundTag itemNBTData, String enchantmentId) {
        ListTag enchantments = (itemNBTData == null ? null : itemNBTData.get("Enchantments"));
        if (enchantments != null) {
            int enchantmentLevel = 0;
            for (Tag tag : enchantments) {
                CompoundTag enchantment = (CompoundTag) tag;
                StringTag enchantId = enchantment.get("id");
                if (enchantId.getValue().equals(enchantmentId)) {
                    enchantmentLevel = (int) ((ShortTag) enchantment.get("lvl")).getValue();
                }
            }
            return enchantmentLevel;
        }
        return 0;
    }

    /**
     * @return the correct Bedrock durability for this item.
     */
    public static int getCorrectBedrockDurability(GeyserSession session, int javaId, int original) {
        if (javaId == session.getItemMappings().getStoredItems().fishingRod().getJavaId()) {
            // Java durability: 64
            // Bedrock durability : 384
            // 384 / 64 = 6
            return original * 6;
        }
        return original;
    }

    /**
     * @param itemTag the NBT tag of the item
     * @return the custom name of the item
     */
    public static String getCustomName(CompoundTag itemTag) {
        if (itemTag != null) {
            if (itemTag.get("display") instanceof CompoundTag displayTag) {
                if (displayTag.get("Name") instanceof StringTag nameTag) {
                    return nameTag.getValue();
                }
            }
        }
        return null;
    }
}
