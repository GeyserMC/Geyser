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
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.ItemStackTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.utils.ItemUtils;
import org.geysermc.connector.utils.Toolbox;

import java.util.List;
import java.util.stream.Collectors;

@ItemRemapper
public class BannerTranslator extends ItemStackTranslator {

    private List<ItemEntry> appliedItems;

    public BannerTranslator() {
        appliedItems = Toolbox.ITEM_ENTRIES.values().stream().filter(entry -> entry.getJavaIdentifier().endsWith("banner")).collect(Collectors.toList());
    }

    @Override
    public ItemData translateToBedrock(ItemStack itemStack, ItemEntry itemEntry) {
        if (itemStack.getNbt() == null) return super.translateToBedrock(itemStack, itemEntry);

        ItemData itemData = super.translateToBedrock(itemStack, itemEntry);

        CompoundTag blockEntityTag = itemStack.getNbt().get("BlockEntityTag");
        if (blockEntityTag.contains("Patterns")) {
            ListTag patterns = blockEntityTag.get("Patterns");

            CompoundTagBuilder builder = itemData.getTag().toBuilder();
            builder.tag(ItemUtils.convertBannerPattern(patterns));

            itemData = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount(), builder.buildRootTag());
        }

        return itemData;
    }

    @Override
    public ItemStack translateToJava(ItemData itemData, ItemEntry itemEntry) {
        if (itemData.getTag() == null) return super.translateToJava(itemData, itemEntry);

        ItemStack itemStack = super.translateToJava(itemData, itemEntry);

        com.nukkitx.nbt.tag.CompoundTag nbtTag = itemData.getTag();
        if (nbtTag.contains("Patterns")) {
            com.nukkitx.nbt.tag.ListTag patterns = (com.nukkitx.nbt.tag.ListTag) nbtTag.get("Patterns");

            CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
            blockEntityTag.put(ItemUtils.convertBannerPattern(patterns));

            itemStack.getNbt().put(blockEntityTag);
        }

        return itemStack;
    }

    @Override
    public List<ItemEntry> getAppliedItems() {
        return appliedItems;
    }
}
