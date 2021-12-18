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

import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.util.ItemUtils;

import java.util.ArrayList;
import java.util.List;

@ItemRemapper(priority = -1)
public class BasicItemTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        Tag damage = itemTag.get("Damage");
        if (damage instanceof IntTag) {
            int originalDurability = ((IntTag) damage).getValue();
            int durability = ItemUtils.getCorrectBedrockDurability(session, mapping.getJavaId(), originalDurability);
            if (durability != originalDurability) {
                // Fix damage tag inconsistencies
                itemTag.put(new IntTag("Damage", durability));
            }
        }

        CompoundTag displayTag = itemTag.get("display");
        if (displayTag == null) {
            return;
        }

        Tag loreTag = displayTag.get("Lore");
        if (loreTag instanceof ListTag listTag) {
            List<Tag> lore = new ArrayList<>();
            for (Tag tag : listTag.getValue()) {
                if (!(tag instanceof StringTag)) continue;
                lore.add(new StringTag("", MessageTranslator.convertMessageLenient(((StringTag) tag).getValue(), session.getLocale())));
            }
            displayTag.put(new ListTag("Lore", lore));
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemMapping mapping) {
        CompoundTag displayTag = itemTag.get("display");
        if (displayTag == null) {
            return;
        }

        if (displayTag.contains("Name")) {
            StringTag nameTag = displayTag.get("Name");
            displayTag.put(new StringTag("Name", MessageTranslator.convertToJavaMessage(nameTag.getValue())));
        }

        if (displayTag.contains("Lore")) {
            ListTag loreTag = displayTag.get("Lore");
            List<Tag> lore = new ArrayList<>();
            for (Tag tag : loreTag.getValue()) {
                if (!(tag instanceof StringTag)) continue;
                lore.add(new StringTag("", MessageTranslator.convertToJavaMessage(((StringTag) tag).getValue())));
            }
            displayTag.put(new ListTag("Lore", lore));
        }
    }
}
