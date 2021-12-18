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
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.text.MinecraftLocale;

@ItemRemapper
public class PlayerHeadTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        if (!itemTag.contains("display") || !((CompoundTag) itemTag.get("display")).contains("Name")) {
            if (itemTag.contains("SkullOwner")) {
                StringTag name;
                Tag skullOwner = itemTag.get("SkullOwner");
                if (skullOwner instanceof StringTag) {
                    name = (StringTag) skullOwner;
                } else {
                    StringTag skullName;
                    if (skullOwner instanceof CompoundTag && (skullName = ((CompoundTag) skullOwner).get("Name")) != null) {
                        name = skullName;
                    } else {
                        session.getGeyser().getLogger().debug("Not sure how to handle skull head item display. " + itemTag);
                        return;
                    }
                }
                // Add correct name of player skull
                // TODO: It's always yellow, even with a custom name. Handle?
                String displayName = "\u00a7r\u00a7e" + MinecraftLocale.getLocaleString("block.minecraft.player_head.named", session.getLocale()).replace("%s", name.getValue());
                if (!itemTag.contains("display")) {
                    itemTag.put(new CompoundTag("display"));
                }
                ((CompoundTag) itemTag.get("display")).put(new StringTag("Name", displayName));
            }
        }
    }

    @Override
    public boolean acceptItem(ItemMapping mapping) {
        return mapping.getJavaIdentifier().equals("minecraft:player_head");
    }
}
