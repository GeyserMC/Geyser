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

package org.geysermc.geyser.item.type;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.item.TippedArrowPotion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

public class ArrowItem extends Item {
    public ArrowItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    public ItemStack translateToJava(ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        TippedArrowPotion tippedArrowPotion = TippedArrowPotion.getByBedrockId(itemData.getDamage());
        ItemStack itemStack = super.translateToJava(itemData, mapping, mappings);
        if (tippedArrowPotion != null) {
            itemStack = Items.TIPPED_ARROW.newItemStack(itemStack.getAmount(), itemStack.getNbt());
            StringTag potionTag = new StringTag("Potion", tippedArrowPotion.getJavaIdentifier());
            itemStack.getNbt().put(potionTag);
        }
        return itemStack;
    }
}
