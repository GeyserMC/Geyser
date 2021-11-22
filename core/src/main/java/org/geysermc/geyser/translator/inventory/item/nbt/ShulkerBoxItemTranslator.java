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

import com.github.steveice10.mc.protocol.data.game.Identifier;
import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;
import org.geysermc.geyser.util.MathUtils;

@ItemRemapper
public class ShulkerBoxItemTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        if (!itemTag.contains("BlockEntityTag")) return; // Empty shulker box

        CompoundTag blockEntityTag = itemTag.get("BlockEntityTag");
        if (blockEntityTag.get("Items") == null) return;
        ListTag itemsList = new ListTag("Items");
        for (Tag item : (ListTag) blockEntityTag.get("Items")) {
            CompoundTag itemData = (CompoundTag) item; // Information about the item
            CompoundTag boxItemTag = new CompoundTag(""); // Final item tag to add to the list
            boxItemTag.put(new ByteTag("Slot", (byte) (MathUtils.getNbtByte(itemData.get("Slot").getValue()) & 255)));
            boxItemTag.put(new ByteTag("WasPickedUp", (byte) 0)); // ???

            ItemMapping boxMapping = session.getItemMappings().getMapping(Identifier.formalize(((StringTag) itemData.get("id")).getValue()));

            boxItemTag.put(new StringTag("Name", boxMapping.getBedrockIdentifier()));
            boxItemTag.put(new ShortTag("Damage", (short) boxMapping.getBedrockData()));
            boxItemTag.put(new ByteTag("Count", MathUtils.getNbtByte(itemData.get("Count").getValue())));
            // Only the display name is what we have interest in, so just translate that if relevant
            CompoundTag displayTag = itemData.get("tag");
            if (displayTag == null && boxMapping.hasTranslation()) {
                displayTag = new CompoundTag("tag");
            }
            if (displayTag != null) {
                boxItemTag.put(ItemTranslator.translateDisplayProperties(session, displayTag, boxMapping, '7'));
            }

            itemsList.add(boxItemTag);
        }
        itemTag.put(itemsList);
        // Don't actually bother with removing the block entity tag. Too risky to translate
        // if the user is on creative and messing with a shulker box
        //itemTag.remove("BlockEntityTag");
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemMapping mapping) {
        if (itemTag.contains("Items")) { // Remove any extraneous Bedrock tag and don't touch the Java one
            itemTag.remove("Items");
        }
    }

    @Override
    public boolean acceptItem(ItemMapping mapping) {
        return mapping.getJavaIdentifier().contains("shulker_box");
    }
}
