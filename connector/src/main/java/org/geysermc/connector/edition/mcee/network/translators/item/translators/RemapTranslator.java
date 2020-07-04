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

package org.geysermc.connector.edition.mcee.network.translators.item.translators;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RemapTranslator will remap items that don't exist to ones that do with appropriate description updates
 */
@ItemRemapper
public class RemapTranslator extends ItemTranslator {

    private List<ItemEntry> appliedItems;

    public RemapTranslator() {
    }

    @Override
    public ItemData translateToBedrock(ItemStack itemStack, ItemEntry itemEntry) {
        ItemData itemData = super.translateToBedrock(itemStack, itemEntry);

        if (itemEntry.getExtra().has("name")) {
            CompoundTag tag = itemData.getTag();
            if (tag == null) {
                tag = CompoundTag.builder().buildRootTag();
            }

            CompoundTag display = tag.get("display");
            if (display == null) {
                display = CompoundTag.builder().buildRootTag();
            }

            CompoundTagBuilder displayBuilder = display.toBuilder();
            displayBuilder.stringTag("Name", itemEntry.getExtra().get("name").textValue());

            CompoundTagBuilder builder = tag.toBuilder();
            builder.tag(displayBuilder.build("display"));

            itemData = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount(), builder.buildRootTag());
        }
        return itemData;
    }

    @Override
    public List<ItemEntry> getAppliedItems() {
        if (appliedItems == null) {
            appliedItems = ItemRegistry.ITEM_ENTRIES.values().stream().filter(entry -> entry.getExtra() != null).collect(Collectors.toList());
        }
        return appliedItems;
    }

}
