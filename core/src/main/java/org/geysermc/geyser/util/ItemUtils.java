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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.geysermc.geyser.session.GeyserSession;

import javax.annotation.Nullable;

public class ItemUtils {
    private static Int2IntMap DYE_COLORS = null;

    public static int getEnchantmentLevel(@Nullable CompoundTag itemNBTData, String enchantmentId) {
        if (itemNBTData == null) {
            return 0;
        }
        ListTag enchantments = itemNBTData.get("Enchantments");
        if (enchantments != null) {
            for (Tag tag : enchantments) {
                CompoundTag enchantment = (CompoundTag) tag;
                StringTag enchantId = enchantment.get("id");
                if (enchantId.getValue().equals(enchantmentId)) {
                    Tag lvl = enchantment.get("lvl");
                    if (lvl != null && lvl.getValue() instanceof Number number) {
                        return number.intValue();
                    }
                }
            }
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

    /**
     * Return the dye color associated with this Java item ID, if any. Returns -1 if no dye color exists for this item.
     */
    public static int dyeColorFor(int javaId) {
        return DYE_COLORS.get(javaId);
    }

    public static void setDyeColors(Int2IntMap dyeColors) {
        if (DYE_COLORS != null) {
            throw new RuntimeException();
        }
        dyeColors.defaultReturnValue(-1);
        DYE_COLORS = dyeColors;
    }
}
