/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.item.translators.nbt;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.NbtItemStackTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

@ItemRemapper(priority = -1)
public class BasicItemTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.contains("display")) {
            CompoundTag displayTag = itemTag.get("display");
            if (displayTag.contains("Name")) {
                StringTag nameTag = displayTag.get("Name");
                try {
                    displayTag.put(new StringTag("Name", "§r" + MessageUtils.getBedrockMessage(nameTag.getValue())));
                } catch (Exception ex) {}
            }

            if (displayTag.contains("Lore")) {
                ListTag loreTag = displayTag.get("Lore");
                List<Tag> lore = new ArrayList<>();
                for (Tag tag : loreTag.getValue()) {
                    if (!(tag instanceof StringTag)) return;
                    try {
                        lore.add(new StringTag("", "§r" + MessageUtils.getBedrockMessage((String) tag.getValue())));
                    } catch (Exception ex) {}
                }
                displayTag.put(new ListTag("Lore", lore));
            }
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.contains("display")) {
            CompoundTag displayTag = itemTag.get("display");
            if (displayTag.contains("Name")) {
                StringTag nameTag = displayTag.get("Name");
                displayTag.put(new StringTag("Name", toJavaMessage(nameTag)));
            }

            if (displayTag.contains("Lore")) {
                ListTag loreTag = displayTag.get("Lore");
                List<Tag> lore = new ArrayList<>();
                for (Tag tag : loreTag.getValue()) {
                    if (!(tag instanceof StringTag)) return;
                    lore.add(new StringTag("", "§r" + toJavaMessage((StringTag) tag)));
                }
                displayTag.put(new ListTag("Lore", lore));
            }
        }
    }

    private String toJavaMessage(StringTag tag) {
        String message = tag.getValue();
        if (message == null) return null;
        if (message.startsWith("§r")) {
            message = message.replaceFirst("§r", "");
        }
        return MessageUtils.getJavaMessage(message);
    }
}
