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

package org.geysermc.geyser.translator.inventory.item.nbt;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.Arrays;
import java.util.List;

@ItemRemapper
public class LeatherArmorTranslator extends NbtItemStackTranslator {

    private static final List<String> ITEMS = Arrays.asList("minecraft:leather_helmet", "minecraft:leather_chestplate",
            "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:leather_horse_armor");

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        CompoundTag displayTag = itemTag.get("display");
        if (displayTag == null) {
            return;
        }
        IntTag color = displayTag.get("color");
        if (color != null) {
            itemTag.put(new IntTag("customColor", color.getValue()));
            displayTag.remove("color");
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemMapping mapping) {
        IntTag color = itemTag.get("customColor");
        if (color == null) {
            return;
        }
        CompoundTag displayTag = itemTag.get("display");
        if (displayTag == null) {
            displayTag = new CompoundTag("display");
        }
        displayTag.put(color);
        itemTag.remove("customColor");
    }

    @Override
    public boolean acceptItem(ItemMapping mapping) {
        return ITEMS.contains(mapping.getJavaIdentifier());
    }
}
