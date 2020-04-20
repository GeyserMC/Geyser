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

package org.geysermc.connector.network.translators.item.translators;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemStackTranslator;
import org.geysermc.connector.network.translators.ItemTranslator;

import java.util.HashMap;

@ItemTranslator
public class LeatherArmorTranslator extends ItemStackTranslator {

    @Override
    public ItemData translateToJava(GeyserSession session, ItemData itemData) {
        if(itemData == null || itemData.getTag() == null) return itemData;

        CompoundTag itemTag = itemData.getTag();

        if(itemTag.contains("customColor")){
            int color = itemTag.getInt("customColor");
            CompoundTag displayTag = itemTag.getCompound("display");
            if(displayTag == null){
                displayTag = new CompoundTag("display", new HashMap<>());
            }
            displayTag = displayTag.toBuilder().intTag("color", color).build("display");
            itemTag.getValue().put("display", displayTag);
            System.out.println("Java: " + itemTag);
        }
        return itemData;
    }

    @Override
    public ItemStack translateToBedrock(GeyserSession session, ItemStack itemStack) {
        if(itemStack == null || itemStack.getNbt() == null) return itemStack;

        com.github.steveice10.opennbt.tag.builtin.CompoundTag itemTag = itemStack.getNbt();
        if(itemTag.contains("display")){
            com.github.steveice10.opennbt.tag.builtin.CompoundTag displayTag = itemTag.get("display");
            if(displayTag.contains("color")){
                IntTag color = displayTag.get("color");
                if(color != null){
                    itemTag.put(new IntTag("customColor", color.getValue()));
                    displayTag.remove("color");
                }
                System.out.println("Bedrock: " + itemTag);
            }
        }
        return itemStack;
    }
}
