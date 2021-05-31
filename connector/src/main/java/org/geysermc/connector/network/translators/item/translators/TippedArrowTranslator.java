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

package org.geysermc.connector.network.translators.item.translators;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.TippedArrowPotion;

import java.util.List;
import java.util.stream.Collectors;

@ItemRemapper
public class TippedArrowTranslator extends ItemTranslator {

    private final List<ItemEntry> appliedItems;

    private static final int TIPPED_ARROW_JAVA_ID = ItemRegistry.getItemEntry("minecraft:tipped_arrow").getJavaId();

    public TippedArrowTranslator() {
        appliedItems = ItemRegistry.ITEM_ENTRIES.values().stream().filter(entry ->
                entry.getJavaIdentifier().contains("arrow") && !entry.getJavaIdentifier().contains("spectral")).collect(Collectors.toList());
    }

    @Override
    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemEntry itemEntry) {
        if (!itemEntry.getJavaIdentifier().equals("minecraft:tipped_arrow") || itemStack.getNbt() == null) {
            // We're only concerned about minecraft:arrow when translating Bedrock -> Java
            return super.translateToBedrock(itemStack, itemEntry);
        }
        Tag potionTag = itemStack.getNbt().get("Potion");
        if (potionTag instanceof StringTag) {
            TippedArrowPotion tippedArrowPotion = TippedArrowPotion.getByJavaIdentifier(((StringTag) potionTag).getValue());
            if (tippedArrowPotion != null) {
                return ItemData.builder()
                        .id(itemEntry.getBedrockId())
                        .damage(tippedArrowPotion.getBedrockId())
                        .count(itemStack.getAmount())
                        .tag(translateNbtToBedrock(itemStack.getNbt()));
            }
            GeyserConnector.getInstance().getLogger().debug("Unknown Java potion (tipped arrow): " + potionTag.getValue());
        }
        return super.translateToBedrock(itemStack, itemEntry);
    }

    @Override
    public ItemStack translateToJava(ItemData itemData, ItemEntry itemEntry) {
        TippedArrowPotion tippedArrowPotion = TippedArrowPotion.getByBedrockId(itemData.getDamage());
        ItemStack itemStack = super.translateToJava(itemData, itemEntry);
        if (tippedArrowPotion != null) {
            itemStack = new ItemStack(TIPPED_ARROW_JAVA_ID, itemStack.getAmount(), itemStack.getNbt());
            StringTag potionTag = new StringTag("Potion", tippedArrowPotion.getJavaIdentifier());
            itemStack.getNbt().put(potionTag);
        }
        return itemStack;
    }

    @Override
    public List<ItemEntry> getAppliedItems() {
        return appliedItems;
    }
}
